package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

/**
 * A frosted, darkened panel behind the greeting, clock and date, sized to their text (the header block itself runs
 * the width of the screen), so they stay readable over a bright wallpaper. Draws a little outside its owner.
 */
final class LeftFrost extends Drawable {
    private final View owner, root;
    private final Frost.Source source;
    private final TextView[] texts;
    private final float density;
    private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final RectF rect = new RectF();
    private final RectF reported = new RectF();
    private java.util.function.Consumer<RectF> onPanel;

    /** Called (from draw) with the panel's rectangle in root coordinates whenever it moves or resizes. */
    void setOnPanel(java.util.function.Consumer<RectF> listener) {
        onPanel = listener;
    }

    LeftFrost(View owner, View root, Frost.Source source, float density, TextView... texts) {
        this.owner = owner;
        this.root = root;
        this.source = source;
        this.texts = texts;
        this.density = density;
        tint.setColor(0x33000000);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(density);
        edge.setColor(0x26FFFFFF);
    }

    @Override
    public void draw(Canvas c) {
        boolean any = false;
        int[] o = new int[2], a = new int[2];
        owner.getLocationInWindow(o);
        for (TextView t : texts) {
            if (t == null || t.getVisibility() != View.VISIBLE || t.getWidth() == 0 || t.getText().length() == 0) continue;
            t.getLocationInWindow(a);
            CharSequence text = t.getText();
            // measureText leaves out letter-spacing, which the header's small caps lines use a lot of.
            float spaced = t.getPaint().measureText(text.toString()) + t.getLetterSpacing() * t.getTextSize() * text.length();
            float w = Math.min(t.getWidth(), spaced + t.getCompoundPaddingLeft() + t.getCompoundPaddingRight());
            float l = a[0] - o[0], tp = a[1] - o[1];
            if (!any) {
                rect.set(l, tp, l + w, tp + t.getHeight());
                any = true;
            } else {
                rect.union(l, tp, l + w, tp + t.getHeight());
            }
        }
        if (!any) return;
        // One dock tile in size (as the weather panel), grown only when the text needs more room.
        float padX = WeatherPanel.PAD_DP * density;
        float width = Math.max(WeatherPanel.TILE_W_DP * density, rect.width() + 2 * padX);
        float height = WeatherPanel.TILE_H_DP * density;
        // The date line is a fixed-width box with its text centred: the panel is that box plus the padding.
        TextView box = texts[0];
        int[] bl = new int[2];
        float left = rect.left - padX, top = rect.centerY() - height / 2f;
        if (box != null && box.getWidth() > 0) {
            box.getLocationInWindow(bl);
            left = bl[0] - o[0] - padX;
            width = Math.max(width, box.getWidth() + 2 * padX);
        }
        rect.set(left, top, left + width, top + height);
        float radius = WeatherPanel.RADIUS_DP * density;
        Frost.panel(c, owner, root, source, rect, radius, density, owner.isFocused());
        if (onPanel != null) {
            int[] r = new int[2];
            root.getLocationInWindow(r);
            RectF inRoot = new RectF(rect.left + o[0] - r[0], rect.top + o[1] - r[1], rect.right + o[0] - r[0], rect.bottom + o[1] - r[1]);
            if (!inRoot.equals(reported)) {
                reported.set(inRoot);
                onPanel.accept(new RectF(inRoot));
            }
        }
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter colorFilter) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
