package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

/** Full-screen frosted wallpaper that fades in as the app drawer is pulled up, so the app tiles read over it. */
final class DrawerFrost extends View {
    private final Frost.Source source;
    private final View root;

    DrawerFrost(Context context, Frost.Source source, View root) {
        super(context);
        this.source = source;
        this.root = root;
        setAlpha(0f);
        setVisibility(INVISIBLE);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** 0 hides it, 1 is fully frosted. */
    void setProgress(float progress) {
        float p = Math.max(0f, Math.min(1f, progress));
        setAlpha(p);
        setVisibility(p <= 0.01f ? INVISIBLE : VISIBLE);
    }

    @Override
    protected void onDraw(Canvas c) {
        Frost.paintBackdrop(c, this, root, source);
        c.drawColor(0x66000000);   // a dark veil so labels stay readable over any picture
    }
}
