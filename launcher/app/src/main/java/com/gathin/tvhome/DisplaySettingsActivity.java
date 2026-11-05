package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

/** Home settings > Clock: time format, date format, and how long Home sits idle before ambient mode. */
public class DisplaySettingsActivity extends Activity {
    private static final String[] TIME = {"system", "12", "24"};
    private static final String[] DATE = {"short", "long", "numeric"};
    private static final int[] AMBIENT = {0, 1, 2, 5, 10, 15, 30};

    private SharedPreferences settings;
    private TextView timeRow, dateRow, ambientRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        SettingsScreen screen = new SettingsScreen(this, R.string.sec_clock);
        timeRow = screen.row("");
        dateRow = screen.row("");
        ambientRow = screen.row("");
        TextView done = screen.row(getString(R.string.done));
        timeRow.setOnClickListener(v -> {
            int i = (SettingsScreen.indexOf(TIME, settings.getString(Config.CLOCK_FORMAT_KEY, "system")) + 1) % TIME.length;
            settings.edit().putString(Config.CLOCK_FORMAT_KEY, TIME[i]).apply();
            changed();
        });
        dateRow.setOnClickListener(v -> {
            int i = (SettingsScreen.indexOf(DATE, settings.getString("date_style", "short")) + 1) % DATE.length;
            settings.edit().putString("date_style", DATE[i]).apply();
            changed();
        });
        ambientRow.setOnClickListener(v -> {
            int i = (SettingsScreen.indexOf(AMBIENT, settings.getInt("ambient_minutes", 1)) + 1) % AMBIENT.length;
            settings.edit().putInt("ambient_minutes", AMBIENT[i]).apply();
            changed();
        });
        done.setOnClickListener(v -> finish());
        bind();
        timeRow.requestFocus();
    }

    private void changed() {
        SettingsScreen.bump(this);
        bind();
    }

    private void bind() {
        String time = settings.getString(Config.CLOCK_FORMAT_KEY, "system");
        timeRow.setText(getString(R.string.setting_time) + "   ·   "
                + getString("12".equals(time) ? R.string.value_12h : "24".equals(time) ? R.string.value_24h : R.string.value_system));
        String date = settings.getString("date_style", "short");
        dateRow.setText(getString(R.string.setting_date) + "   ·   "
                + getString("long".equals(date) ? R.string.value_date_long : "numeric".equals(date) ? R.string.value_date_numeric : R.string.value_date_short));
        int ambient = settings.getInt("ambient_minutes", 1);
        ambientRow.setText(getString(R.string.setting_ambient) + "   ·   "
                + (ambient == 0 ? getString(R.string.value_off) : getString(R.string.value_minutes, ambient)));
    }
}
