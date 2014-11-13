package fi.aalto.legroup.achso.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.LoginActivity;
import fi.aalto.legroup.achso.state.LoginManager;
import fi.aalto.legroup.achso.util.App;

/**
 * Created by lassi on 4.11.14.
 */
public class AuthenticationHelper {
    public static final int ACTIVITY_LOGIN = 2;
    public static final int ACTIVITY_LOGOUT = 3;

    public static void logout(Activity activity) {
        App.loginManager.logoutExplicitly();
        Context context = activity.getApplicationContext();
        Toast message = Toast.makeText(context, context.getString(R.string.logged_out), Toast.LENGTH_SHORT);
        message.show();
    }

    public static void login(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        activity.startActivityForResult(intent, ACTIVITY_LOGIN);
    }

    public static void loginStateDidChange(Context applicationContext) {
        switch (App.loginManager.getState()) {
            case LOGGED_IN:
                String name = App.loginManager.getUserInfo().get("name").getAsString();
                String welcome = String.format(applicationContext.getString(R.string.logged_in_as), name);

                Toast.makeText(applicationContext, welcome,
                        Toast.LENGTH_LONG).show();
                break;

            case LOGGED_OUT:
                Toast.makeText(applicationContext, R.string.logged_out,
                        Toast.LENGTH_LONG).show();
                break;
        }
    }

    public static void loginDidFail(Context applicationContext, Intent intent) {
        String error = intent.getStringExtra(LoginManager.KEY_MESSAGE);
        String message = String.format(applicationContext.getString(R.string.login_error), error);

        Toast.makeText(applicationContext, message,
                Toast.LENGTH_LONG).show();
    }

    public static void loginResult(Activity activity) {
        activity.invalidateOptionsMenu();
        Context context = activity.getApplicationContext();
        if (App.loginManager.getUserInfo() != null) {
            String name = App.loginManager.getUserInfo().get("name").getAsString();
            String welcome = String.format(context.getString(R.string.logged_in_as), name);
            Toast message = Toast.makeText(context, welcome, Toast.LENGTH_SHORT);
            message.show();
        }
    }
}
