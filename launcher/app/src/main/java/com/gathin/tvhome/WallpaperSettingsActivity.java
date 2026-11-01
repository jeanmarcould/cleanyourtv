package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

/** Home settings > Wallpapers: how often the picture changes, how it changes, automatic updates, and the collection. */
public class WallpaperSettingsActivity extends Activity {
    /** Minutes between pictures: 5 min, 15 min, 30 min, 1 h, 2 h, 6 h, 24 h, 2 days, 1 week, 1 month. */
    private static final int[] MINUTES = {5, 15, 30, 60, 120, 360, 1440, 2880, 10080, 43200};
    private static final int[] FADE_MS = {500, 1000, 1600, 3000, 5000};
    /** Days between automatic updates; 0 is off. */
    private static final int[] UPDATE_DAYS = {0, 1, 7, 30};

    private SharedPreferences settings;
    private WallpaperUpdater updater;
    private TextView everyRow, transitionRow, durationRow, updatesRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        updater = new WallpaperUpdater(this);
        SettingsScreen screen = new SettingsScreen(this, R.string.sec_wallpapers);
        everyRow = screen.row("");
        transitionRow = screen.row("");
        durationRow = screen.row("");
        updatesRow = screen.row("");
        TextView refresh = screen.row(getString(R.string.refresh_wallpapers));
        TextView manage = screen.row(getString(R.string.wallpapers_manage));
        TextView done = screen.row(getString(R.string.done));
        everyRow.setOnClickListener(v -> {
            CharSequence[] labels = new CharSequence[MINUTES.length];
            for (int i = 0; i < MINUTES.length; i++) labels[i] = minutesLabel(MINUTES[i]);
            int current = -1;
            for (int i = 0; i < MINUTES.length; i++) if (MINUTES[i] == minutes()) current = i;
            SettingsScreen.choose(this, R.string.setting_wallpaper_interval, labels, current, which -> {
                settings.edit().putInt("wallpaper_minutes", MINUTES[which]).apply();
                changed();
            });
        });
        transitionRow.setOnClickListener(v -> SettingsScreen.choose(this, R.string.setting_transition,
                new CharSequence[] {getString(R.string.value_fade), getString(R.string.value_none)}, fade() ? 0 : 1, which -> {
                    settings.edit().putString("wallpaper_fade", which == 0 ? "fade" : "none").apply();
                    changed();
                }));
        durationRow.setOnClickListener(v -> {
            if (!fade()) return;
            CharSequence[] labels = new CharSequence[FADE_MS.length];
            for (int i = 0; i < FADE_MS.length; i++) labels[i] = seconds(FADE_MS[i]);
            SettingsScreen.choose(this, R.string.setting_fade_duration, labels,
                    SettingsScreen.indexOf(FADE_MS, settings.getInt("wallpaper_fade_ms", 1600)), which -> {
                        settings.edit().putInt("wallpaper_fade_ms", FADE_MS[which]).apply();
                        changed();
                    });
        });
        updatesRow.setOnClickListener(v -> SettingsScreen.choose(this, R.string.setting_auto_update,
                new CharSequence[] {getString(R.string.value_off), getString(R.string.freq_day), getString(R.string.freq_week),
                        getString(R.string.freq_month)},
                SettingsScreen.indexOf(UPDATE_DAYS, updater.days()), which -> {
                    updater.setDays(UPDATE_DAYS[which]);
                    changed();
                }));
        refresh.setOnClickListener(v -> {
            updater.refreshSoon();
            Toast.makeText(this, R.string.refresh_wallpapers_toast, Toast.LENGTH_LONG).show();
        });
        manage.setOnClickListener(v -> startActivity(new Intent(this, WallpaperManagerActivity.class)));
        done.setOnClickListener(v -> finish());
        bind();
        everyRow.requestFocus();
    }

    /** The interval in force: the choice made here, else the theme's. */
    private int minutes() {
        int chosen = settings.getInt("wallpaper_minutes", 0);
        return chosen > 0 ? chosen : Math.max(1, Config.load(this).wallpaperRotationMinutes);
    }

    private boolean fade() {
        return !"none".equals(settings.getString("wallpaper_fade", "fade"));
    }

    private void changed() {
        SettingsScreen.bump(this);
        bind();
    }

    private String minutesLabel(int m) {
        if (m == 43200) return getString(R.string.value_month);
        if (m == 10080) return getString(R.string.value_week);
        if (m == 2880) return getString(R.string.value_days_n, 2);
        if (m == 1440) return getString(R.string.value_hours_n, 24);
        if (m == 60) return getString(R.string.value_hour_one);
        if (m % 60 == 0) return getString(R.string.value_hours_n, m / 60);
        return getString(R.string.value_minutes, m);
    }

    private String seconds(int ms) {
        return getString(R.string.value_seconds, String.valueOf(ms / 1000f).replaceAll("\\.0$", ""));
    }

    private void bind() {
        String arrow = "  ▾";
        everyRow.setText(getString(R.string.setting_wallpaper_interval) + "   ·   " + minutesLabel(minutes()) + arrow);
        transitionRow.setText(getString(R.string.setting_transition) + "   ·   " + getString(fade() ? R.string.value_fade : R.string.value_none) + arrow);
        durationRow.setText(getString(R.string.setting_fade_duration) + "   ·   "
                + (fade() ? seconds(settings.getInt("wallpaper_fade_ms", 1600)) + arrow : "—"));
        durationRow.setAlpha(fade() ? 1f : 0.4f);
        int d = updater.days();
        updatesRow.setText(getString(R.string.setting_auto_update) + "   ·   "
                + getString(d == 0 ? R.string.value_off : d >= 30 ? R.string.freq_month : d >= 7 ? R.string.freq_week : R.string.freq_day) + arrow);
    }
}
