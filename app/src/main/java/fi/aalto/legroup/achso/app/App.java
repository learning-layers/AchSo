package fi.aalto.legroup.achso.app;

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
import fi.aalto.legroup.achso.authentication.AuthenticatedHttpClient;
import fi.aalto.legroup.achso.authentication.LoginManager;
import fi.aalto.legroup.achso.authentication.LoginRequestEvent;
import fi.aalto.legroup.achso.authoring.LocationManager;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.local.LocalVideoInfoRepository;
import fi.aalto.legroup.achso.storage.local.LocalVideoRepository;
import fi.aalto.legroup.achso.storage.remote.strategies.ClViTra2Strategy;
import fi.aalto.legroup.achso.storage.remote.strategies.SssStrategy;
import fi.aalto.legroup.achso.storage.remote.strategies.Strategy;

public class App extends MultiDexApplication {

    private static final String ACH_SO_LOCAL_STORAGE_NAME = "Ach so!";

    private static App singleton;

    public static Bus bus;

    public static ConnectivityManager connectivityManager;

    public static LoginManager loginManager;
    public static OkHttpClient httpClient;
    public static AuthenticatedHttpClient authenticatedHttpClient;
    public static LocationManager locationManager;

    public static JsonSerializer jsonSerializer;

    public static LocalVideoInfoRepository videoInfoRepository;
    public static LocalVideoRepository videoRepository;

    public static File localStorageDirectory;

    public static Strategy videoStrategy;
    public static Strategy metadataStrategy;

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

        videoStrategy = new ClViTra2Strategy(bus, getString(R.string.clvitra2Url));
        metadataStrategy = new SssStrategy(bus, Uri.parse(getString(R.string.sssUrl)));

        // TODO: The instantiation of repositories should be abstracted further.
        // That would allow for multiple repositories.
        File mediaDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        localStorageDirectory = new File(mediaDirectory, ACH_SO_LOCAL_STORAGE_NAME);

        if (!(localStorageDirectory.isDirectory() || localStorageDirectory.mkdirs())) {
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
        }

        jsonSerializer = new JsonSerializer();

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
