package fi.aalto.legroup.achso.browsing;

import android.accounts.Account;
import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.astuetz.PagerSlidingTabStrip;
import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ScrollDirectionListener;
import com.nispok.snackbar.Snackbar;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;


import com.google.gson.JsonObject;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.app.AppPreferences;
import fi.aalto.legroup.achso.authentication.LoginActivity;
import fi.aalto.legroup.achso.authentication.LoginErrorEvent;
import fi.aalto.legroup.achso.authentication.LoginRequestEvent;
import fi.aalto.legroup.achso.authentication.LoginStateEvent;
import fi.aalto.legroup.achso.authentication.OIDCConfig;
import fi.aalto.legroup.achso.authoring.QRHelper;
import fi.aalto.legroup.achso.authoring.VideoCreatorService;
import fi.aalto.legroup.achso.settings.SettingsActivity;
import fi.aalto.legroup.achso.sharing.SharingActivity;
import fi.aalto.legroup.achso.storage.VideoRepositoryUpdatedEvent;
import fi.aalto.legroup.achso.storage.remote.SyncRequiredEvent;
import fi.aalto.legroup.achso.storage.remote.SyncService;
import fi.aalto.legroup.achso.storage.remote.upload.UploadStateEvent;
import fi.aalto.legroup.achso.utilities.BaseActivity;
import fi.aalto.legroup.achso.utilities.ProgressDialogFragment;
import fi.aalto.legroup.achso.views.adapters.VideoTabAdapter;
import fi.aalto.legroup.achso.entities.User;

/**
 * Activity for browsing available videos.
 *
 * TODO: Extract video creation stuff into its own activity.
 * TODO: Move fine location permission checking to somewhere where it makes more sense.
 */
