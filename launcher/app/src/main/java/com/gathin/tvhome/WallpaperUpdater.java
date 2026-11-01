package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Keeps the wallpaper collection fresh, like Apple TV's aerial rotation: about once a day, while Home is on screen,
 * it fetches a few new landscape, city and aerial photos from Wikimedia Commons (CC0, public domain, CC BY and
 * CC BY-SA only), adds them to the wallpapers folder and drops the oldest beyond {@link #KEEP}. The author and
 * license of every photo go into captions.txt, which ambient mode shows as the photo credit.
 *
 * It is the only network use in this launcher: requests go to commons.wikimedia.org and upload.wikimedia.org, carry
 * no account, no identifier and nothing about the TV but the User-Agent below, and run on a background thread.
 */
final class WallpaperUpdater {
    static final long INTERVAL_MS = 24L * 60 * 60 * 1000;
    private static final long RETRY_MS = 60L * 60 * 1000;
    static final int PER_RUN = 8;
    static final int KEEP = 60;

    private static final String TAG = "TvHome.WallpaperUpdate";
    private static final String API = "https://commons.wikimedia.org/w/api.php";
    private static final String UA = "TvHome-wallpapers/1.0 (personal TV home screen)";
    private static final String[] NATURE = {
            "Category:Featured pictures of landscapes", "Category:Featured pictures of mountains",
            "Category:Featured pictures of forests", "Category:Featured pictures of lakes",
            "Category:Featured pictures of waterfalls", "Category:Featured pictures of coasts",
            "Category:Featured pictures of deserts", "Category:Featured pictures of sunsets",
            "Category:Featured pictures of aerial photographs"};
    private static final String[] CITY = {
            "Category:Featured pictures of cityscapes", "Category:Featured pictures of skylines",
            "Category:Featured pictures of night photographs of cities",
            "Category:Featured pictures of aerial photographs of cities"};
    private static final Pattern LICENSE = Pattern.compile("^(CC0|Public domain|PD|CC BY(-SA)? \\d).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    private final File dir;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private volatile boolean running;

    WallpaperUpdater(Context context) {
        File base = Config.dir(context);
        dir = base == null ? null : new File(base, "wallpapers");
        prefs = context.getSharedPreferences("wallpaper_updater", Context.MODE_PRIVATE);
    }

    /** Makes the next visit to Home fetch (Display settings: Refresh wallpapers now). */
    void refreshSoon() {
        prefs.edit().putLong("last_attempt", 0).putLong("last_success", 0).apply();
    }

    /** How often to fetch, in days; 0 is off. Home settings > Wallpapers > Automatic updates. */
    int days() {
        return prefs.getInt("days", prefs.getBoolean("enabled", true) ? 1 : 0);
    }

    void setDays(int days) {
        prefs.edit().putInt("days", days).apply();
    }

    boolean enabled() {
        return days() > 0;
    }

    /** More pictures per visit when the visits are further apart. */
    private int perRun() {
        int d = days();
        return d >= 30 ? 30 : d >= 7 ? 20 : PER_RUN;
    }

    /** Starts a fetch when one is due. {@code onChanged} runs on the main thread after new pictures were added. */
    void maybeRun(java.util.function.Supplier<String> inUse, Runnable onChanged) {
        if (dir == null || running || !enabled()) return;
        long now = System.currentTimeMillis();
        if (now - prefs.getLong("last_success", 0) < days() * INTERVAL_MS || now - prefs.getLong("last_attempt", 0) < RETRY_MS) return;
        running = true;
        prefs.edit().putLong("last_attempt", now).apply();
        executor.execute(() -> {
            try {
                int added = fetch(inUse.get());
                prefs.edit().putLong("last_success", System.currentTimeMillis()).apply();
                if (added > 0) main.post(onChanged);
            } catch (Exception | OutOfMemoryError failure) {
                Log.w(TAG, "Wallpaper update failed: " + failure);   // offline or rate-limited: retried in an hour
            } finally {
                running = false;
            }
        });
    }

    private int fetch(String inUse) throws Exception {
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        Set<String> seen = new HashSet<>(prefs.getStringSet("seen", Collections.emptySet()));
        List<String> titles = new ArrayList<>();
        // Two categories from each group per run, each read from a random place in its sort order.
        for (String[] group : new String[][] {NATURE, CITY}) {
            List<String> cats = new ArrayList<>(Arrays.asList(group));
            Collections.shuffle(cats, random);
            for (int i = 0; i < 2; i++) titles.addAll(members(cats.get(i)));
        }
        titles.removeAll(seen);
        Collections.shuffle(titles, random);
        if (titles.isEmpty()) {
            prefs.edit().remove("seen").apply();   // everything offered has been shown once: start over
            return 0;
        }
        Map<String, String> credits = new TreeMap<>(WallpaperCaptions.read(dir));
        int added = 0;
        long stamp = System.currentTimeMillis();
        for (int from = 0; from < titles.size() && added < perRun(); from += 40) {
            JSONArray pages = imageInfo(titles.subList(from, Math.min(titles.size(), from + 40)));
            for (int i = 0; i < pages.length() && added < perRun(); i++) {
                JSONObject page = pages.getJSONObject(i);
                String title = page.optString("title");
                seen.add(title);
                JSONArray infos = page.optJSONArray("imageinfo");
                if (infos == null || infos.length() == 0) continue;
                JSONObject info = infos.getJSONObject(0);
                int w = info.optInt("width"), h = info.optInt("height");
                if (!"image/jpeg".equals(info.optString("mime")) || w < 2800 || h <= 0 || w / (float) h < 1.5f || w / (float) h > 2.4f) continue;
                JSONObject meta = info.optJSONObject("extmetadata");
                String license = meta == null ? "" : meta.optJSONObject("LicenseShortName") == null ? ""
                        : meta.getJSONObject("LicenseShortName").optString("value");
                if (!LICENSE.matcher(license).matches()) continue;
                String author = meta.optJSONObject("Artist") == null ? "" : clean(meta.getJSONObject("Artist").optString("value"));
                byte[] data = get(info.getString("thumburl"), 4 * 1024 * 1024);
                if (data.length < 8 || data[0] != (byte) 0xFF || data[1] != (byte) 0xD8) continue;
                String name = String.format(Locale.ROOT, "wm_%d_%02d.jpg", stamp, added);
                File part = new File(dir, name + ".part");
                Files.write(part.toPath(), data);
                if (!part.renameTo(new File(dir, name))) {
                    //noinspection ResultOfMethodCallIgnored
                    part.delete();
                    continue;
                }
                credits.put(name, credit(title, author));
                added++;
                Thread.sleep(500);   // be gentle with the servers
            }
        }
        prefs.edit().putStringSet("seen", trim(seen)).apply();
        prune(inUse, credits);
        try {
            writeCredits(dir, credits);
        } catch (IOException noCredits) {
            Log.w(TAG, "Could not write the photo credits: " + noCredits);   // the pictures are still usable
        }
        return added;
    }

    private List<String> members(String category) throws Exception {
        String start = String.format(Locale.ROOT, "%016x", random.nextLong());
        for (String extra : new String[] {"&cmsort=sortkey&cmstarthexsortkey=" + start, ""}) {
            JSONObject json = new JSONObject(new String(get(API + "?action=query&format=json&formatversion=2&list=categorymembers"
                    + "&cmtype=file&cmlimit=100&cmtitle=" + enc(category) + extra, 512 * 1024), StandardCharsets.UTF_8));
            JSONArray list = json.getJSONObject("query").getJSONArray("categorymembers");
            if (list.length() == 0) continue;
            List<String> out = new ArrayList<>();
            for (int i = 0; i < list.length(); i++) out.add(list.getJSONObject(i).getString("title"));
            return out;
        }
        return Collections.emptyList();
    }

    private JSONArray imageInfo(List<String> titles) throws Exception {
        StringBuilder joined = new StringBuilder();
        for (String t : titles) joined.append(joined.length() == 0 ? "" : "|").append(t);
        JSONObject json = new JSONObject(new String(get(API + "?action=query&format=json&formatversion=2&prop=imageinfo"
                + "&iiprop=url%7Csize%7Cmime%7Cextmetadata&iiurlwidth=1920&titles=" + enc(joined.toString()), 2 * 1024 * 1024),
                StandardCharsets.UTF_8));
        return json.getJSONObject("query").getJSONArray("pages");
    }

    /** Keeps the newest {@link #KEEP} pictures; the one on screen always stays. */
    private void prune(String inUse, Map<String, String> credits) {
        File[] all = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg|png|webp)"));
        if (all == null || all.length <= KEEP) return;
        Arrays.sort(all, Comparator.comparingLong(File::lastModified));
        int excess = all.length - KEEP;
        for (File f : all) {
            if (excess <= 0) break;
            if (f.getName().equals(inUse)) continue;
            if (f.delete()) {
                credits.remove(f.getName());
                excess--;
            }
        }
    }

    static void writeCredits(File dir, Map<String, String> credits) throws IOException {
        StringBuilder out = new StringBuilder("# Photo credits: file<TAB>title - author (written by the wallpaper updater)\n");
        for (Map.Entry<String, String> e : credits.entrySet()) out.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        // A new file renamed over the old one: a captions.txt pushed over ADB belongs to another user and cannot be
        // overwritten in place, but the folder is ours, so it can be replaced.
        File target = new File(dir, WallpaperCaptions.FILE_NAME);
        File temp = new File(dir, WallpaperCaptions.FILE_NAME + ".part");
        Files.write(temp.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
        Files.move(temp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /** "Title - Author", cut to what WallpaperCaptions keeps. */
    static String credit(String fileTitle, String author) {
        String t = fileTitle.replaceFirst("^File:", "").replaceFirst("\\.[A-Za-z]+$", "").replace('_', ' ').trim();
        String text = author.isEmpty() ? t : t + " · " + author;
        return text.length() > WallpaperCaptions.MAX_LENGTH ? text.substring(0, WallpaperCaptions.MAX_LENGTH - 1).trim() + "…" : text;
    }

    private static String clean(String html) {
        String text = TAGS.matcher(html).replaceAll("").replaceFirst("^(User|Utilisateur):", "");
        text = text.replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace('\n', ' ').replace('\t', ' ').trim();
        return text.length() > 40 ? text.substring(0, 40).trim() : text;
    }

    private static Set<String> trim(Set<String> seen) {
        if (seen.size() <= 3000) return seen;
        List<String> list = new ArrayList<>(seen);
        return new HashSet<>(list.subList(list.size() - 2000, list.size()));
    }

    private static String enc(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8");
    }

    private static byte[] get(String url, int max) throws IOException {
        return Net.get(url, max, UA);
    }
}
