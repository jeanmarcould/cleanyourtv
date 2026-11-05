package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Gear menu > Customize home screen: the sections of home screen settings. */
public class HomeCustomizeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsScreen screen = new SettingsScreen(this, R.string.customize_home);
        screen.row("1   ·   " + getString(R.string.sec_clock))
                .setOnClickListener(v -> startActivity(new Intent(this, DisplaySettingsActivity.class)));
        screen.row("2   ·   " + getString(R.string.sec_weather))
                .setOnClickListener(v -> startActivity(new Intent(this, WeatherSettingsActivity.class)));
        screen.row("3   ·   " + getString(R.string.sec_wallpapers))
                .setOnClickListener(v -> startActivity(new Intent(this, WallpaperSettingsActivity.class)));
        screen.row("4   ·   " + getString(R.string.ambient_mode))
                .setOnClickListener(v -> startActivity(new Intent(this, AmbientSettingsActivity.class)));
        screen.row("5   ·   " + getString(R.string.sec_default_launcher))
                .setOnClickListener(v -> startActivity(new Intent(this, HomeLauncherActivity.class)));
        screen.row(getString(R.string.done)).setOnClickListener(v -> finish());
        screen.column.getChildAt(1).requestFocus();
    }
}
