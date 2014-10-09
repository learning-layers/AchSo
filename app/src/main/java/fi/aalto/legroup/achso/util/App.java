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
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.squareup.okhttp.OkHttpClient;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.networking.AuthenticatedHttpClient;
import fi.aalto.legroup.achso.state.LoginManager;
import fi.aalto.legroup.achso.upload.DisabledUploader;
import fi.aalto.legroup.achso.upload.DummyUploader;
import fi.aalto.legroup.achso.upload.Uploader;
import fi.aalto.legroup.achso.upload.video.ClViTra2VideoUploader;

public class App extends Application {

    private static App singleton;
    
    public static ConnectivityManager connectivityManager;

    public static final int BROWSE_BY_QR = 0;
    public static final int ATTACH_QR = 1;
    public static final int API_VERSION = android.os.Build.VERSION.SDK_INT;
    public static LoginManager loginManager;
    public static OkHttpClient httpClient;
    public static AuthenticatedHttpClient authenticatedHttpClient;
    public static Connection connection;
    public static LocationManager locationManager;

    private static int qr_mode;

    public static final String ACHSO_ACCOUNT_TYPE = "fi.aalto.legroup.achso.ll_oidc";

    public static Uploader videoUploader;
    public static Uploader metadataUploader;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        httpClient = new OkHttpClient();
        authenticatedHttpClient = new AuthenticatedHttpClient(this, httpClient);

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        loginManager = new LoginManager(this);

        connection = new AaltoConnection();

        locationManager = new LocationManager(this);

        videoUploader = new DisabledUploader();
        metadataUploader = new DummyUploader();
    }

    public static boolean isTablet() {
        return (singleton.getResources().getConfiguration().screenLayout & Configuration
                .SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isHorizontalCandybar() {
        Configuration c = singleton.getResources().getConfiguration();
        int size = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return (c.orientation == Configuration.ORIENTATION_LANDSCAPE && (size == Configuration
                .SCREENLAYOUT_SIZE_SMALL || size == Configuration.SCREENLAYOUT_SIZE_NORMAL));
    }

    public static boolean isCandybar() {
        Configuration c = singleton.getResources().getConfiguration();
        int size = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return (size == Configuration.SCREENLAYOUT_SIZE_SMALL || size == Configuration.SCREENLAYOUT_SIZE_NORMAL);
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
        return singleton.getApplicationContext();
    }

}


