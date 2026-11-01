package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * Current weather in a frosted panel the size of one dock tile, beside the clock panel: the city in the date line's
 * small capitals, a glyph and the temperature on the time's line. Not interactive: its settings are under the gear.
 */
final class WeatherPanel extends View {
    /** One dock tile, about: the clock and weather panels share this size. */
    static final float TILE_W_DP = 114.5f, TILE_H_DP = 64f, RADIUS_DP = 18f, PAD_DP = 10f;
    /** A test build draws every glyph in a row, cycling through the three sets. */
    static final boolean DEBUG_STRIP = false;

    private final Frost.Source source;
    private final View root;
    private final float density;
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    /** Baselines, from the panel's top, of the clock panel's date line and time: the city and the temperature sit on them. */
    private float smallBaseline = -1, bigBaseline = -1;
    private String temperature = "";
    private String city = "";
    private int category;
    private boolean night;
    private int iconStyle;
    private int debugStyle;

    WeatherPanel(Context context, Frost.Source source, View root, float density, Typeface bigFace, float bigSizePx,
                 Typeface smallFace, float smallSizePx, float smallSpacing) {
        super(context);
        this.source = source;
        this.root = root;
        this.density = density;
        text.setColor(0xFFFFFFFF);
        text.setTypeface(bigFace);
        text.setTextSize(bigSizePx);
        small.setColor(0x8CFFFFFF);
        small.setTypeface(smallFace);
        small.setTextSize(smallSizePx);
        small.setLetterSpacing(smallSpacing);
        setFocusable(false);
        setContentDescription(context.getString(R.string.weather_settings));
        refresh();
        if (DEBUG_STRIP) postDelayed(this::cycleDebug, 6000);
    }

    private void cycleDebug() {
        debugStyle = (debugStyle + 1) % 3;
        invalidate();
        postDelayed(this::cycleDebug, 6000);
    }

    void setBaselines(float small, float big) {
        if (small == smallBaseline && big == bigBaseline) return;
        smallBaseline = small;
        bigBaseline = big;
        invalidate();
    }

    /** Re-reads the last reading (and the unit and icon set) after a fetch or a settings change. */
    void refresh() {
        Context ctx = getContext();
        boolean ready = Weather.hasReading(ctx);
        temperature = ready ? Weather.temperature(ctx) + "°" : "--°";
        category = ready ? Weather.category(ctx) : Weather.CLOUDY;
        night = ready && Weather.night(ctx);
        city = Weather.placeName(ctx).toUpperCase(java.util.Locale.getDefault());
        iconStyle = WeatherIcon.styleFrom(Weather.iconSet(ctx));
        requestLayout();
        invalidate();
    }

    private float iconBox() {
        Rect digits = new Rect();
        text.getTextBounds("0", 0, 1, digits);
        return digits.height() * 1.15f;
    }

    private float smallWidth(String s) {
        return small.measureText(s) + small.getLetterSpacing() * small.getTextSize() * s.length();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        float pad = PAD_DP * density, gap = 8 * density;
        float content = Math.max(iconBox() + gap + text.measureText(temperature), smallWidth(city));
        float width = Math.max(TILE_W_DP * density, content + pad * 2);
        if (DEBUG_STRIP) width = 7 * iconBox() * 1.5f + pad * 2;
        setMeasuredDimension(resolveSize(Math.round(width), widthSpec), resolveSize(Math.round(TILE_H_DP * density), heightSpec));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float pad = PAD_DP * density, gap = 8 * density, radius = RADIUS_DP * density;
        rect.set(0, 0, w, h);
        Frost.panel(c, this, root, source, rect, radius, density, false);
        float box = iconBox();
        float smallY = smallBaseline >= 0 ? smallBaseline : h * 0.3f;
        float bigY = bigBaseline >= 0 ? bigBaseline : h * 0.82f;
        Rect digits = new Rect();
        text.getTextBounds("0", 0, 1, digits);
        if (DEBUG_STRIP) {
            for (int i = 0; i < 7; i++) {
                WeatherIcon.draw(c, i, false, pad + box * 0.75f + i * box * 1.5f, h / 2f, box / 2f, glyph, debugStyle);
            }
            return;
        }
        String line = city;
        float room = w - pad * 2;
        while (line.length() > 3 && smallWidth(line) > room) line = line.substring(0, line.length() - 2).trim() + "…";
        // Both lines centred in the panel, as the clock panel's are. The city's trailing letter-spacing is not visible.
        float lineWidth = smallWidth(line) - small.getLetterSpacing() * small.getTextSize();
        c.drawText(line, (w - lineWidth) / 2f, smallY, small);
        float row = box + gap + text.measureText(temperature);
        float start = (w - row) / 2f;
        WeatherIcon.draw(c, category, night, start + box / 2f, bigY - digits.height() / 2f, box / 2f, glyph, iconStyle);
        c.drawText(temperature, start + box + gap, bigY, text);
    }
}
