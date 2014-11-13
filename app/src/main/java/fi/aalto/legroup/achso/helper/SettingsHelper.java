package fi.aalto.legroup.achso.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.util.Log;

import fi.aalto.legroup.achso.R;

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
}
