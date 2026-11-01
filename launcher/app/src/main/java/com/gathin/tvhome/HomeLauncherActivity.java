package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Which home screen the remote's Home button opens: this launcher, or Google TV. It is a switch for the Home-key
 * helper (HomeKeyService), so it works without Android's own home picker, which many TVs do not have.
 * Either way, "Go to Google TV Home" in the gear menu and this launcher's entry in Google TV's app list stay available.
 */
public class HomeLauncherActivity extends Activity {
    private SharedPreferences settings;
    private TextView thisRow, stockRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        SettingsScreen screen = new SettingsScreen(this, R.string.sec_default_launcher);
        thisRow = screen.row("");
        stockRow = screen.row("");
        TextView done = screen.row(getString(R.string.done));
        thisRow.setOnClickListener(v -> {
            settings.edit().putString("home_target", "this").apply();
            bind();
        });
        stockRow.setOnClickListener(v -> {
            if (StockHome.find(this) == null) return;
            settings.edit().putString("home_target", "stock").apply();
            bind();
            StockHome.open(this);
            finish();
        });
        done.setOnClickListener(v -> finish());
        bind();
        (isStock() ? stockRow : thisRow).requestFocus();
    }

    private boolean isStock() {
        return "stock".equals(settings.getString("home_target", "this"));
    }

    private void bind() {
        String mark = "✓   ";
        thisRow.setText((isStock() ? "" : mark) + getString(R.string.launcher_this));
        stockRow.setText((isStock() ? mark : "") + getString(R.string.launcher_google_tv));
    }
}
