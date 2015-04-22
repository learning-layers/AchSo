package fi.aalto.legroup.achso.app;

/**
 * Holds preference keys for programmatic usage.
 */
public final class AppPreferences {

    private AppPreferences() {
        // Static access only
    }

    /**
     * The account name used for logging in automatically.
     */
    public static final String AUTO_LOGIN_ACCOUNT = "AUTO_LOGIN_ACCOUNT";

    // NOTE:
    // The preference keys below are user-facing. If you change them here, change them in
    // preferences.xml also.

    /**
     * The base duration for an annotation pause.
     */
    public static final String ANNOTATION_PAUSE_DURATION = "ANNOTATION_PAUSE_DURATION";

    /**
     * Layers box base URL.
     */
    public static final String LAYERS_BOX_URL = "LAYERS_BOX_URL";

    /**
     * Whether analytics data should be sent.
     */
    public static final String ANALYTICS_OPT_IN = "ANALYTICS_OPT_IN";

}
