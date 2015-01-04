package com.denisk.bullshitbingochampion;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

/**
 * @author denisk
 * @since 27.07.14.
 */
public class Util {
    public static final float IDEAL_FONT_SIZE_PX_FOR_1280_800 = 120f;
    public static final double LANDSCAPE_WIDTH_HEIGHT_COEFF = 1280. / 800;

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

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static float getCardFontSize(Context context, int dim, boolean land) {
        String prefix = getFontPrefix(land);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        float finalFontSize;
        String key = getFontKey(dim, prefix);
        float fontSize = sharedPreferences.getFloat(key, -1);

        if (fontSize < 0) {
            finalFontSize = IDEAL_FONT_SIZE_PX_FOR_1280_800 / dim;
            if (land) {
                finalFontSize *= LANDSCAPE_WIDTH_HEIGHT_COEFF;
            }

            sharedPreferences.edit().putFloat(key, finalFontSize).commit();
        } else {
            finalFontSize = fontSize;
        }

        return finalFontSize;
    }

    public static String getFontKey(int dim, String prefix) {
        return prefix + dim;
    }

    public static String getFontPrefix(boolean land) {
        return land ? "land" : "portrait";
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

}
