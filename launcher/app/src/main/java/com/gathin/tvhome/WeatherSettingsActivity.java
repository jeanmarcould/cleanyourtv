package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Weather options, opened by clicking the weather panel: the place (searched by name) and the temperature unit. */
public class WeatherSettingsActivity extends Activity {
    private SettingsScreen screen;
    private TextView placeRow, unitRow, iconRow;
    private android.view.View strip;
    private EditText query;
    private final List<TextView> results = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screen = new SettingsScreen(this, R.string.weather_settings);
        placeRow = screen.row("");
        placeRow.setOnClickListener(v -> {
            query.setVisibility(android.view.View.VISIBLE);
            query.requestFocus();
        });
        query = new EditText(this);
        query.setHint(R.string.weather_search_hint);
        query.setHintTextColor(0x88FFFFFF);
        query.setTextColor(0xFFFFFFFF);
        query.setSingleLine(true);
        query.setInputType(InputType.TYPE_CLASS_TEXT);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.setVisibility(android.view.View.GONE);
        query.setOnEditorActionListener((v, action, event) -> {
            search();
            return true;
        });
        screen.column.addView(query, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        unitRow = screen.row("");
        unitRow.setOnClickListener(v -> {
            Weather.setFahrenheit(this, !Weather.fahrenheit(this));
            SettingsScreen.bump(this);
            bind();
        });
        iconRow = screen.row("");
        iconRow.setOnClickListener(v -> SettingsScreen.choose(this, R.string.setting_icon_set,
                new CharSequence[] {getString(R.string.icons_outline), getString(R.string.icons_filled), getString(R.string.icons_color)},
                WeatherIcon.styleFrom(Weather.iconSet(this)), which -> {
                    Weather.setIconSet(this, which == 1 ? "filled" : which == 2 ? "color" : "outline");
                    SettingsScreen.bump(this);
                    bind();
                }));
        strip = new android.view.View(this) {
            private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(android.graphics.Canvas c) {
                int style = WeatherIcon.styleFrom(Weather.iconSet(WeatherSettingsActivity.this));
                float cell = getWidth() / 7f, s = Math.min(cell, getHeight()) * 0.34f;
                for (int i = 0; i < 7; i++) WeatherIcon.draw(c, i, false, cell * (i + 0.5f), getHeight() / 2f, s, paint, style);
            }
        };
        float d = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.round(64 * d));
        sp.bottomMargin = Math.round(8 * d);
        screen.column.addView(strip, sp);
        screen.row(getString(R.string.done)).setOnClickListener(v -> finish());
        bind();
        placeRow.requestFocus();
    }

    private void bind() {
        placeRow.setText(getString(R.string.weather_place) + "   ·   " + Weather.placeName(this));
        unitRow.setText(getString(R.string.weather_unit) + "   ·   " + (Weather.fahrenheit(this) ? "°F" : "°C"));
        int set = WeatherIcon.styleFrom(Weather.iconSet(this));
        iconRow.setText(getString(R.string.setting_icon_set) + "   \u00B7   "
                + getString(set == WeatherIcon.FILLED ? R.string.icons_filled : set == WeatherIcon.COLOR ? R.string.icons_color : R.string.icons_outline) + "  \u25BE");
        if (strip != null) strip.invalidate();
    }

    private void search() {
        String text = query.getText().toString().trim();
        if (text.length() < 2) return;
        Weather.search(text, places -> {
            if (isFinishing()) return;
            for (TextView old : results) screen.column.removeView(old);
            results.clear();
            if (places == null) {
                Toast.makeText(this, R.string.weather_offline, Toast.LENGTH_LONG).show();
                return;
            }
            if (places.isEmpty()) {
                Toast.makeText(this, R.string.weather_no_match, Toast.LENGTH_LONG).show();
                return;
            }
            int at = screen.column.indexOfChild(query) + 1;
            for (Weather.Place p : places) {
                TextView row = screen.row(p.detail.isEmpty() ? p.name : p.name + "  ·  " + p.detail);
                screen.column.removeView(row);
                screen.column.addView(row, at++);
                row.setOnClickListener(v -> {
                    Weather.setPlace(this, p);
                    SettingsScreen.bump(this);
                    finish();
                });
                results.add(row);
            }
            results.get(0).requestFocus();
        });
    }
}
