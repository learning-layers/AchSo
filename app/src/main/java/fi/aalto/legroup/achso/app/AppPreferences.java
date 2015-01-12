package fi.aalto.legroup.achso.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Holds preference keys and convenience methods.
 *
 * TODO: Some syntactic sugar for retrieving preferences and their default values. Use annotations?
 *
 * @author Leo Nikkilä
 */
public final class AppPreferences {

    /**
     * Name for the app preference collection.
     */
    public static final String NAME = "AchSoPreferences";

    /**
     * The account name used for logging in automatically.
     */
    public static final String AUTO_LOGIN_ACCOUNT = "AUTO_LOGIN_ACCOUNT";

    /**
     * Whether migration is needed or not.
     */
    public static final String SHOULD_MIGRATE = "SHOULD_MIGRATE";

    public static SharedPreferences with(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

}
