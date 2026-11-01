package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

/** The TV's own home screen (Google TV), still installed beside this launcher, and a way to open it directly. */
final class StockHome {
    private static final String[] KNOWN = {"com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher",
            "com.google.android.leanbacklauncher", "com.mitv.tvhome"};

    private StockHome() {}

    /** The stock launcher's Home activity, or null while none is installed and enabled. */
    static ComponentName find(Context ctx) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ComponentName other = null;
        for (ResolveInfo ri : ctx.getPackageManager().queryIntentActivities(home, 0)) {
            if (ri.activityInfo == null || !ri.activityInfo.exported) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName())) continue;
            ComponentName c = new ComponentName(pkg, ri.activityInfo.name);
            for (String known : KNOWN) if (known.equals(pkg)) return c;
            if (other == null) other = c;
        }
        return other;
    }

    /** Opens it by explicit component. The Home key still brings this launcher back. */
    static boolean open(Context ctx) {
        ComponentName c = find(ctx);
        if (c == null) return false;
        ctx.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return true;
    }
}
