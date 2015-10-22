package fi.aalto.legroup.achso.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.rollbar.android.Rollbar;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.Bus;

import java.io.File;
import java.security.GeneralSecurityException;

import fi.aalto.legroup.achso.BuildConfig;
import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.authentication.AuthenticatedHttpClient;
import fi.aalto.legroup.achso.authentication.LoginManager;
import fi.aalto.legroup.achso.authentication.LoginRequestEvent;
import fi.aalto.legroup.achso.authoring.LocationManager;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.CombinedVideoRepository;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;
import fi.aalto.legroup.achso.storage.VideoRepository;
import fi.aalto.legroup.achso.storage.remote.SyncService;
import fi.aalto.legroup.achso.storage.remote.UploadService;
import fi.aalto.legroup.achso.storage.remote.strategies.AchRailsStrategy;
import fi.aalto.legroup.achso.storage.remote.strategies.ClViTra2Strategy;
import fi.aalto.legroup.achso.storage.remote.strategies.DumbPhpStrategy;
import fi.legroup.aalto.cryptohelper.CryptoHelper;

public final class App extends MultiDexApplication
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ACH_SO_LOCAL_STORAGE_NAME = "Ach so!";

    public static Bus bus;

    public static ConnectivityManager connectivityManager;

    public static LoginManager loginManager;
    public static OkHttpClient httpClient;
    public static AuthenticatedHttpClient authenticatedHttpClient;
    public static LocationManager locationManager;

    public static JsonSerializer jsonSerializer;

    public static VideoRepository videoRepository;
    public static VideoInfoRepository videoInfoRepository;

    public static File localStorageDirectory;

    private static Uri layersBoxUrl;

    @Override
    public void onCreate() {
        super.onCreate();

        setupErrorReporting();

        setupPreferences();

        layersBoxUrl = readLayersBoxUrl();

        bus = new AppBus();

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        httpClient = new OkHttpClient();
        authenticatedHttpClient = new AuthenticatedHttpClient(this, httpClient);

        loginManager = new LoginManager(this, bus);

        locationManager = new LocationManager(this);

        jsonSerializer = new JsonSerializer();

        setupUploaders();

        // TODO: The instantiation of repositories should be abstracted further.
        // That would allow for multiple repositories.
        File mediaDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        localStorageDirectory = new File(mediaDirectory, ACH_SO_LOCAL_STORAGE_NAME);

        if (!(localStorageDirectory.isDirectory() || localStorageDirectory.mkdirs())) {
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
        }

        File cacheVideoDirectory = new File(localStorageDirectory, "cache");
        cacheVideoDirectory.mkdirs();

        CombinedVideoRepository combinedRepository = new CombinedVideoRepository(bus, jsonSerializer,
                localStorageDirectory, cacheVideoDirectory);

        //combinedRepository.addHost(ownCloudStrategy);
        combinedRepository.addHost(new AchRailsStrategy(jsonSerializer, Uri.parse(getString(R.string.achRailsUrl))));

        videoRepository = combinedRepository;
        videoInfoRepository = combinedRepository;

        videoRepository.refreshOffline();

        SyncService.syncWithCloudStorage(this);

        bus.post(new LoginRequestEvent(LoginRequestEvent.Type.LOGIN));

        // Trim the caches asynchronously
        AppCache.trim(this);

        // Setup Google Analytics
        AppAnalytics.setup(this);
    }

    public static Uri getLayersBoxUrl() {
        return layersBoxUrl;
    }

    public static Uri getLayersServiceUrl(String serviceUriString) {
        return getLayersServiceUrl(Uri.parse(serviceUriString));
    }

    public static Uri getLayersServiceUrl(Uri serviceUri) {
        return resolveRelativeUri(serviceUri, layersBoxUrl);
    }

    /**
     * If the given URI is relative, resolves it into an absolute one against the given root. If
     * the URI is already absolute, it will be returned as-is.
     *
     * @param uri     Relative or absolute URI.
     * @param rootUri Absolute URI to use as the root in case the given URI is relative.
     * @return An absolute URI.
     */
    private static Uri resolveRelativeUri(Uri uri, Uri rootUri) {
        if (uri.isAbsolute()) {
            return uri;
        } else {
            // Remove a leading slash if there is one, otherwise it'll be duplicated
            String path = uri.toString().replaceFirst("^/", "");

            return rootUri.buildUpon().appendEncodedPath(path).build();
        }
    }

    public static boolean isConnected() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isDisconnected() {
        return !isConnected();
    }

    private void setupPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void setupErrorReporting() {
        String releaseStage;

        if (BuildConfig.DEBUG) {
            releaseStage = "debug";
        } else {
            releaseStage = "production";
        }

        Rollbar.init(this, getString(R.string.rollbarApiKey), releaseStage);
    }

    private void setupUploaders() {

        // Temporary uploader until ClViTra2 is fixed in the Layers Box
        // TODO: Remove this
        String achsoStorageUrlString = getString(R.string.achsoStorageUrl);
        if (!Strings.isNullOrEmpty(achsoStorageUrlString)) {
            UploadService.addUploader(new DumbPhpStrategy(Uri.parse(achsoStorageUrlString)));
        }

        Uri clViTra2Url = Uri.parse(getString(R.string.clvitra2Url));
        Uri sssUrl = Uri.parse(getString(R.string.sssUrl));

        ClViTra2Strategy videoStrategy = new ClViTra2Strategy(clViTra2Url);

        UploadService.addUploader(videoStrategy);
    }

    private Uri readLayersBoxUrl() {

        String[] searchPackages = {
                "com.raycom.ltb",
                "fi.legroup.aalto.achsoexampleconnection",
        };

        for (String packageName : searchPackages) {
            try {
                Context context = createPackageContext(packageName, 0);
                SharedPreferences ltbPreferences = context.getSharedPreferences(
                        "eu.learning-layers.LAYERS_BOX", Context.MODE_PRIVATE);

                String encryptedUrlString = ltbPreferences.getString("LAYERS_BOX_URL", null);
                if (encryptedUrlString != null) {
                    String secret = getString(R.string.ltbSecret);
                    String urlString = CryptoHelper.decrypt(encryptedUrlString, secret);
                    return Uri.parse(urlString);
                }
            } catch (PackageManager.NameNotFoundException | GeneralSecurityException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String defaultUrlString = getString(R.string.layersBoxUrl);
        String urlString = preferences.getString(AppPreferences.LAYERS_BOX_URL, defaultUrlString);

        return Uri.parse(urlString);
    }

    /**
     * Listens for changes to the shared preferences.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        switch (key) {
            // Listen for changes to the Layers box URL preference and update the internal field.
            case AppPreferences.LAYERS_BOX_URL:
                String defaultUrlString = getString(R.string.layersBoxUrl);
                String urlString = preferences.getString(key, defaultUrlString);

                layersBoxUrl = Uri.parse(urlString);
                break;

            // Listen for changes to the analytics opt in preference.
            case AppPreferences.ANALYTICS_OPT_IN:
                boolean hasOptedIn = preferences.getBoolean(key, false);
                GoogleAnalytics.getInstance(this).setAppOptOut(!hasOptedIn);
                break;
        }
    }

}
