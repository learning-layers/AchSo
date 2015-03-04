package fi.aalto.legroup.achso.views.utilities;

import android.content.Context;

/**
 * Utility class for converting between px and dp units.
 */
public final class DimensionUnits {

    public static float pxToDp(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

}
