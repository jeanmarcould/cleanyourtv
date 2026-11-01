package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Weather glyphs drawn with paths, in three sets: outline (white lines), filled (white shapes) and colourful.
 * Clouds are one merged outline, so an outlined cloud has no lines inside it.
 */
final class WeatherIcon {
    static final int OUTLINE = 0, FILLED = 1, COLOR = 2;

    private static final int WHITE = 0xFFFFFFFF;

    private WeatherIcon() {}

    static int styleFrom(String name) {
        return "filled".equals(name) ? FILLED : "color".equals(name) ? COLOR : OUTLINE;
    }

    /** Draws the glyph for a Weather category centred on (cx, cy), about 2*s wide. */
    static void draw(Canvas c, int category, boolean night, float cx, float cy, float s, Paint paint, int style) {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        float sw = Math.max(1.5f, s * 0.11f);
        switch (category) {
            case Weather.CLEAR:
                if (night) moon(c, cx, cy, s * 0.9f, paint, style, sw);
                else sun(c, cx, cy, s * 0.82f, paint, style, sw, null);
                break;
            case Weather.PARTLY: {
                Path cloud = cloud(cx + s * 0.14f, cy + s * 0.2f, s * 0.82f);
                if (night) moon(c, cx - s * 0.32f, cy - s * 0.3f, s * 0.7f, paint, style, sw);
                else sun(c, cx - s * 0.36f, cy - s * 0.36f, s * 0.62f, paint, style, sw, cloud);
                drawCloud(c, cloud, paint, style, sw, true);
                break;
            }
            case Weather.FOG:
                drawCloud(c, cloud(cx, cy - s * 0.25f, s * 0.86f), paint, style, sw, false);
                line(c, cx - s * 0.62f, cy + s * 0.5f, cx + s * 0.62f, cy + s * 0.5f, paint, style == COLOR ? 0xFFB8C4D4 : WHITE, sw);
                line(c, cx - s * 0.4f, cy + s * 0.78f, cx + s * 0.4f, cy + s * 0.78f, paint, style == COLOR ? 0xFFB8C4D4 : WHITE, sw);
                break;
            case Weather.RAIN:
                drawCloud(c, cloud(cx, cy - s * 0.22f, s * 0.9f), paint, style, sw, false);
                for (int i = -1; i <= 1; i++) {
                    float x = cx + i * s * 0.4f;
                    line(c, x + s * 0.07f, cy + s * 0.46f, x - s * 0.07f, cy + s * 0.8f, paint, style == COLOR ? 0xFF5AB4FF : WHITE, sw);
                }
                break;
            case Weather.SNOW:
                drawCloud(c, cloud(cx, cy - s * 0.22f, s * 0.9f), paint, style, sw, false);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(WHITE);
                for (int i = -1; i <= 1; i++) c.drawCircle(cx + i * s * 0.4f, cy + (i == 0 ? 0.78f : 0.6f) * s, s * 0.09f, paint);
                break;
            case Weather.STORM: {
                drawCloud(c, cloud(cx, cy - s * 0.24f, s * 0.9f), paint, style, sw, false);
                Path bolt = new Path();
                bolt.moveTo(cx + s * 0.1f, cy + s * 0.26f);
                bolt.lineTo(cx - s * 0.2f, cy + s * 0.64f);
                bolt.lineTo(cx, cy + s * 0.64f);
                bolt.lineTo(cx - s * 0.12f, cy + s * 0.98f);
                bolt.lineTo(cx + s * 0.3f, cy + s * 0.5f);
                bolt.lineTo(cx + s * 0.07f, cy + s * 0.5f);
                bolt.close();
                shape(c, bolt, paint, style, sw, style == COLOR ? 0xFFFFD23C : WHITE);
                break;
            }
            default:
                drawCloud(c, cloud(cx, cy, s), paint, style, sw, false);
        }
    }

    /** Fill or outline one closed shape according to the set. */
    private static void shape(Canvas c, Path p, Paint paint, int style, float sw, int color) {
        paint.setColor(color);
        if (style == OUTLINE) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(sw);
        } else {
            paint.setStyle(Paint.Style.FILL);
        }
        c.drawPath(p, paint);
    }

    private static void line(Canvas c, float x1, float y1, float x2, float y2, Paint paint, int color, float sw) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sw);
        paint.setColor(color);
        c.drawLine(x1, y1, x2, y2, paint);
    }

    /** The cloud's merged outline: three circles and a rounded base, united. */
    private static Path cloud(float cx, float cy, float s) {
        Path a = new Path();
        a.addCircle(cx - s * 0.36f, cy + s * 0.12f, s * 0.3f, Path.Direction.CW);
        Path b = new Path();
        b.addCircle(cx - s * 0.02f, cy - s * 0.1f, s * 0.42f, Path.Direction.CW);
        Path d = new Path();
        d.addCircle(cx + s * 0.4f, cy + s * 0.14f, s * 0.28f, Path.Direction.CW);
        Path base = new Path();
        base.addRoundRect(new RectF(cx - s * 0.66f, cy + s * 0.1f, cx + s * 0.68f, cy + s * 0.42f), s * 0.16f, s * 0.16f, Path.Direction.CW);
        a.op(b, Path.Op.UNION);
        a.op(d, Path.Op.UNION);
        a.op(base, Path.Op.UNION);
        return a;
    }

    /** {@code halo}: a dark rim round a cloud that overlaps a sun or moon, so the two stay apart. */
    private static void drawCloud(Canvas c, Path cloud, Paint paint, int style, float sw, boolean halo) {
        if (halo && style != OUTLINE) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(sw * 1.8f);
            paint.setColor(0x66000000);
            c.drawPath(cloud, paint);
        }
        shape(c, cloud, paint, style, sw, style == COLOR ? 0xFFEAF1F8 : WHITE);
    }

    /** {@code hide}: a cloud in front; an outlined sun leaves it (and a gap round it) out, so lines do not cross it. */
    private static void sun(Canvas c, float cx, float cy, float s, Paint paint, int style, float sw, Path hide) {
        c.save();
        if (hide != null && style == OUTLINE) {
            Path gap = new Path(hide);
            Path rim = new Path(hide);
            // a stroked copy of the cloud as the gap: grow it by drawing it into a path with a wider outline
            android.graphics.Matrix grow = new android.graphics.Matrix();
            RectF b = new RectF();
            rim.computeBounds(b, true);
            grow.setScale(1.12f, 1.12f, b.centerX(), b.centerY());
            gap.transform(grow);
            c.clipOutPath(gap);
        }
        int body = style == COLOR ? 0xFFFFC83C : WHITE, rays = style == COLOR ? 0xFFFFA726 : WHITE;
        paint.setColor(body);
        if (style == OUTLINE) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(sw);
        } else {
            paint.setStyle(Paint.Style.FILL);
        }
        c.drawCircle(cx, cy, s * 0.4f, paint);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            line(c, cx + cos * s * 0.62f, cy + sin * s * 0.62f, cx + cos * s * 0.92f, cy + sin * s * 0.92f, paint, rays, sw);
        }
        c.restore();
    }

    private static void moon(Canvas c, float cx, float cy, float s, Paint paint, int style, float sw) {
        Path disk = new Path();
        disk.addCircle(cx, cy, s * 0.6f, Path.Direction.CW);
        Path bite = new Path();
        bite.addCircle(cx + s * 0.34f, cy - s * 0.22f, s * 0.52f, Path.Direction.CW);
        disk.op(bite, Path.Op.DIFFERENCE);
        shape(c, disk, paint, style, sw, style == COLOR ? 0xFFF6E7A6 : WHITE);
    }
}
