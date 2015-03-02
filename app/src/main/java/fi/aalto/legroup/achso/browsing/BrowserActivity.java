package fi.aalto.legroup.achso.browsing;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import com.google.gson.JsonObject;
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
import fi.aalto.legroup.achso.support.FeedbackDialogFragment;
import fi.aalto.legroup.achso.utilities.ProgressDialogFragment;
import fi.aalto.legroup.achso.views.SlidingTabLayout;
import fi.aalto.legroup.achso.views.adapters.VideoTabAdapter;

/**
 * Activity for browsing available videos.
 *
 * TODO: Extract video creation stuff into its own activity.
 */
public class BrowserActivity extends ActionBarActivity {

    private static final int REQUEST_RECORD_VIDEO = 1;
    private static final int REQUEST_CHOOSE_VIDEO = 2;

    private static final String STATE_VIDEO_BUILDER = "STATE_VIDEO_BUILDER";

    private Bus bus;

    private VideoTabAdapter tabAdapter;
    private MenuItem searchItem;

    // Video that is "under construction". Sent to VideoCreatorService for processing after it has
    // been recorded.
    private VideoCreatorService.VideoBuilder videoBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            videoBuilder = savedInstanceState.getParcelable(STATE_VIDEO_BUILDER);
        }

        // TODO: Inject instead
        this.bus = App.bus;

        bus.register(this);

        this.setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        this.tabAdapter = new VideoTabAdapter(this, this.getSupportFragmentManager());

        ViewPager tabs = (ViewPager) this.findViewById(R.id.pager);
        tabs.setAdapter(this.tabAdapter);

        SlidingTabLayout slidingTabs = (SlidingTabLayout) this.findViewById(R.id.main_tabs);
        slidingTabs.setViewPager(tabs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.bus.register(this);
    }

    @Override
    protected void onPause() {
        App.bus.unregister(this);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(STATE_VIDEO_BUILDER, videoBuilder);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * FIXME: Temporarily removing the ability to choose videos.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem addVideo = menu.findItem(R.id.action_add_video);

        addVideo.setVisible(false);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main_menubar, menu);

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
        switch (item.getItemId()) {
            case R.id.action_new_video:
                recordVideo();
                return true;

            case R.id.action_add_video:
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

            case R.id.action_feedback:
                String name = "";
                String email = "";

                JsonObject userInfo = App.loginManager.getUserInfo();

                if (userInfo != null) {
                    name = userInfo.get("name").getAsString();
                    email = userInfo.get("email").getAsString();
                }

                FeedbackDialogFragment.newInstance(name, email)
                        .show(getFragmentManager(), "FeedbackDialog");

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_RECORD_VIDEO:
            case REQUEST_CHOOSE_VIDEO:
                if (resultCode == RESULT_OK) {
                    createVideo(data);
                }
                break;

            // FIXME: Default is not good here
            default:
                QRHelper.readQRCodeResult(this, requestCode, resultCode, data);
                break;
        }
    }

    private void recordVideo() {
        App.locationManager.startLocationUpdates();

        videoBuilder = VideoCreatorService.build();

        File videoFile = VideoCreatorService.getStorageVideoFile(videoBuilder);

        // Some camera apps (looking at you, Samsung) don't return any data if the EXTRA_OUTPUT
        // flag is set. The storage file is a good fallback in case the camera app doesn't give us
        // a URI.
        videoBuilder.setVideoUri(Uri.fromFile(videoFile));

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoFile);

        startActivityForResult(intent, REQUEST_RECORD_VIDEO);
    }

    private void chooseVideo() {
        Intent intent;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }

        // TODO: Specify video/* when other mime types are supported
        intent.setType("video/mp4");

        intent.putExtra(Intent.CATEGORY_OPENABLE, true);

        try {
            startActivityForResult(intent, REQUEST_CHOOSE_VIDEO);
        } catch (ActivityNotFoundException e) {
            // TODO: Offer alternatives
            Toast.makeText(this, "No file manager is installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void createVideo(@Nullable Intent resultData) {
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

                Toast.makeText(this, welcome, Toast.LENGTH_SHORT).show();
                break;

            case LOGGED_OUT:
                Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
                break;
        }

        invalidateOptionsMenu();
    }

    @Subscribe
    public void onLoginError(LoginErrorEvent event) {
        String message = getString(R.string.login_error, event.getMessage());

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

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
                Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
                // Fall through

            case FINISHED:
                if (fragment != null) {
                    manager.beginTransaction().remove(fragment).commit();
                }

                break;
        }
    }

}
