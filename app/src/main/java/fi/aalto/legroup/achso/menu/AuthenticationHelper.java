package fi.aalto.legroup.achso.menu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.LoginActivity;
import fi.aalto.legroup.achso.util.App;

/**
 * Created by lassi on 4.11.14.
 */
public class AuthenticationHelper {
    public static final int LOGIN_ACTIVITY_NUMBER = 2;
    public static final int LOGOUT_ACTIVITY_NUMBER = 3;

    public static void logout(Activity activity) {
        App.loginManager.logoutExplicitly();
        Context context = activity.getApplicationContext();
        Toast message = Toast.makeText(context, context.getString(R.string.logged_out), Toast.LENGTH_SHORT);
        message.show();
    }

    public static void login(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        activity.startActivityForResult(intent, LOGIN_ACTIVITY_NUMBER);
    }

    public static void loginResult(Activity activity, int resultCode, Intent data) {
        activity.invalidateOptionsMenu();
        Context context = activity.getApplicationContext();
        String name = App.loginManager.getUserInfo().get("name").getAsString();
        String welcome = String.format(context.getString(R.string.logged_in_as), name);
        Toast message = Toast.makeText(context, welcome, Toast.LENGTH_SHORT);
        message.show();
    }
}
