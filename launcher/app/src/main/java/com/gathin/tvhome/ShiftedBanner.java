package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * A banner on a solid black backing, moved down by a fraction of its height. For banners whose own black field stops
 * short of the bottom edge (Live TV x ships a 320x180 banner whose black rectangle is 160 high), which left a
 * transparent strip under the logo and put the logo above the middle of the tile.
 */
final class ShiftedBanner extends Drawable {
    private final Drawable banner;
    private final float shift;
    private final Paint black = new Paint();

    ShiftedBanner(Drawable banner, float shiftFraction) {
        this.banner = banner;
        this.shift = shiftFraction;
        black.setColor(0xFF000000);
    }

    @Override
    protected void onBoundsChange(android.graphics.Rect bounds) {
        banner.setBounds(0, 0, bounds.width(), bounds.height());
    }

    @Override
    public void draw(Canvas canvas) {
        android.graphics.Rect b = getBounds();
        canvas.drawRect(b, black);
        canvas.save();
        canvas.translate(b.left, b.top + b.height() * shift);
        banner.draw(canvas);
        canvas.restore();
    }

    @Override public int getIntrinsicWidth() { return banner.getIntrinsicWidth(); }
    @Override public int getIntrinsicHeight() { return banner.getIntrinsicHeight(); }
    @Override public void setAlpha(int alpha) { banner.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter colorFilter) { banner.setColorFilter(colorFilter); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
