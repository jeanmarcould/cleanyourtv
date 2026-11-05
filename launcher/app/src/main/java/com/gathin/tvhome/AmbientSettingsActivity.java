package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Home settings > Ambient mode: whether Home fades to the wallpaper when it sits idle, and after how many minutes.
 * The Wallpapers screen sets the same ambient_minutes key (0 turns it off), so Home picks a change up on resume.
 */
public class AmbientSettingsActivity extends Activity {
    /** The last timeout chosen, kept while ambient mode is off so switching it back on restores it. */
    static final String AMBIENT_LAST_KEY = "ambient_last_minutes";
    /** Timeouts offered while ambient mode is on: 1, 2, 5, 10, 15 and 30 minutes. */
    private static final int[] TIMEOUTS = {1, 2, 5, 10, 15, 30};
    private static final int DEFAULT_MINUTES = 1;

    private SharedPreferences settings;
    private TextView onRow, timeoutRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        SettingsScreen screen = new SettingsScreen(this, R.string.ambient_mode);
        onRow = screen.row("");
        timeoutRow = screen.row("");
        TextView done = screen.row(getString(R.string.done));
        onRow.setOnClickListener(v -> {
            if (on()) {
                settings.edit().putInt(AMBIENT_LAST_KEY, settings.getInt("ambient_minutes", DEFAULT_MINUTES))
                        .putInt("ambient_minutes", 0).apply();
            } else {
                settings.edit().putInt("ambient_minutes", lastTimeout()).apply();
            }
            changed();
        });
        timeoutRow.setOnClickListener(v -> {
            CharSequence[] labels = new CharSequence[TIMEOUTS.length];
            for (int i = 0; i < TIMEOUTS.length; i++) labels[i] = getString(R.string.value_minutes, TIMEOUTS[i]);
            SettingsScreen.choose(this, R.string.setting_ambient, labels,
                    SettingsScreen.indexOf(TIMEOUTS, settings.getInt("ambient_minutes", DEFAULT_MINUTES)), which -> {
                        settings.edit().putInt("ambient_minutes", TIMEOUTS[which])
                                .putInt(AMBIENT_LAST_KEY, TIMEOUTS[which]).apply();
                        changed();
                    });
        });
        done.setOnClickListener(v -> finish());
        bind();
        onRow.requestFocus();
    }

    private boolean on() {
        return settings.getInt("ambient_minutes", DEFAULT_MINUTES) > 0;
    }

    /** The timeout to switch back on with: the last one chosen, else one minute. */
    private int lastTimeout() {
        int last = settings.getInt(AMBIENT_LAST_KEY, DEFAULT_MINUTES);
        return last > 0 ? last : DEFAULT_MINUTES;
    }

    private void changed() {
        SettingsScreen.bump(this);
        bind();
    }

    private void bind() {
        onRow.setText(getString(R.string.ambient_mode) + "   \u00b7   " + getString(on() ? R.string.value_on : R.string.value_off));
        // The timeout only means something while ambient mode is on, so the row is hidden with it.
        timeoutRow.setVisibility(on() ? View.VISIBLE : View.GONE);
        if (on()) {
            timeoutRow.setText(getString(R.string.setting_ambient) + "   \u00b7   "
                    + getString(R.string.value_minutes, settings.getInt("ambient_minutes", DEFAULT_MINUTES)) + "  \u25be");
        }
    }
}
