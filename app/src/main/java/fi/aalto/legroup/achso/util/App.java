/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
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

package fi.aalto.legroup.achso.util;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import fi.aalto.legroup.achso.state.LoginManager;

public class App extends Application {
    
    public static ConnectivityManager connectivityManager;

    public static final int BROWSE_BY_QR = 0;
    public static final int ATTACH_QR = 1;
    public static final int API_VERSION = android.os.Build.VERSION.SDK_INT;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static LoginManager loginManager;
    public static Connection connection;
    private static Context mContext;
    private static File mLogFile;
    private static int qr_mode;

    private static Location lastLocation;
    private static LocationListener locationListener;
    private static GoogleApiClient locationApiClient;

    //
    private static final int OIDC_AUTHENTICATION = 5;
    private static final int LASCONNECTION = 4;
    public static final int AALTO_TEST_SERVER = 3;
    public static final int CLVITRA2 = 2;
    public static final int CLVITRA = 1;
    public static final int DEV_NULL = 0;


    public static final String DEFAULT_USERNAME = "achso_device_owner";
    public static final String UPDATE_ANNOTATION = "update_annotation";
    public static final String ADD_ANNOTATION = "add_annotation";
    public static final String REMOVE_ANNOTATION = "remove_annotation";
    public static final String UPDATE_VIDEO= "update_video";
    public static final String FINALIZE_VIDEO= "finalize_video";
    public static final String PENDING_POLLS = "aalto.legroup.achso.PENDING_POLLS";

    public static final String ACHSO_ACCOUNT_TYPE = "fi.aalto.legroup.achso.ll_oidc";


    public static boolean use_las = false;
    private static boolean use_log_file = false;
    public static boolean allow_upload = false;

    public static int login_provider = OIDC_AUTHENTICATION;
    public static int video_uploader = CLVITRA2;
    public static int metadata_uploader = AALTO_TEST_SERVER;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        loginManager = new LoginManager(this);

        if (use_log_file) {
            mLogFile = new File(mContext.getExternalFilesDir(null), "achso.log");
            if (!mLogFile.exists()) {
                try {
                    boolean ok = mLogFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        if (use_las) {
            connection = new LasConnection();
        } else {
            connection = new AaltoConnection();
        }
        if (isConnected()) {
            doPendingPolls();
        }

        appendLog("Starting Ach so! -app on device " + android.os.Build.MODEL);
        Log.i("App", "Starting Ach so! -app on device " + android.os.Build.MODEL);

    }

    public static void appendLog(String text) {
        if (use_log_file) {
            try {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(mLogFile, true));
                buf.append(String.format("%s %s", sdf.format(new Date()), text));
                buf.newLine();
                buf.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static boolean isTablet() {
        return (mContext.getResources().getConfiguration().screenLayout & Configuration
                .SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static int getScreenSize() {
        return mContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
    }

    public static int getScreenOrientation() {
        return mContext.getResources().getConfiguration().orientation;
    }

    public static boolean isHorizontalCandybar() {
        Configuration c = mContext.getResources().getConfiguration();
        int size = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return (c.orientation == Configuration.ORIENTATION_LANDSCAPE && (size == Configuration
                .SCREENLAYOUT_SIZE_SMALL || size == Configuration.SCREENLAYOUT_SIZE_NORMAL));
    }

    public static boolean isVerticalCandybar() {
        Configuration c = mContext.getResources().getConfiguration();
        int size = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return (c.orientation == Configuration.ORIENTATION_PORTRAIT && (size == Configuration
                .SCREENLAYOUT_SIZE_SMALL || size == Configuration.SCREENLAYOUT_SIZE_NORMAL));
    }

    public static boolean isCandybar() {
        Configuration c = mContext.getResources().getConfiguration();
        int size = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return (size == Configuration.SCREENLAYOUT_SIZE_SMALL || size == Configuration.SCREENLAYOUT_SIZE_NORMAL);
    }


    public static void addPollingReminder(String key,
                                          String user_id) {
        SharedPreferences pending = mContext.getSharedPreferences(PENDING_POLLS,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pending.edit();
        editor.putString(key, user_id);
        editor.commit();
    }

    public static void removePollingReminder(String key) {
        SharedPreferences pending = mContext.getSharedPreferences(PENDING_POLLS,
                Context.MODE_PRIVATE);
        if (pending.getString(key, null) != null) {
            Log.i("App", "Removing polling pref. " + key);
            SharedPreferences.Editor editor = pending.edit();
            editor.putString(key, null);
            editor.commit();
        }
    }


    public static void doPendingPolls() {
        SharedPreferences pending = mContext.getSharedPreferences(PENDING_POLLS,
                Context.MODE_PRIVATE);
        Map<String, ?> items = pending.getAll();
        String user_id;
        for (String key: items.keySet()) {
            user_id = (String) items.get(key);
            removePollingReminder(key);
            //Intent pollingIntent = new Intent(App.getContext(), PollingService.class);
            //pollingIntent.putExtra(PollingService.VIDEO_KEY, key);
            //pollingIntent.putExtra(PollingService.USERID_PART, user_id);
            //App.getContext().startService(pollingIntent);
        }
    }



    /**
     * Location updates should be requested when recording starts. This should give us enough time
     * to fetch an accurate location.
     *
     * TODO: Move location stuff into its own class.
     */
    public static void startRequestingLocationUpdates() {
        // NOTE: There's a small possibility that the location could not be retrieved before the
        // video recording is finished. Is this acceptable or is there a need for a waiting dialog?

        locationApiClient = new GoogleApiClient.Builder(mContext)
                .useDefaultAccount()
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        LocationRequest locationRequest = LocationRequest.create()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                        locationListener = new LocationListener() {
                            public void onLocationChanged(Location location) {
                                lastLocation = location;
                            }
                        };

                        LocationServices.FusedLocationApi.requestLocationUpdates(locationApiClient,
                                locationRequest, locationListener);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {}
                })
                .build();

        locationApiClient.connect();
    }

    /**
     * When recording has finished, ask for the location via this method, so we'll stop listening
     * for further location updates.
     */
    public static Location getLastLocation() {
        if (locationApiClient != null && locationListener != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(locationApiClient,
                    locationListener);
        }

        return lastLocation;
    }

    public static void setQrMode(int mode) {
        qr_mode = mode;
    }

    public static int getQrMode() {
        return qr_mode;
    }

    public static boolean isConnected() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isDisconnected() {
        return ! isConnected();
    }

    public static Context getContext() {
        return mContext.getApplicationContext();
    }

}


