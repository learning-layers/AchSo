package fi.aalto.legroup.achso.views.utilities;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.AttrRes;
import android.util.TypedValue;

import fi.aalto.legroup.achso.R;

/**
 * Utility class for resolving colours from the current theme.
 */
public final class ThemeColors {

    /**
     * Returns the primary colour of the theme.
     * @param context Context with the theme.
     */
    public static int getPrimaryColor(Context context) {
        return resolveThemeAttribute(context, R.attr.colorPrimary).data;
    }

    /**
     * Returns the dark primary colour of the theme.
     * @param context Context with the theme.
     */
    public static int getDarkPrimaryColor(Context context) {
        return resolveThemeAttribute(context, R.attr.colorPrimaryDark).data;
    }

    /**
     * Returns the accent colour of the theme.
     * @param context Context with the theme.
     */
    public static int getAccentColor(Context context) {
        return resolveThemeAttribute(context, R.attr.colorAccent).data;
    }

    /**
     * Resolves a value for an attribute from the current theme.
     * @param context   Context with the theme.
     * @param attribute Attribute to resolve.
     * @return Resolved typed value.
     */
    private static TypedValue resolveThemeAttribute(Context context, @AttrRes int attribute) {
        Resources.Theme theme = context.getTheme();
        TypedValue value = new TypedValue();

        theme.resolveAttribute(attribute, value, true);

        return value;
    }

}
