package com.denisk.bullshitbingochampion;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

/**
 * @author denisk
 * @since 27.07.14.
 */
public class Util {
    public static int dpToPix(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * @param context A context of the current application.
     * @return The application name of the current application.
     */
    public static String getApplicationName(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (final PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "(unknown)");
    }

    public static int getDim(int size) {
        double sqrt = Math.sqrt(size);
        double floor = Math.floor(sqrt + 0.5);
        if (Math.abs(floor - sqrt) > 0.1) {
            return -1;
        }

        return (int) Math.round(sqrt);
    }
}
