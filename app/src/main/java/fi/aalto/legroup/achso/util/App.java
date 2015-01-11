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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.StringRes;
import android.support.multidex.MultiDexApplication;
import android.widget.Toast;

import com.bugsnag.android.Bugsnag;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.Bus;

import java.io.File;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.networking.AuthenticatedHttpClient;
import fi.aalto.legroup.achso.repositories.local.LocalVideoInfoRepository;
import fi.aalto.legroup.achso.repositories.local.LocalVideoRepository;
import fi.aalto.legroup.achso.serialization.json.JsonSerializerService;
import fi.aalto.legroup.achso.state.LoginManager;
import fi.aalto.legroup.achso.state.LoginRequestEvent;
import fi.aalto.legroup.achso.upload.Uploader;
import fi.aalto.legroup.achso.upload.metadata.SssMetadataUploader;
import fi.aalto.legroup.achso.upload.video.ClViTra2VideoUploader;

public class App extends MultiDexApplication {

    private static final String ACH_SO_LOCAL_STORAGE_NAME = "Ach so!";

    private static App singleton;

    public static Bus bus;

    public static ConnectivityManager connectivityManager;

    public static LoginManager loginManager;
    public static OkHttpClient httpClient;
    public static AuthenticatedHttpClient authenticatedHttpClient;
    public static LocationManager locationManager;

    public static JsonSerializerService jsonSerializer;

    public static LocalVideoInfoRepository videoInfoRepository;
    public static LocalVideoRepository videoRepository;

    public static File localStorageDirectory;

    public static Uploader videoUploader;
    public static Uploader metadataUploader;

    @Override
    public void onCreate() {
        super.onCreate();

        singleton = this;

        Bugsnag.register(this, getString(R.string.bugsnagApiKey));

        bus = new AndroidBus();

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        httpClient = new OkHttpClient();
        authenticatedHttpClient = new AuthenticatedHttpClient(this, httpClient);

        loginManager = new LoginManager(this, bus);

        locationManager = new LocationManager(this);

        videoUploader = new ClViTra2VideoUploader(bus, getString(R.string.clvitra2Url));
        metadataUploader = new SssMetadataUploader(bus, Uri.parse(getString(R.string.sssUrl)));

        // TODO: The instantiation of repositories should be abstracted further.
        // That would allow for multiple repositories.
        File mediaDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        localStorageDirectory = new File(mediaDirectory, ACH_SO_LOCAL_STORAGE_NAME);

        if (!(localStorageDirectory.isDirectory() || localStorageDirectory.mkdirs())) {
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
        }

        jsonSerializer = new JsonSerializerService();

        videoInfoRepository = new LocalVideoInfoRepository(bus, jsonSerializer,
                localStorageDirectory);

        videoRepository = new LocalVideoRepository(bus, jsonSerializer, localStorageDirectory);

        bus.post(new LoginRequestEvent(LoginRequestEvent.Type.LOGIN));
    }

    public static void showError(@StringRes int resId) {
        App.showError(getContext().getString(resId));
    }

    public static void showError(String error) {
        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
    }

    public static boolean isConnected() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isDisconnected() {
        return !isConnected();
    }

    public static Context getContext() {
        return singleton;
    }

}

