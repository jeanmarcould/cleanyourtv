package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Current weather for one place from Open-Meteo (no account, no key). Only the place's latitude and longitude are
 * sent, and only while Home is on screen: about every 30 minutes. The place is chosen in Weather settings, which
 * searches Open-Meteo's geocoding by the name typed there; Paris until then.
 */
final class Weather {
    static final int CLEAR = 0, PARTLY = 1, CLOUDY = 2, FOG = 3, RAIN = 4, SNOW = 5, STORM = 6;

    private static final String TAG = "TvHome.Weather";
    private static final String UA = "TvHome-weather/1.0 (personal TV home screen)";
    private static final long REFRESH_MS = 30L * 60 * 1000;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile boolean running;

    private Weather() {}

    /** One search result in Weather settings. */
    static final class Place {
        final String name, detail;
        final double latitude, longitude;

        Place(String name, String detail, double latitude, double longitude) {
            this.name = name;
            this.detail = detail;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("weather", Context.MODE_PRIVATE);
    }

    static String placeName(Context ctx) {
        return prefs(ctx).getString("name", "Paris");
    }

    static boolean fahrenheit(Context ctx) {
        return "f".equals(prefs(ctx).getString("unit", "c"));
    }

    /** Icon set: "outline" (default), "filled" or "color". */
    static String iconSet(Context ctx) {
        return prefs(ctx).getString("icon_set", "outline");
    }

    static void setIconSet(Context ctx, String set) {
        prefs(ctx).edit().putString("icon_set", set).apply();
    }

    static void setFahrenheit(Context ctx, boolean f) {
        prefs(ctx).edit().putString("unit", f ? "f" : "c").remove("fetched").apply();
    }

    static void setPlace(Context ctx, Place p) {
        prefs(ctx).edit().putString("name", p.name).putString("lat", String.valueOf(p.latitude))
                .putString("lon", String.valueOf(p.longitude)).remove("fetched").remove("temp").apply();
    }

    /** True once a reading has been fetched for the current place and unit. */
    static boolean hasReading(Context ctx) {
        return prefs(ctx).contains("temp");
    }

    /** The last reading's temperature, rounded, in the chosen unit. */
    static int temperature(Context ctx) {
        return Math.round(prefs(ctx).getFloat("temp", 0f));
    }

    static int category(Context ctx) {
        return categoryOf(prefs(ctx).getInt("code", 0));
    }

    static boolean night(Context ctx) {
        return prefs(ctx).getInt("day", 1) == 0;
    }

    /** WMO weather code to one of the icons. */
    static int categoryOf(int code) {
        if (code <= 1) return CLEAR;
        if (code == 2) return PARTLY;
        if (code == 3) return CLOUDY;
        if (code == 45 || code == 48) return FOG;
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return RAIN;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return SNOW;
        if (code >= 95) return STORM;
        return CLOUDY;
    }

    /** Fetches a fresh reading when the last one is older than 30 minutes; {@code done} runs on the main thread. */
    static void refreshIfDue(Context context, Runnable done) {
        Context ctx = context.getApplicationContext();
        SharedPreferences p = prefs(ctx);
        if (running || System.currentTimeMillis() - p.getLong("fetched", 0) < REFRESH_MS
                || System.currentTimeMillis() - p.getLong("attempt", 0) < 5 * 60 * 1000L) return;
        running = true;
        p.edit().putLong("attempt", System.currentTimeMillis()).apply();
        String lat = p.getString("lat", "48.8566"), lon = p.getString("lon", "2.3522");
        String unit = "f".equals(p.getString("unit", "c")) ? "fahrenheit" : "celsius";
        EXECUTOR.execute(() -> {
            try {
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code,is_day&timezone=auto&temperature_unit=" + unit;
                JSONObject current = new JSONObject(new String(Net.get(url, 64 * 1024, UA), StandardCharsets.UTF_8))
                        .getJSONObject("current");
                p.edit().putFloat("temp", (float) current.getDouble("temperature_2m"))
                        .putInt("code", current.getInt("weather_code")).putInt("day", current.optInt("is_day", 1))
                        .putLong("fetched", System.currentTimeMillis()).apply();
                MAIN.post(done);
            } catch (Exception failure) {
                Log.w(TAG, "Weather fetch failed: " + failure);
            } finally {
                running = false;
            }
        });
    }

    /** Looks a place up by name; {@code done} gets the matches (possibly none) on the main thread, null on failure. */
    static void search(String query, Callback done) {
        EXECUTOR.execute(() -> {
            List<Place> out = null;
            try {
                String lang = Locale.getDefault().getLanguage();
                String url = "https://geocoding-api.open-meteo.com/v1/search?count=6&format=json&language=" + lang
                        + "&name=" + URLEncoder.encode(query.trim(), "UTF-8");
                JSONObject json = new JSONObject(new String(Net.get(url, 128 * 1024, UA), StandardCharsets.UTF_8));
                JSONArray results = json.optJSONArray("results");
                out = new ArrayList<>();
                for (int i = 0; results != null && i < results.length(); i++) {
                    JSONObject r = results.getJSONObject(i);
                    String detail = join(r.optString("admin1"), r.optString("country"));
                    out.add(new Place(r.getString("name"), detail, r.getDouble("latitude"), r.getDouble("longitude")));
                }
            } catch (Exception failure) {
                Log.w(TAG, "Place search failed: " + failure);
            }
            List<Place> result = out;
            MAIN.post(() -> done.found(result));
        });
    }

    private static String join(String a, String b) {
        if (a.isEmpty()) return b;
        return b.isEmpty() ? a : a + ", " + b;
    }

    interface Callback {
        void found(List<Place> places);
    }
}
