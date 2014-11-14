package fi.aalto.legroup.achso.util;

import android.content.Context;

/**
 * Utility class for converting between px and dp units.
 *
 * @author Leo Nikkilä
 */
public final class DimensionUnits {

    public static float pxToDp(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

}
