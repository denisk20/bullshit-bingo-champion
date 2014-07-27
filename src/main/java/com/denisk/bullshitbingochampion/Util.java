package com.denisk.bullshitbingochampion;

import android.content.Context;

/**
 * @author denisk
 * @since 27.07.14.
 */
public class Util {
    public static int dpToPix(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
