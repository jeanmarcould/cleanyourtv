package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The wallpaper collection as a grid of tiles, newest first, each with the date it arrived. OK on a tile opens a
 * large preview with the photo's name, credit and date and two choices: delete it, or go back to the list. Deleting
 * makes Home rescan the folder when it comes back.
 */
public class WallpaperManagerActivity extends Activity {
    private static final int PER_ROW = 5;
    /** The updater names a download wm_<epoch-ms>_<n>.jpg; that timestamp is when the photo arrived. */
    private static final Pattern DOWNLOADED = Pattern.compile("^wm_(\\d+)_\\d+\\.(?:jpe?g|png|webp)$", Pattern.CASE_INSENSITIVE);

    private final ExecutorService loader = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private File dir;
    private Map<String, String> credits;
    private List<File> pictures;
    private LinearLayout grid;
    private TextView title;
    private FrameLayout detail;
    private View listColumn;
    private TextView deleteButton, backButton;
    private ImageView preview;
    private TextView detailName;
    private File shown;
    private int shownIndex;
    private boolean changed;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        File base = Config.dir(this);
        dir = base == null ? null : new File(base, "wallpapers");
        credits = new TreeMap<>(WallpaperCaptions.read(dir));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0B0F17);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(Math.round(64 * density), Math.round(36 * density), Math.round(64 * density), Math.round(16 * density));
        title = new TextView(this);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, Math.round(12 * density));
        column.addView(title);
        ScrollView scroll = new ScrollView(this);
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(grid);
        column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        listColumn = column;
        root.addView(column);
        root.addView(buildDetail());
        setContentView(root);
        load();
        buildGrid(0);
    }

    private void load() {
        File[] files = dir == null ? null : dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg|png|webp)"));
        pictures = new ArrayList<>(Arrays.asList(files == null ? new File[0] : files));
        pictures.sort(Comparator.comparingLong(File::lastModified).reversed());
    }

    private void buildGrid(int focusIndex) {
        grid.removeAllViews();
        title.setText(getString(R.string.wallpapers_title, pictures.size()));
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int gap = Math.round(10 * density);
        int tileW = (screenW - Math.round(128 * density) - gap * (PER_ROW - 1)) / PER_ROW;
        int tileH = tileW * 9 / 16;
        LinearLayout row = null;
        View toFocus = null;
        for (int i = 0; i < pictures.size(); i++) {
            if (i % PER_ROW == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.bottomMargin = gap;
                grid.addView(row, rp);
            }
            File f = pictures.get(i);
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = (i % PER_ROW == PER_ROW - 1) ? 0 : gap;
            row.addView(cell, lp);
            ImageView tile = new ImageView(this);
            tile.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.setBackgroundResource(R.drawable.settings_choice_bg);
            int pad = Math.round(4 * density);
            tile.setPadding(pad, pad, pad, pad);
            tile.setCropToPadding(true);   // the picture is cut to the tile, not drawn over its border
            tile.setFocusable(true);
            tile.setClickable(true);
            cell.addView(tile, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tileH + 2 * pad));
            TextView date = new TextView(this);
            date.setText(dateLabel(f));
            date.setTextColor(0xFF9AA4B2);
            date.setTextSize(12);
            date.setGravity(Gravity.CENTER);
            date.setSingleLine(true);
            date.setEllipsize(android.text.TextUtils.TruncateAt.END);
            date.setPadding(0, Math.round(4 * density), 0, 0);
            cell.addView(date);
            final int index = i;
            tile.setOnClickListener(v -> open(index));
            loader.execute(() -> {
                Bitmap b = decode(f, tileW * 2);
                if (b != null) main.post(() -> tile.setImageBitmap(b));
            });
            if (i == focusIndex) toFocus = tile;
        }
        if (toFocus != null) toFocus.requestFocus();
    }

    /** The large preview: the picture, its credit and file, and the two choices. */
    private View buildDetail() {
        detail = new FrameLayout(this);
        detail.setBackgroundColor(0xFF0B0F17);
        detail.setVisibility(View.GONE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Math.round(80 * density), Math.round(30 * density), Math.round(80 * density), Math.round(24 * density));
        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        detailName = new TextView(this);
        detailName.setTextColor(0xFFFFFFFF);
        detailName.setTextSize(17);
        detailName.setGravity(Gravity.CENTER);
        detailName.setPadding(0, Math.round(12 * density), 0, Math.round(12 * density));
        box.addView(detailName);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView delete = button(getString(R.string.wallpaper_delete));
        TextView back = button(getString(R.string.wallpaper_back));
        deleteButton = delete;
        backButton = back;
        delete.setNextFocusRightId(View.generateViewId());
        int deleteId = View.generateViewId(), backId = View.generateViewId();
        delete.setId(deleteId);
        back.setId(backId);
        delete.setNextFocusRightId(backId);
        delete.setNextFocusLeftId(deleteId);
        back.setNextFocusLeftId(deleteId);
        back.setNextFocusRightId(backId);
        delete.setNextFocusUpId(deleteId);
        delete.setNextFocusDownId(deleteId);
        back.setNextFocusUpId(backId);
        back.setNextFocusDownId(backId);
        buttons.addView(delete, new LinearLayout.LayoutParams(0, Math.round(52 * density), 1f));
        View spacer = new View(this);
        buttons.addView(spacer, new LinearLayout.LayoutParams(Math.round(16 * density), 1));
        buttons.addView(back, new LinearLayout.LayoutParams(0, Math.round(52 * density), 1f));
        box.addView(buttons, new LinearLayout.LayoutParams(Math.round(720 * density), ViewGroup.LayoutParams.WRAP_CONTENT));
        detail.addView(box);
        delete.setOnClickListener(v -> deleteShown());
        back.setOnClickListener(v -> closeDetail());
        return detail;
    }

    private TextView button(String label) {
        TextView t = new TextView(this, null, 0, R.style.SettingsRow);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void open(int index) {
        shownIndex = index;
        shown = pictures.get(index);
        String credit = credits.get(shown.getName());
        detailName.setText((credit == null ? shown.getName() : credit) + "\n" + shown.getName()
                + "\n" + dateLabel(shown));
        preview.setImageDrawable(null);
        // The grid behind must not take focus while the preview is up: that is what made Delete unreachable.
        listColumn.setVisibility(View.INVISIBLE);
        detail.setVisibility(View.VISIBLE);
        backButton.requestFocus();   // Back to list is the safe default; Left reaches Delete
        File f = shown;
        loader.execute(() -> {
            Bitmap b = decode(f, 1600);
            if (b != null) main.post(() -> {
                if (f.equals(shown)) preview.setImageBitmap(b);
            });
        });
    }

    private void closeDetail() {
        detail.setVisibility(View.GONE);
        listColumn.setVisibility(View.VISIBLE);
        shown = null;
        buildGrid(Math.min(shownIndex, Math.max(0, pictures.size() - 1)));
    }

    private void deleteShown() {
        if (shown == null) return;
        File f = shown;
        if (f.delete()) {
            credits.remove(f.getName());
            try {
                WallpaperUpdater.writeCredits(dir, credits);
            } catch (java.io.IOException ignored) {
                // the picture is gone; a stale credit line is harmless
            }
            changed = true;
            load();
        }
        closeDetail();
    }

    @Override
    public void onBackPressed() {
        if (detail.getVisibility() == View.VISIBLE) closeDetail();
        else super.onBackPressed();
    }

    /** When the picture arrived: the updater's timestamp in its name, else the file date (a theme push). */
    static long downloadedAt(File f) {
        Matcher m = DOWNLOADED.matcher(f.getName());
        if (m.matches()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
                // a truncated name: fall through to the file date
            }
        }
        return f.lastModified();
    }

    private static String dateLabel(File f) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(downloadedAt(f)));
    }

    private static Bitmap decode(File f, int width) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        if (o.outWidth <= 0) return null;
        o.inSampleSize = Math.max(1, o.outWidth / width);
        o.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(f.getPath(), o);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loader.shutdownNow();
        if (changed) SettingsScreen.bump(this);   // Home rescans the folder when it comes back
    }
}
