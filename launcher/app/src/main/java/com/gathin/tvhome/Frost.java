package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Frosted glass without GPU backdrop blur: the wallpaper is blurred once, at a tiny size, when it is decoded
 * (WallpaperPlayer), and glass surfaces paint that picture where they sit, lined up with the full-screen
 * CENTER_CROP wallpaper behind them so the glass reads as a blurred window onto it.
 */
final class Frost {
    private Frost() {}

    /** Where the current blurred wallpaper comes from; the bitmap is null until the first picture is decoded. */
    interface Source {
        Bitmap blurred();

        /** The picture being faded out while a new one fades in; null outside a crossfade. */
        default Bitmap previous() {
            return null;
        }

        /** How far the new picture has faded in, 0..1. */
        default float blend() {
            return 1f;
        }
    }

    private static final int BLUR_WIDTH = 160;
    private static final int BLUR_RADIUS = 5;
    private static final int BLUR_PASSES = 3;

    private static final Paint BITMAP = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private static final Matrix MATRIX = new Matrix();

    /** A small, strongly blurred, opaque copy of a software bitmap. Runs off the UI thread (the decoder's). */
    static Bitmap blur(Bitmap source) {
        int w = BLUR_WIDTH;
        int h = Math.max(1, Math.round(w * (float) source.getHeight() / source.getWidth()));
        Bitmap scaled = Bitmap.createScaledBitmap(source, w, h, true);
        Bitmap out = scaled.copy(Bitmap.Config.ARGB_8888, true);
        if (scaled != source) scaled.recycle();
        if (out == null) return null;
        int[] px = new int[w * h];
        out.getPixels(px, 0, w, 0, 0, w, h);
        int[] tmp = new int[w * h];
        for (int i = 0; i < BLUR_PASSES; i++) {
            box(px, tmp, w, h, true);
            box(tmp, px, w, h, false);
        }
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    /** One box-blur pass along rows (horizontal) or columns, edges clamped. */
    private static void box(int[] in, int[] out, int w, int h, boolean horizontal) {
        int lines = horizontal ? h : w, length = horizontal ? w : h;
        int stride = horizontal ? 1 : w, lineStep = horizontal ? w : 1;
        int window = BLUR_RADIUS * 2 + 1;
        for (int line = 0; line < lines; line++) {
            int base = line * lineStep;
            int r = 0, g = 0, b = 0;
            for (int k = -BLUR_RADIUS; k <= BLUR_RADIUS; k++) {
                int p = in[base + Math.max(0, Math.min(length - 1, k)) * stride];
                r += (p >> 16) & 0xFF;
                g += (p >> 8) & 0xFF;
                b += p & 0xFF;
            }
            for (int i = 0; i < length; i++) {
                out[base + i * stride] = 0xFF000000 | (r / window << 16) | (g / window << 8) | (b / window);
                int add = in[base + Math.min(length - 1, i + BLUR_RADIUS + 1) * stride];
                int drop = in[base + Math.max(0, i - BLUR_RADIUS) * stride];
                r += ((add >> 16) & 0xFF) - ((drop >> 16) & 0xFF);
                g += ((add >> 8) & 0xFF) - ((drop >> 8) & 0xFF);
                b += (add & 0xFF) - (drop & 0xFF);
            }
        }
    }

    /**
     * Paints the blurred wallpaper over the whole root, in {@code owner}'s canvas: the caller clips to the glass
     * shape first. The picture is scaled to cover the root and centred, as the wallpaper's CENTER_CROP does.
     */
    static void paintBackdrop(Canvas c, View owner, View root, Source source) {
        Bitmap blurred = source.blurred();
        if (blurred == null || root.getWidth() == 0) return;
        int[] o = new int[2], r = new int[2];
        owner.getLocationInWindow(o);
        root.getLocationInWindow(r);
        float rw = root.getWidth(), rh = root.getHeight();
        c.save();
        c.translate(r[0] - o[0], r[1] - o[1]);
        Bitmap previous = source.previous();
        float blend = source.blend();
        if (previous != null && blend < 1f) {
            draw(c, previous, rw, rh, 255);
            draw(c, blurred, rw, rh, Math.round(255 * blend));   // the glass fades with the wallpaper behind it
        } else {
            draw(c, blurred, rw, rh, 255);
        }
        c.restore();
    }

    private static void draw(Canvas c, Bitmap b, float rw, float rh, int alpha) {
        float s = Math.max(rw / b.getWidth(), rh / b.getHeight());
        MATRIX.setScale(s, s);
        MATRIX.postTranslate((rw - b.getWidth() * s) / 2f, (rh - b.getHeight() * s) / 2f);
        BITMAP.setAlpha(alpha);
        c.drawBitmap(b, MATRIX, BITMAP);
        BITMAP.setAlpha(255);
    }

    private static final Path SHAPE = new Path();
    private static final Paint TINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint EDGE = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * The header panels' glass (clock and weather): the blurred wallpaper in a rounded rectangle, a light dark veil
     * and a thin edge, brighter while the panel has focus. One routine for both, so they always match.
     * {@code rect} is in {@code owner}'s canvas coordinates.
     */
    static void panel(Canvas c, View owner, View root, Source source, RectF rect, float radius, float density, boolean focused) {
        SHAPE.reset();
        SHAPE.addRoundRect(rect, radius, radius, Path.Direction.CW);
        c.save();
        c.clipPath(SHAPE);
        paintBackdrop(c, owner, root, source);
        TINT.setColor(0x33000000);
        c.drawPath(SHAPE, TINT);
        c.restore();
        EDGE.setStyle(Paint.Style.STROKE);
        EDGE.setStrokeWidth(focused ? 2.5f * density : density);
        EDGE.setColor(focused ? 0xE6FFFFFF : 0x26FFFFFF);
        c.drawRoundRect(rect, radius, radius, EDGE);
    }
}
