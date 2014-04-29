/**
 * Copyright 2013 Aalto university, see AUTHORS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.activity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.LocalRawVideos;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.state.IntentDataHolder;
import fi.aalto.legroup.achso.state.LoginState;
import fi.aalto.legroup.achso.util.App;
import fi.google.zxing.integration.android.IntentIntegrator;

import static fi.aalto.legroup.achso.util.App.appendLog;

/**
 * This activity is used to present the same actionbar buttons for every activity.
 * Extend this class instead of FragmentActivity to inherit the same actionbar for your activity.
 * This is never used by itself and so it doesn't have onCreate or intent filters defined.
 */

public abstract class ActionbarActivity extends FragmentActivity {

    public static final int REQUEST_VIDEO_CAPTURE = 1;
    public static final int REQUEST_SEMANTIC_VIDEO_GENRE = 2;
    public static final int REQUEST_LOCATION_SERVICES = 3;
    public static final int REQUEST_QR_CODE_READ = 4;
    public static final int REQUEST_QR_CODE_FOR_EXISTING_VIDEO = 5;
    public static final int REQUEST_LOGIN = 7;
    public static final int API_VERSION = android.os.Build.VERSION.SDK_INT;
    private static final int REQUEST_VIDEO_FILE = 8;

    protected Menu mMenu;
    private Uri mVideoUri;
    protected IntentFilter mFilter;
    protected AchSoBroadcastReceiver mReceiver;
    protected IntentFilter mLocalFilter;
    protected AchSoLocalBroadcastReceiver mLocalReceiver;

    protected boolean show_record() {return true;}
    protected boolean show_login() {return true;}
    protected boolean show_qr() {return true;}
    protected boolean show_search() {return true;}