public final class BrowserActivity extends BaseActivity implements View.OnClickListener,
        ScrollDirectionListener, SwipeRefreshLayout.OnRefreshListener {

    private static final int REQUEST_RECORD_VIDEO = 1;
    private static final int REQUEST_CHOOSE_VIDEO = 2;

    private static final int ACH_SO_TAKE_VIDEO_PERM = 3;
    private static final int ACH_SO_LOG_IN_PERM = 4;
    private static final int ACH_SO_BROWSE_PERM = 5;

    private static final String STATE_VIDEO_BUILDER = "STATE_VIDEO_BUILDER";
    private static final String ARG_LAYERS_BOX_URL = "ARG_LAYERS_BOX_URL";

    private Bus bus;

    private FloatingActionButton fab;
    private VideoTabAdapter tabAdapter;
    private MenuItem searchItem;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ViewPager viewPager;

    private PendingRepositoryUpdateListener pendingListener = new PendingRepositoryUpdateListener();

    // Video that is "under construction". Sent to VideoCreatorService for processing after it has
    // been recorded.
    private VideoCreatorService.VideoBuilder videoBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Inject instead
        this.bus = App.bus;

        setContentView(R.layout.activity_browser);

        if (savedInstanceState != null) {
            videoBuilder = savedInstanceState.getParcelable(STATE_VIDEO_BUILDER);
        }

        // Try to parse the Layers Box URL from the intent
        Intent intent = getIntent();
        if (intent != null) {
            String intentLayersBoxUrlString = intent.getStringExtra(ARG_LAYERS_BOX_URL);
            if (intentLayersBoxUrlString != null) {
                Uri intentLayersBoxUrl = Uri.parse(intentLayersBoxUrlString);
                if (intentLayersBoxUrl != null && !intentLayersBoxUrl.equals(App.getLayersBoxUrl())) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    preferences.edit()
                            .putString(AppPreferences.LAYERS_BOX_URL, intentLayersBoxUrl.toString())
                            .putBoolean(AppPreferences.USE_PUBLIC_LAYERS_BOX, false)
                            .apply();

                    bus.post(new LoginRequestEvent(LoginRequestEvent.Type.EXPLICIT_LOGOUT));
                    OIDCConfig.setTokens(null, null);
                }
            }
        }

        this.tabAdapter = new VideoTabAdapter(this, getSupportFragmentManager());
        this.tabAdapter.notifyDataSetChanged();

        tabAdapter.setScrollDirectionListener(this);

        // Start listening for updated events even before resuming the activity (and resuming
        // expects pendingListener to be registered)
        bus.register(pendingListener);

        viewPager = (ViewPager) findViewById(R.id.pager);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.toolbar_tabs);

        viewPager.setAdapter(this.tabAdapter);
        tabs.setViewPager(viewPager);


        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(this);

        // Control the media volume instead of the ringer volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            }, ACH_SO_BROWSE_PERM);
        }
    }

    @Override
    protected void onDestroy() {

        // Activities are paused before destroyed, so onPause because registers pendingListener
        // it has to be removed so we don't leak memory (and listeners)
        bus.unregister(pendingListener);

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Switch to the active listener (Note: register before unregister so someone always gets
        // notified. Double notification is alright in this case, since the action is idempotent)
        bus.register(this);
        bus.unregister(pendingListener);

        // HACK: This is bad, but will be called after setting the preferences
        if (!OIDCConfig.isReady()) {
            App.setupUploaders(this);
            App.updateOIDCTokens(this);
        }

        // If we received a VideoRepositoryUpdatedEvent while paused handle it here
        if (pendingListener.hasRepositoryUpdated()) {
            this.tabAdapter.notifyDataSetChanged();
            pendingListener.clearRepositoryUpdated();
        }

        // Download and upload modified videos every time the user goes to the browsing activity.
        // This includes returning from detail and playback activities, so it should be enough.
        if (App.videoRepository.hasImportantSyncPending()) {
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(true);
                }
            });
        }
        SyncService.syncWithCloudStorage(this);
    }

    @Override
    protected void onPause() {

        // Switch to the deferred listener (Note: register before unregister so someone always gets
        // notified. Double notification is alright in this case, since the action is idempotent)
        bus.register(pendingListener);
        bus.unregister(this);

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(STATE_VIDEO_BUILDER, videoBuilder);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.activity_browser, menu);

        SearchManager manager = (SearchManager) this.getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        this.searchItem = menu.findItem(R.id.action_search);
        searchView.setSearchableInfo(manager.getSearchableInfo(this.getComponentName()));

        if (App.loginManager.isLoggedIn()) {
            menu.findItem(R.id.action_login).setVisible(false);
            menu.findItem(R.id.action_logout).setVisible(true);

            User user = App.loginManager.getUser();
            String infoTitle = user.getName();
            JsonObject userInfo = App.loginManager.getUserInfo();

            try {
                if (userInfo != null) {
                    String email = userInfo.get("email").getAsString();
                    menu.findItem(R.id.info_email).setVisible(true).setTitle(email);

                }
            } catch (Exception ex) {}

            String loggedInText = getResources().getString(R.string.logged_in_as_short, infoTitle);
            menu.findItem(R.id.info_loggedinas).setVisible(true).setTitle(loggedInText);
        } else {
            menu.findItem(R.id.action_login).setVisible(true);
            menu.findItem(R.id.action_logout).setVisible(false);
            menu.findItem(R.id.info_loggedinas).setVisible(false);
            menu.findItem(R.id.info_email).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_import_video:
                chooseVideo();
                return true;

            case R.id.action_read_qrcode:
                QRHelper.readQRCodeForSearching(this, this.searchItem);
                return true;

            case R.id.action_manage_groups:
                SharingActivity.openManageGroupsActivity(this);
                return true;

            case R.id.action_login:
                // TODO: This needs some event magic

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                    }, ACH_SO_LOG_IN_PERM);
                } else {
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                }
                return true;

            case R.id.action_logout:
                // Go back to the 'All videos' tab since we're logging out!
                // TODO: Disable user tapping the tabs after log out has not completed fully!
                viewPager.setCurrentItem(0);
                bus.post(new LoginRequestEvent(LoginRequestEvent.Type.EXPLICIT_LOGOUT));
                return true;

            case R.id.action_settings:
                showSettings();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_RECORD_VIDEO:
            case REQUEST_CHOOSE_VIDEO:
                createVideo(resultCode, data);
                break;

            // FIXME: Default is not good here
            default:
                QRHelper.readQRCodeResult(this, requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
            case R.id.fab:
                startRecording();
                break;
        }
    }

    @Override
    public void onScrollDown() {
        fab.hide();
    }

    @Override
    public void onScrollUp() {
        fab.show();
    }

    @Override
    public void onShow(Snackbar snackbar) {
        int height = snackbar.getHeight();

        fab.animate()
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(200)
                .translationY(-height)
                .start();
    }

    @Override
    public void onDismissed(Snackbar snackbar) {
        fab.animate()
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(200)
                .translationY(0)
                .start();
    }

    private void startRecording() {
        int cameraCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int locationCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int fileWriteCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int fileReadCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (cameraCheck != PackageManager.PERMISSION_GRANTED ||
                locationCheck != PackageManager.PERMISSION_GRANTED ||
                fileWriteCheck != PackageManager.PERMISSION_GRANTED ||
                fileReadCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            }, ACH_SO_TAKE_VIDEO_PERM);
        } else {
            recordVideo();
        }
    }

    private void recordVideo() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            App.locationManager.startLocationUpdates();
        }

        videoBuilder = VideoCreatorService.build();

        File videoFile = VideoCreatorService.getStorageVideoFile(videoBuilder);
        Uri videoUri = Uri.fromFile(videoFile);

        // Some camera apps (looking at you, Samsung) don't return any data if the EXTRA_OUTPUT
        // flag is set. The storage file is a good fallback in case the camera app doesn't give us
        // a URI.
        videoBuilder.setVideoUri(videoUri);

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);

        try {
            startActivityForResult(intent, REQUEST_RECORD_VIDEO);
        } catch (ActivityNotFoundException e) {
            // TODO: Offer alternatives
            showSnackbar("No camera app is installed.");
        }
    }

    private void chooseVideo() {
        videoBuilder = VideoCreatorService.build();

        Intent intent;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }

        intent.setType("video/mp4");

        try {
            startActivityForResult(intent, REQUEST_CHOOSE_VIDEO);
        } catch (ActivityNotFoundException e) {
            // TODO: Offer alternatives
            showSnackbar("No file manager is installed.");
        }
    }

    private void createVideo(int resultCode, @Nullable Intent resultData) {
        if (resultCode != RESULT_OK) {
            return;
        }

        // Data might not be there, in which case a fallback has been set in #recordVideo().
        if (resultData != null) {
            videoBuilder.setVideoUri(resultData.getData());
        }

        videoBuilder.create(BrowserActivity.this);
    }

    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Subscribe
    public void onLoginState(LoginStateEvent event) {

        if (event.shouldNotifyUser()) {
            switch (event.getState()) {
                case LOGGED_IN:
                    // TODO: Include user info in the event
                    String name = App.loginManager.getUserInfo().get("name").getAsString();
                    String welcome = getString(R.string.logged_in_as, name);

                    showSnackbar(welcome);
                    break;

                case LOGGED_OUT:
                    showSnackbar(R.string.logged_out);
                    break;
            }
        }


        swipeRefreshLayout.setRefreshing(true);
        SyncService.syncWithCloudStorage(this);

        invalidateOptionsMenu();
    }

    @Subscribe
    public void onUploadState(UploadStateEvent event) {

        if (event.getType() == UploadStateEvent.Type.SUCCEEDED) {
            // TODO: There could be many of these, should direct to some multi-share page.
            UUID videoId = event.getVideoId();
            List<UUID> videoIds = Collections.singletonList(videoId);

            SharingActivity.openShareActivity(this, videoIds);
        }
    }

    @Subscribe
    public void onLoginError(LoginErrorEvent event) {
        String message = getString(R.string.login_error, event.getMessage());

        showSnackbar(message);

        invalidateOptionsMenu();
    }

    @Subscribe
    public void onVideoRepositoryUpdated(VideoRepositoryUpdatedEvent event) {
        this.tabAdapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
    }

    @Subscribe
    public void onVideoCreationState(VideoCreatorService.VideoCreationStateEvent event) {
        VideoCreatorService.VideoCreationStateEvent.Type type = event.getType();

        FragmentManager manager = getFragmentManager();
        Fragment fragment = manager.findFragmentByTag("videoCreation");

        switch (type) {
            case STARTED:
                if (fragment == null) {
                    fragment = ProgressDialogFragment.newInstance(this, R.string.processing_video);
                    manager.beginTransaction().add(fragment, "videoCreation").commit();
                }

                break;

            case ERROR:
                showSnackbar(R.string.storage_error);
                // Fall through

            case FINISHED:
                if (fragment != null) {
                    manager.beginTransaction().remove(fragment).commit();
                }

                UUID id = event.getId();
                trimVideo(id);

                break;
        }
    }

    @Subscribe
    public void onSyncRequired(SyncRequiredEvent event) {
        SyncService.syncWithCloudStorage(this);
    }

    @Override
    public void onRefresh() {
        // @Note: This won't be called currently since pull to refresh is disabled (see
        // VideoRefreshLayout#canChildScrollUp). This is kept here in case pull to refresh is
        // implemented later.
        SyncService.syncWithCloudStorage(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // We need all three permissions (Fine location, using the camera, writing to the filesystem
        // Otherwise we just show a toast and exit.
        if (requestCode == ACH_SO_TAKE_VIDEO_PERM) {
            if (!checkPermissions(R.string.video_no_permissions, permissions, grantResults))
                return;

            recordVideo();

        } else if (requestCode == ACH_SO_LOG_IN_PERM) {
            if (!checkPermissions(R.string.log_in_no_permissions, permissions, grantResults))
                return;

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else if (requestCode == ACH_SO_BROWSE_PERM) {
            if (!checkPermissions(R.string.browse_no_permissions, permissions, grantResults))
                return;

            SyncService.syncWithCloudStorage(this);
        }
    }

    private void trimVideo(final UUID videoId) {
        promptUserForTrimming(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                System.out.println(videoId);
            }
        });
    }

    private void promptUserForTrimming(MaterialDialog.SingleButtonCallback callback) {
       promptYesNoDialog(callback, "Cropping", "Would you like to crop the video?", getString(R.string.ok));
    }

    private void promptYesNoDialog(MaterialDialog.SingleButtonCallback callback, String heading, String content, String positiveText) {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(heading)
                .content(content)
                .negativeText(R.string.cancel)
                .positiveText(positiveText)
                .onPositive(callback)
                .build();

        dialog.show();
    }

    private boolean checkPermissions(int messageResource, String permissions[], int[] grantResults) {
        boolean hasPermissions = true;

        if (permissions.length == grantResults.length) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = false;
                    break;
                }
            }
        } else {
            hasPermissions = false;
        }

        if (!hasPermissions) {

            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(messageResource)
                    .negativeText(R.string.cancel)
                    .positiveText(R.string.go_to_settings)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                            myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                            myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(myAppSettings);
                        }
                    })
                    .build();
            dialog.show();

        }

        return hasPermissions;
    }

    /**
     * This class listens for the VideoRepositoryUpdatedEvent and tells if one has been received.
     */
    public static class PendingRepositoryUpdateListener {
        private boolean repositoryUpdated;

        public boolean hasRepositoryUpdated() {
            return repositoryUpdated;
        }

        public void clearRepositoryUpdated() {
            repositoryUpdated = false;
        }

        @Subscribe
        public void onVideoRepositoryUpdated(VideoRepositoryUpdatedEvent event) {
            this.repositoryUpdated = true;
        }
    }
}
