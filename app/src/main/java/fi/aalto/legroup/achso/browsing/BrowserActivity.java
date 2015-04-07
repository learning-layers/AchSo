package fi.aalto.legroup.achso.browsing;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;
import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ScrollDirectionListener;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.File;

import javax.annotation.Nullable;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.authentication.LoginActivity;
import fi.aalto.legroup.achso.authentication.LoginErrorEvent;
import fi.aalto.legroup.achso.authentication.LoginRequestEvent;
import fi.aalto.legroup.achso.authentication.LoginStateEvent;
import fi.aalto.legroup.achso.authoring.GenreDialogFragment;
import fi.aalto.legroup.achso.authoring.QRHelper;
import fi.aalto.legroup.achso.authoring.VideoCreatorService;
import fi.aalto.legroup.achso.settings.SettingsActivity;
import fi.aalto.legroup.achso.storage.VideoRepositoryUpdatedEvent;
import fi.aalto.legroup.achso.utilities.ProgressDialogFragment;
import fi.aalto.legroup.achso.views.adapters.VideoTabAdapter;

/**
 * Activity for browsing available videos.
 *
 * TODO: Extract video creation stuff into its own activity.
 */
public final class BrowserActivity extends ActionBarActivity implements View.OnClickListener,
        ScrollDirectionListener {

    private static final int REQUEST_RECORD_VIDEO = 1;
    private static final int REQUEST_CHOOSE_VIDEO = 2;

    private static final String STATE_VIDEO_BUILDER = "STATE_VIDEO_BUILDER";

    private Bus bus;

    private FloatingActionButton fab;
    private VideoTabAdapter tabAdapter;
    private MenuItem searchItem;

    // Video that is "under construction". Sent to VideoCreatorService for processing after it has
    // been recorded.
    private VideoCreatorService.VideoBuilder videoBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Inject instead
        this.bus = App.bus;

        setContentView(R.layout.activity_browser);

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            videoBuilder = savedInstanceState.getParcelable(STATE_VIDEO_BUILDER);
        }

        this.tabAdapter = new VideoTabAdapter(this, getSupportFragmentManager());

        tabAdapter.setScrollDirectionListener(this);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.toolbar_tabs);

        pager.setAdapter(this.tabAdapter);
        tabs.setViewPager(pager);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(this);

        // Control the media volume instead of the ringer volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bus.register(this);
    }

    @Override
    protected void onPause() {
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
        } else {
            menu.findItem(R.id.action_login).setVisible(true);
            menu.findItem(R.id.action_logout).setVisible(false);
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

            case R.id.action_login:
                // TODO: This needs some event magic
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_logout:
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
                recordVideo();
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

    private void recordVideo() {
        App.locationManager.startLocationUpdates();

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
            SnackbarManager.show(Snackbar.with(this).text("No camera app is installed."));
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
            SnackbarManager.show(Snackbar.with(this).text("No file manager is installed."));
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

        GenreDialogFragment fragment = new GenreDialogFragment();

        fragment.setCallback(new GenreDialogFragment.Callback() {
            @Override
            public void onGenreSelected(String genre) {
                videoBuilder.setGenre(genre);
                videoBuilder.create(BrowserActivity.this);
            }
        });

        fragment.show(getFragmentManager(), fragment.getClass().getSimpleName());
    }

    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Subscribe
    public void onLoginState(LoginStateEvent event) {
        switch (event.getState()) {
            case LOGGED_IN:
                // TODO: Include user info in the event
                String name = App.loginManager.getUserInfo().get("name").getAsString();
                String welcome = getString(R.string.logged_in_as, name);

                SnackbarManager.show(Snackbar.with(this).text(welcome));
                break;

            case LOGGED_OUT:
                SnackbarManager.show(Snackbar.with(this).text(R.string.logged_out));
                break;
        }

        invalidateOptionsMenu();
    }

    @Subscribe
    public void onLoginError(LoginErrorEvent event) {
        String message = getString(R.string.login_error, event.getMessage());

        SnackbarManager.show(Snackbar.with(this).text(message));

        invalidateOptionsMenu();
    }

    @Subscribe
    public void onVideoRepositoryUpdated(VideoRepositoryUpdatedEvent event) {
        this.tabAdapter.notifyDataSetChanged();
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
                SnackbarManager.show(Snackbar.with(this).text(R.string.storage_error));
                // Fall through

            case FINISHED:
                if (fragment != null) {
                    manager.beginTransaction().remove(fragment).commit();
                }

                break;
        }
    }

}