    /**
     * These are the top row menu items -- all of the main activities inherit the same menu item group, though some may not be visible for all.
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /**
         */
        switch (item.getItemId()) {
            case R.id.action_newvideo:
                launchRecording();
                return true;
            case R.id.action_readqrcode:
                launchQrReading();
                return true;
            case R.id.action_login:
                startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_LOGIN);
                return true;
            case R.id.action_logout:
                App.login_state.logout();
                invalidateOptionsMenu();
                return true;
            case R.id.action_addvideo:
                launchFileSelect();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchFileSelect() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/mp4");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        try {
            startActivityForResult(intent, REQUEST_VIDEO_FILE);
        } catch (ActivityNotFoundException e) {
            Log.e("tag", "No activity can handle picking a file. Showing alternatives.");
        }
    }


    /**
     *
     */

    protected void initMenu(Menu menu) {
        mMenu = menu;
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.main_menubar, menu);
        if (show_login()) {
            updateLoginMenuItem();
        } else {
            menu.removeItem(R.id.action_login);
            menu.removeItem(R.id.action_logout);
            menu.removeItem(R.id.action_offline);
            menu.removeItem(R.id.menu_refresh);
        }
        if (!show_qr()) {
            menu.removeItem(R.id.action_readqrcode);
        }
        if (!show_search()) {
            menu.removeItem(R.id.action_search);
        }
        if (!show_record()) {
            menu.removeItem(R.id.action_newvideo);
        }
    }

    /**
     * Each subclass has its own onCreateOptionsMenu that limits what is displayed in menu bar.
     * Most of them have login/logout/offline -area visible and this method makes sure it is set up correctly.
     *
     * @param menu
     * @return
     */

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mMenu = menu;
        if (show_login()) {
            updateLoginMenuItem();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Call this manually in resume/prepare methods to update correct mode to top right corner login buttons
     */
    private void updateLoginMenuItem() {
        if (mMenu == null || !show_login()) {
            //Log.i("ActionBarActivity", "Skipping icon update -- menu is null.");
            return;
        }
        MenuItem loginItem = mMenu.findItem(R.id.action_login);
        MenuItem logoutItem = mMenu.findItem(R.id.action_logout);
        MenuItem loadingItem = mMenu.findItem(R.id.menu_refresh);
        MenuItem offlineItem = mMenu.findItem(R.id.action_offline);

        if (loginItem == null || logoutItem == null || loadingItem == null || offlineItem == null) {
            Log.i("ActionBarActivity", "Skipping icon update -- they are not present. ");
        } else {
            if (!App.hasConnection()) {
                loginItem.setVisible(false);
                logoutItem.setVisible(false);
                loadingItem.setVisible(false);
                offlineItem.setVisible(true);
            } else if (App.login_state.isIn()) {
                loginItem.setVisible(false);
                logoutItem.setVisible(true);
                loadingItem.setVisible(false);
                offlineItem.setVisible(false);
            } else if (App.login_state.isOut()) {
                loginItem.setVisible(true);
                logoutItem.setVisible(false);
                loadingItem.setVisible(false);
                offlineItem.setVisible(false);
            } else if (App.login_state.isTrying()) {
                loginItem.setVisible(false);
                logoutItem.setVisible(false);
                loadingItem.setVisible(true);
                offlineItem.setVisible(false);
            }
        }
    }

    /**
     * A new recording can be started in most activities.
     */
    public void launchRecording() {
        File output_file = LocalRawVideos.getNewOutputFile();
        if (output_file != null) {
            App.getLocation();
            Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            SharedPreferences.Editor e = getSharedPreferences("AchSoPrefs", 0).edit();
            e.putString("videoUri", output_file.getAbsolutePath());
            e.commit();

            // Some Samsung devices seem to have serious problems with using MediaStore.EXTRA_OUTPUT.
            // The only way around it is that hack in AnViAnno, to let ACTION_VIDEO_CAPTURE to record where it wants by default and
            // then get the file and write it to correct place.

            // In this solution the problem is that some devices return null from ACTION_VIDEO_CAPTURE intent
            // where they should return the path. This is reported Android 4.3.1 bug. So let them try the MediaStore.EXTRA_OUTPUT-way


            if (API_VERSION >= 18) {
                mVideoUri = Uri.fromFile(output_file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mVideoUri); //mVideoUri.toString()); // Set output location
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
            appendLog("Starting recording.");
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);

        } else {
            new AlertDialog.Builder(this).setTitle(getApplicationContext().getResources().getString(R.string.storage_error)).setMessage(getApplicationContext().getResources().getString(R.string.detailed_storage_error)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            }).create().show();
        }
    }

    /**
     * Qr-reading can also be started from many activities.
     */
    public void launchQrReading() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        IntentDataHolder.From = ActionbarActivity.class;
        // propose the free version first, by default IntentIntegrator would propose paid one
        List<String> target_applications = Arrays.asList("com.google.zxing.client.android",  // Barcode Scanner
                "com.srowen.bs.android.simple", // Barcode Scanner+ Simple
                "com.srowen.bs.android"             // Barcode Scanner+
        );
        integrator.setTargetApplications(target_applications);
        integrator.initiateScan(IntentIntegrator.ALL_CODE_TYPES);
        appendLog("Launched Qr Reading.");
    }

    private AlertDialog getLocationNotEnabledDialog() {
        return new AlertDialog.Builder(this).setTitle(R.string.location_not_enabled)
                .setMessage(R.string.location_not_enabled_text)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                                REQUEST_LOCATION_SERVICES);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
    }


    protected long createSemanticVideo(Uri video_uri) {
        VideoDBHelper vdb = new VideoDBHelper(this);
        int count = vdb.getNumberOfVideosToday();
        String dayname = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(new Date());
        String creator = null;
        if (App.login_state.isIn()) {
            creator = App.login_state.getUser();
        }
        String vid_name = ordinal(count + 1) + " video of " + dayname;
        SemanticVideo newvideo = new SemanticVideo(vid_name, video_uri,
                SemanticVideo.Genre.values()[0], creator);
        vdb.insert(newvideo);
        vdb.close();
        appendLog(String.format("Created video %s to uri %s", vid_name, video_uri.toString()));
        return newvideo.getId();
    }

    /**
     * Re-implement this in subclasses to recognize more or less broadcasts -- these are the
     * defaults to have login button to change state according to login process and network
     * availability
     */
    protected void startReceivingBroadcasts() {

        // Start receiving system / inter app broadcasts
        if (mFilter == null || mReceiver == null) {
            mFilter = new IntentFilter();
            mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mReceiver = new AchSoBroadcastReceiver();
        }
        this.registerReceiver(mReceiver, mFilter);
        // Start receiving local broadcasts
        if (mLocalFilter == null || mLocalReceiver == null) {
            mLocalFilter = new IntentFilter();
            mLocalFilter.addAction(LoginState.LOGIN_SUCCESS);
            mLocalFilter.addAction(LoginState.LOGIN_FAILED);
            mLocalReceiver = new AchSoLocalBroadcastReceiver();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, mLocalFilter);
    }
    protected void stopReceivingBroadcasts(){
        this.unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopReceivingBroadcasts();

    }

    @Override
    public void onResume() {
        super.onResume();
        startReceivingBroadcasts();
    }


    /**
         * Handle responses from launched activities
         *
         * @param requestCode
         * @param resultCode
         * @param intent
         */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_VIDEO_FILE:
                if (resultCode == RESULT_OK) {
                    //String videoPath = intent.getData();
                    //Log.i("ActionbarActivity", "Received data: "+ intent.getData());
                    //Log.i("ActionbarActivity", "Received path "+ videoPath);
                    mVideoUri = intent.getData();
                    Log.i("ActionbarActivity", "mVideoUri "+ mVideoUri);
                    String received_path = LocalRawVideos.getRealPathFromURI(this, mVideoUri);
                    Log.i("ActionbarActivity", "Received path "+ received_path);
                    mVideoUri = Uri.parse(received_path);
                    long video_id = createSemanticVideo(mVideoUri);
                    Intent i = new Intent(this, GenreSelectionActivity.class);
                    i.putExtra("videoId", video_id);
                    startActivityForResult(i, REQUEST_SEMANTIC_VIDEO_GENRE);
                } else if (resultCode == RESULT_CANCELED) {
                    Log.d("CANCEL", "Video add canceled");
                } else {
                    Log.i("ActionBarActivity", "Video add failed.");
                }
                break;
            case REQUEST_VIDEO_CAPTURE:
                //Log.d("ActionBarActivity", "REQUEST_VIDEO_CAPTURE returned " + resultCode);

                if (resultCode == RESULT_OK) {
                    // Got video from camera. Starting genre selection activity
                    finishActivity(REQUEST_VIDEO_CAPTURE);
                    appendLog("Finished recording.");
                    String videoPath = getSharedPreferences("AchSoPrefs", 0).getString("videoUri", null);
                    //Log.d("ActionBarActivity", "AchSoPrefs says that videoUri is " + videoPath);

                    if (API_VERSION < 18) {
                        // Version 3. find out the real path and do rename.
                        // This works, but not widely tested.
                        mVideoUri = Uri.parse(videoPath);

                        Uri saved_to = intent.getData();
                        String received_path;
                        File source;
                        if (saved_to == null) {
                            received_path = LocalRawVideos.getLatestVideo(this);
                            source = new File(received_path);
                            int tries = 0;
                            while (!source.isFile() && tries < 100) {
                                tries++;
                                received_path = LocalRawVideos.getLatestVideo(this);
                                source = new File(received_path);
                                Log.i("ActionBarAction", "File was not ready yet, trying again: " + tries);
                            }

                        } else {
                            received_path = LocalRawVideos.getRealPathFromURI(this, saved_to);
                            Log.i("ActionBarAction", "Found real path: " + received_path);
                            source = new File(received_path);
                        }
                        Log.i("ActionBarAction", "Is it file: " + source.isFile());
                        File target = new File(videoPath);
                        source.renameTo(target);
                        mVideoUri = Uri.fromFile(target);


                        // Version 2. write file data
                        // Avoiding EXTRA_OUTPUT: intent saves to its own place, we need to get the file and write it to proper place
                        // This proved to be too slow for >100mB videos.

                    } else {
                        // Version 1: intent wrote file to correct place at once.
                        if (intent == null && videoPath.isEmpty()) {
                            Toast.makeText(this, "Failed to save video.", Toast.LENGTH_LONG).show();
                            super.onBackPressed();
                        } else if (intent != null && !intent.getDataString().isEmpty()) {
                            Log.d("ActionBarActivity", "Found better from intent: " + intent.getData());
                            mVideoUri = intent.getData();
                        } else {
                            mVideoUri = Uri.parse(videoPath);
                            getSharedPreferences("AchSoPrefs", 0).edit().putString("videoUri", null).commit();
                        }
                    }

                    // Verify that the file exists
                    //File does_it = new File(mVideoUri.getPath());
                    //Log.i("ActionBarActivity", "Saved file at " + mVideoUri.getPath() + " " +
                    //        "exists: " + does_it.exists());

                    //Toast.makeText(this, "Video saved to: " + mVideoUri, Toast.LENGTH_LONG).show();
                    long video_id = createSemanticVideo(mVideoUri);

                    Intent i = new Intent(this, GenreSelectionActivity.class);
                    i.putExtra("videoId", video_id);
                    startActivityForResult(i, REQUEST_SEMANTIC_VIDEO_GENRE);
                } else if (resultCode == RESULT_CANCELED) {
                    Log.d("CANCEL", "Camera canceled");
                } else {
                    Log.i("ActionBarActivity", "Video capture failed.");
                }
                break;
            case REQUEST_LOCATION_SERVICES:
                final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
                    getLocationNotEnabledDialog().show();
                }
                break;
            case REQUEST_LOGIN:
                invalidateOptionsMenu();
                Toast.makeText(this, "Login successful", Toast.LENGTH_LONG).show();
                break;

        }
    }


    private static String ordinal(int i) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];

        }
    }

    /**
     * Receive changes in login state, other activities may add more intents that they recognize.
     */
    public class AchSoBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                updateLoginMenuItem();
            }
        }
    }
    /**
     * Receive changes in login state, other activities may add more intents that they recognize.
     */
    public class AchSoLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(LoginState.LOGIN_SUCCESS)) {
                    updateLoginMenuItem();
                } else if (action.equals(LoginState.LOGIN_FAILED)) {
                    updateLoginMenuItem();
                }
            }
        }
    }


}
