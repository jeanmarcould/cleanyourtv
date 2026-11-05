package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

/** Home settings > Wallpapers > Manage wallpapers: fetch now, how often to fetch, and the collection itself. */
public class WallpaperManageActivity extends Activity {
    /** Days between automatic updates; 0 is off. */
    private static final int[] UPDATE_DAYS = {0, 1, 7, 30};

    private WallpaperUpdater updater;
    private TextView updateRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updater = new WallpaperUpdater(this);
        SettingsScreen screen = new SettingsScreen(this, R.string.wallpapers_manage);
        TextView refresh = screen.row(getString(R.string.refresh_wallpapers));
        updateRow = screen.row("");
        TextView view = screen.row(getString(R.string.wallpapers_view));
        TextView done = screen.row(getString(R.string.done));
        refresh.setOnClickListener(v -> {
            updater.refreshSoon();
            Toast.makeText(this, R.string.refresh_wallpapers_toast, Toast.LENGTH_LONG).show();
        });
        updateRow.setOnClickListener(v -> SettingsScreen.choose(this, R.string.setting_auto_update,
                new CharSequence[] {getString(R.string.value_off), getString(R.string.freq_day), getString(R.string.freq_week),
                        getString(R.string.freq_month)},
                SettingsScreen.indexOf(UPDATE_DAYS, updater.days()), which -> {
                    updater.setDays(UPDATE_DAYS[which]);
                    SettingsScreen.bump(this);
                    bind();
                }));
        view.setOnClickListener(v -> startActivity(new Intent(this, WallpaperManagerActivity.class)));
        done.setOnClickListener(v -> finish());
        bind();
        refresh.requestFocus();
    }

    private void bind() {
        int d = updater.days();
        updateRow.setText(getString(R.string.setting_auto_update) + "   \u00b7   "
                + getString(d == 0 ? R.string.value_off : d >= 30 ? R.string.freq_month : d >= 7 ? R.string.freq_week : R.string.freq_day)
                + "  \u25be");
    }
}
