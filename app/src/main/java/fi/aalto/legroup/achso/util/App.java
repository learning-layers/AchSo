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
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import fi.aalto.legroup.achso.state.LoginState;
import fi.aalto.legroup.achso.state.OIDCLoginState;

public class App extends Application {

    public static final int BROWSE_BY_QR = 0;
    public static final int ATTACH_QR = 1;
    public static final int API_VERSION = android.os.Build.VERSION.SDK_INT;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static LoginState login_state;
    public static Connection connection;
    private static Context mContext;
    private static File mLogFile;
    public static Location last_location;
    public static OIDCConfig oidc_config;
    private static int qr_mode;

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
        switch (login_provider) {
            case OIDC_AUTHENTICATION:
                oidc_config = new OIDCConfig("OIDCsettings");
                login_state = new OIDCLoginState(mContext);
                break;
        }

        if (use_las) {
            connection = new LasConnection();
        } else {
            connection = new AaltoConnection();
        }
        if (hasConnection()) {
            doPendingPolls();
        }

        App.login_state.autologinIfAllowed();

        appendLog("Starting Ach so! -app on device " + android.os.Build.MODEL);
        Log.i("App", "Starting Ach so! -app on device " + android.os.Build.MODEL);

    }


    public static Context getContext() {
        return mContext;
    }

    public static boolean hasConnection() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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
     * Location should be asked when starting the app and when starting recording. It can be
     * battery-consuming operation, so don't do it too often.
     */
    public static void getLocation() {
        // NOTE: There's a small possibility that the location could not be retrieved before the
        // video recording is finished. Is this acceptable or is there a need for a waiting dialog?

        final LocationManager locationManager = (LocationManager) mContext.getSystemService(Context
                .LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                last_location = location;
                Log.i("LocationListener", "Found location: " + location.toString());
                //Toast.makeText(mContext, "Found location: " + location.toString(),
                //        Toast.LENGTH_LONG).show();
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

    }



    public static String getUsername() {
        if (login_state != null) {
            if (login_state.isIn() || login_state.isTrying())
                return login_state.getUser();
        }
        return DEFAULT_USERNAME;
    }

    public static void setQrMode(int mode) {
        qr_mode = mode;
    }

    public static int getQrMode() {
        return qr_mode;
    }
}


