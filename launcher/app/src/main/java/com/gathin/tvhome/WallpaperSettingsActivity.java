package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Home settings > Wallpapers: how often the picture changes, how it changes, and the collection. The collection's
 * own rows (update now, automatic updates, view and delete) are in {@link WallpaperManageActivity}.
 */
public class WallpaperSettingsActivity extends Activity {
    /** Minutes between pictures: 5 min, 15 min, 30 min, 1 h, 2 h, 6 h, 24 h, 2 days, 1 week, 1 month. */
    private static final int[] MINUTES = {5, 15, 30, 60, 120, 360, 1440, 2880, 10080, 43200};
    private SharedPreferences settings;
    private TextView everyRow, transitionRow, manageRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        SettingsScreen screen = new SettingsScreen(this, R.string.sec_wallpapers);
        everyRow = screen.row("");
        transitionRow = screen.row("");
        manageRow = screen.row("");
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
        manageRow.setOnClickListener(v -> startActivity(new Intent(this, WallpaperManageActivity.class)));
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

    private void bind() {
        String arrow = "  \u25be";
        everyRow.setText(getString(R.string.setting_wallpaper_interval) + "   \u00b7   " + minutesLabel(minutes()) + arrow);
        transitionRow.setText(getString(R.string.setting_transition) + "   \u00b7   "
                + getString(fade() ? R.string.value_fade : R.string.value_none) + arrow);
        manageRow.setText(getString(R.string.wallpapers_manage) + "  \u203a");
    }
}
