package fi.aalto.legroup.achso.app;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import java.util.Map;

import fi.aalto.legroup.achso.BuildConfig;
import fi.aalto.legroup.achso.R;

/**
 * Convenience methods and parameters for Google Analytics.
 */
public final class AppAnalytics {

    // Categories
    public static final String CATEGORY_VIDEOS = "Videos";

    // Actions
    public static final String ACTION_CREATE = "Create";

    private static Tracker tracker;

    private AppAnalytics() {
        // Static access only
    }

    public static void setup(Context context) {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);

        tracker = analytics.newTracker(context.getString(R.string.gaPropertyId));

        // Automatically track activities
        tracker.enableAutoActivityTracking(true);

        // Don't send the last octet of the IP address
        tracker.setAnonymizeIp(true);

        // Log analytics to the console on debug builds
        if (BuildConfig.DEBUG) {
            analytics.setDryRun(true);
            analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        }
    }

    public static Tracker getTracker() {
        return tracker;
    }

    public static void send(Map<String, String> params) {
        tracker.send(params);
    }

}
