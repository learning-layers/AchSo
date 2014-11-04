package fi.aalto.legroup.achso.menu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.LocalRawVideos;
import fi.aalto.legroup.achso.util.App;

/**
 * Created by lassi on 3.11.14.
 */
public class NewVideoHelper {

    public static final int ACTIVITY_NUMBER = 1;

    public static void result(Activity activity, int resultCode, Intent data) {
        activity.finishActivity(ACTIVITY_NUMBER);
        String videoPath = activity.getSharedPreferences("AchSoPrefs", 0).getString("videoUri", null);


    }

    public static void record(Activity activity) {
        File output_file = LocalRawVideos.getNewOutputFile();
        if (output_file != null) {
            App.locationManager.startLocationUpdates();
            Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            SharedPreferences.Editor e = activity.getSharedPreferences("AchSoPrefs", 0).edit();
            e.putString("videoUri", output_file.getAbsolutePath());
            e.commit();

            // Some Samsung devices seem to have serious problems with using MediaStore.EXTRA_OUTPUT.
            // The only way around it is that hack in AnViAnno, to let ACTION_VIDEO_CAPTURE to record where it wants by default and
            // then get the file and write it to correct place.

            // In this solution the problem is that some devices return null from ACTION_VIDEO_CAPTURE intent
            // where they should return the path. This is reported Android 4.3.1 bug. So let them try the MediaStore.EXTRA_OUTPUT-way


            if (App.API_VERSION >= 18) {
                //mVideoUri = Uri.fromFile(output_file);
                //intent.putExtra(MediaStore.EXTRA_OUTPUT, mVideoUri); //mVideoUri.toString()); // Set output location
            }


            // Old code goes here:
            /*
            mVideoUri = Uri.fromFile(output_file);
            Log.i("ActionBarActivity", "Storing video to " + mVideoUri); //mVideoUri.toString());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mVideoUri); //mVideoUri.toString()); // Set output location
            Log.i("ActionBarActivity", "Prefs abspath is " + output_file.getAbsolutePath());
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // High video quality
            appendLog("Starting recording.");
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
            */

            // Intent without EXTRA_OUTPUT
            //Log.i("ActionBarActivity", "Storing video through prefs: " + output_file.getAbsolutePath());
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // High video quality
            activity.startActivityForResult(intent, ACTIVITY_NUMBER);
        } else {
            new AlertDialog.Builder(activity).setTitle(activity.getApplicationContext().getResources().getString(R.string.storage_error)).setMessage(activity.getApplicationContext().getResources().getString(R.string.detailed_storage_error)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            }).create().show();
        }
    }
}
