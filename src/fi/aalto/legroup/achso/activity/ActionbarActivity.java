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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.LocalVideos;
import fi.aalto.legroup.achso.state.AppState;
import fi.aalto.legroup.achso.state.IntentDataHolder;
import fi.aalto.legroup.achso.util.App;
import fi.google.zxing.integration.android.IntentIntegrator;

import static fi.aalto.legroup.achso.util.App.appendLog;

/**
 * This activity is used to present the same actionbar buttons for every activity.
 * Extend this class instead of FragmentActivity to inherit the same actionbar for your activity.
 * This is never used by itself and so it doesn't have onCreate or intent filters defined.
 */

public class ActionbarActivity extends FragmentActivity {

    public static final int REQUEST_VIDEO_CAPTURE = 1;
    public static final int REQUEST_SEMANTIC_VIDEO_GENRE = 2;
    public static final int REQUEST_QR_CODE_READ = 4;
    public static final int REQUEST_QR_CODE_FOR_EXISTING_VIDEO = 5;
    public static final int REQUEST_LOGIN = 7;
    public static final int API_VERSION= android.os.Build.VERSION.SDK_INT;

    protected Menu mMenu;
    private Uri mVideoUri;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i("ActionBarActivity", "Inflating options menu - ActionBarActivity");
        mMenu = menu;
        App.login_state.setHostActivity(this);
        App.login_state.autologinIfAllowed();
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.main_menubar, menu);
        menu.removeItem(R.id.action_search);
        menu.removeItem(R.id.action_readqrcode);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.i("ActionBarActivity", "Preparing options menu - ActionBarActivity");
        mMenu = menu;
        updateLoginMenuItem();
        return super.onPrepareOptionsMenu(menu);
    }

    public void updateLoginMenuItem() {
        if (mMenu == null) {
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


    public void launchRecording() {
        File output_file = LocalVideos.getNewOutputFile();
        if (output_file != null) {
            // NOTE: There's a small possibility that the location could not be retrieved before the
            // video recording is finished. Is this acceptable or is there a need for a waiting dialog?

            final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // Define a listener that responds to location updates
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                    AppState.get().last_location = location;
                    //Log.i("LocationListener", "Found location: " + location.toString());
                    Toast.makeText(getBaseContext(), "Found location: " + location.toString(), Toast.LENGTH_LONG).show();
                    // take location only once
                    locationManager.removeUpdates(this);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }

            // Register the listener with the Location Manager to receive location updates
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
            new AlertDialog.Builder(this)
                    .setTitle(getApplicationContext().getResources().getString(R.string.storage_error))
                    .setMessage(getApplicationContext().getResources().getString(R.string.detailed_storage_error))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).create().show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
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
                            received_path = LocalVideos.getLatestVideo(this);
                            source = new File(received_path);
                            int tries = 0;
                            while (!source.isFile() && tries < 100) {
                                tries++;
                                received_path = LocalVideos.getLatestVideo(this);
                                source = new File(received_path);
                                Log.i("ActionBarAction", "File was not ready yet, trying again: " + tries);
                            }

                        } else {
                            received_path = LocalVideos.getRealPathFromURI(this, saved_to);
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
                    File does_it = new File(mVideoUri.getPath());
                    Log.i("ActionBarActivity","Saved file at "+mVideoUri.getPath()+ " exists: " + does_it.exists());

                    //Toast.makeText(this, "Video saved to: " + mVideoUri, Toast.LENGTH_LONG).show();
                    Intent i = new Intent(this, GenreSelectionActivity.class);
                    i.putExtra("videoUri", mVideoUri.getPath()); //mVideoUri.toString());
                    startActivityForResult(i, REQUEST_SEMANTIC_VIDEO_GENRE);
                } else if (resultCode == RESULT_CANCELED) {
                    Log.d("CANCEL", "Camera canceled");
                } else {
                    Log.i("ActionBarActivity", "Video capture failed.");
                }
                break;
            case REQUEST_SEMANTIC_VIDEO_GENRE:
                // Go back to video recording if we got back from genre selection.
                if (resultCode != RESULT_OK) launchRecording();
                break;
            default:
                Log.i("ActionBarActivity", "unknown requestCode");
                break;
        }
    }

    public void launchQrReading() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        IntentDataHolder.From = ActionbarActivity.class;
        // propose the free version first, by default IntentIntegrator would propose paid one
        List<String> target_applications = Arrays.asList(
                "com.google.zxing.client.android",  // Barcode Scanner
                "com.srowen.bs.android.simple", // Barcode Scanner+ Simple
                "com.srowen.bs.android"             // Barcode Scanner+
                );
        integrator.setTargetApplications(target_applications);
        integrator.initiateScan(IntentIntegrator.ALL_CODE_TYPES);
        appendLog("Launched Qr Reading.");
    }
}
