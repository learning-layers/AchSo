package fi.aalto.legroup.achso.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.HashMap;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.fragment.FeedbackDialogFragment;
import fi.aalto.legroup.achso.service.OsTicketService;
import fi.aalto.legroup.achso.state.LoginManager;
import fi.aalto.legroup.achso.util.App;
import retrofit.Callback;
import retrofit.RestAdapter;

/**
 * Created by lassi on 12.11.14.
 */
public class SettingsHelper {
    public static void showAboutDialog(Activity activity) {
        //TODO: add a nice dialog including OS information
        String versionName = "-1";
        try {
            versionName = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("", "Version name could not be retrieved.");
        }
        new AlertDialog.Builder(activity)
                .setTitle(activity.getResources().getString(R.string.app_name) + " version " + versionName)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public static void showSendFeedback(Activity activity) {
        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        Fragment prev = activity.getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        DialogFragment feedbackFragment = FeedbackDialogFragment.newInstance(App.loginManager.getUserInfo().get("name").getAsString(), App.loginManager.getUserInfo().get("email").getAsString());

        feedbackFragment.show(ft, "dialog");
    }

    public static void sendFeedback(Activity activity, HashMap<String, String> map, Callback<String> callback) {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(activity.getString(R.string.feedbackServerUrl))
                .build();

        OsTicketService service = adapter.create(OsTicketService.class);
        service.sendFeedback(activity.getString(R.string.feedbackServerKey), map, callback);
    }

    public static void showFeedbackSentDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(context.getResources().getString(R.string.feedback_sent))
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }
}
