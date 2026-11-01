package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Reorder mode's cues: a banner that says what the remote does right now (and that Back leaves the mode), and
 * chevrons around the carried tile for each way it can still move. Not focusable, never takes a key.
 */
final class ReorderCue extends FrameLayout {
    private final TextView banner;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;
    private RectF target;
    private boolean left, up, right, down;

    ReorderCue(Context ctx, int accent) {
        super(ctx);
        density = ctx.getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        setFocusable(false);
        setClickable(false);
        setVisibility(GONE);
        paint.setColor(0xB3FFFFFF);   // thin, translucent white: a cue, not a decoration
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2.5f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        banner = new TextView(ctx);
        banner.setTextColor(0xFFFFFFFF);
        banner.setTextSize(15);
        banner.setGravity(Gravity.CENTER);
        banner.setPadding(dp(18), dp(8), dp(18), dp(8));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xB010151F);
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), accent);
        banner.setBackground(bg);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.TOP);
        lp.topMargin = dp(14);
        addView(banner, lp);
    }

    /** Top of the screen, in the gap between the clock block (left) and the top-right buttons. */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        LayoutParams lp = (LayoutParams) banner.getLayoutParams();
        lp.leftMargin = Math.round(w * 0.45f);
        banner.setMaxWidth(Math.round(w * 0.37f));
        banner.setLayoutParams(lp);
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    void show(CharSequence text) {
        banner.setText(text);
        setVisibility(VISIBLE);
        invalidate();
    }

    void hide() {
        target = null;
        setVisibility(GONE);
    }

    /** Chevrons around {@code tile} (in this view's coordinates) for each direction that is still open. */
    void setTarget(RectF tile, boolean left, boolean up, boolean right, boolean down) {
        this.target = tile;
        this.left = left;
        this.up = up;
        this.right = right;
        this.down = down;
        invalidate();
    }

    void clearTarget() {
        if (target != null) {
            target = null;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (target == null) return;
        float s = dp(7);        // half the chevron's size
        float gap = dp(10);
        float cx = target.centerX(), cy = target.centerY();
        if (left) chevron(c, target.left - gap, cy, -1, 0, s);
        if (right) chevron(c, target.right + gap, cy, 1, 0, s);
        if (up) chevron(c, cx, target.top - gap, 0, -1, s);
        if (down) chevron(c, cx, target.bottom + gap, 0, 1, s);
    }

    /** An open chevron whose tip points along (dx, dy). */
    private void chevron(Canvas c, float x, float y, int dx, int dy, float s) {
        path.reset();
        path.moveTo(x - dx * s + dy * s, y - dy * s + dx * s);
        path.lineTo(x + dx * s, y + dy * s);                      // tip
        path.lineTo(x - dx * s - dy * s, y - dy * s - dx * s);
        c.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        return false;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
