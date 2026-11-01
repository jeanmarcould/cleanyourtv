package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared layout for the small settings screens: a title and a column of focusable rows on a dark background. */
final class SettingsScreen {
    final LinearLayout column;
    private final Activity activity;
    private final float density;

    SettingsScreen(Activity activity, int titleRes) {
        this.activity = activity;
        density = activity.getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(0xFF0B0F17);
        column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.round(640 * density), ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(column, lp);
        TextView title = new TextView(activity);
        title.setText(titleRes);
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, Math.round(18 * density));
        column.addView(title);
        activity.setContentView(root);
    }

    /** A focusable row; the caller sets the click action. */
    TextView row(CharSequence label) {
        TextView row = new TextView(activity, null, 0, R.style.SettingsRow);
        row.setText(label);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(52 * density));
        lp.bottomMargin = Math.round(8 * density);
        column.addView(row, lp);
        return row;
    }

    /** A dropdown: a list dialog with the current choice marked; OK picks one and closes it. */
    static void choose(Activity activity, int titleRes, CharSequence[] labels, int checked, java.util.function.IntConsumer picked) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(titleRes)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    picked.accept(which);
                    d.dismiss();
                })
                .create();
        dialog.show();
        if (checked >= 0) dialog.getListView().setSelection(checked);
    }

    /** Tells Home to rebuild when it comes back (its onResume watches this count). */
    static void bump(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        p.edit().putLong(Config.SETTINGS_REVISION_KEY, p.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
        return 0;
    }

    static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) if (values[i] == value) return i;
        return 0;
    }
}
