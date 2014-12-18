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

import com.bugsnag.android.Bugsnag;
import com.google.gson.JsonObject;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.authentication.LoginActivity;
import fi.aalto.legroup.achso.authentication.LoginErrorEvent;
import fi.aalto.legroup.achso.authentication.LoginRequestEvent;
import fi.aalto.legroup.achso.authentication.LoginStateEvent;
import fi.aalto.legroup.achso.authoring.GenreDialogFragment;
import fi.aalto.legroup.achso.authoring.QRHelper;
import fi.aalto.legroup.achso.authoring.VideoCreatorService;
import fi.aalto.legroup.achso.storage.VideoRepositoryUpdatedEvent;
import fi.aalto.legroup.achso.storage.local.ExportCreatorTaskResultEvent;
import fi.aalto.legroup.achso.support.AboutDialogFragment;
import fi.aalto.legroup.achso.support.FeedbackDialogFragment;
import fi.aalto.legroup.achso.utilities.ProgressDialogFragment;
import fi.aalto.legroup.achso.views.SlidingTabLayout;
import fi.aalto.legroup.achso.views.adapters.VideoTabAdapter;

public class BrowserActivity extends ActionBarActivity {

    private Bus bus;

    private VideoTabAdapter tabAdapter;
    private MenuItem searchItem;

    private final int REQUEST_RECORD_VIDEO = 1;
    private final int REQUEST_CHOOSE_VIDEO = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

            case R.id.action_about:
                AboutDialogFragment.newInstance(this).show(getFragmentManager(), "AboutDialog");
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_RECORD_VIDEO:
            case REQUEST_CHOOSE_VIDEO:
                if (resultCode == RESULT_OK) {
                    createVideo(data.getData());
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

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
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

    private void createVideo(final Uri contentUri) {
        if (contentUri == null) {
            Bugsnag.notify(new IllegalArgumentException("Result contained a null URI."));
            return;
        }

        GenreDialogFragment fragment = new GenreDialogFragment();

        fragment.setCallback(new GenreDialogFragment.Callback() {
            @Override
            public void onGenreSelected(String genre) {
                VideoCreatorService.create(BrowserActivity.this, contentUri, genre);
            }
        });

        fragment.show(getFragmentManager(), fragment.getClass().getSimpleName());
    }

    @Subscribe
    public void onLoginState(LoginStateEvent event) {
        switch (event.getState()) {
            case LOGGED_IN:
                // TODO: Include user info in the event
                String name = App.loginManager.getUserInfo().get("name").getAsString();
                String welcome = String.format(getString(R.string.logged_in_as), name);

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
        String message = String.format(getString(R.string.login_error), event.getMessage());

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

    @Subscribe
    public void onExportCreatorTaskResult(ExportCreatorTaskResultEvent event) {
        List<Uri> uris = event.getResult();

        if (uris == null) {
            App.showError(R.string.error_sharing);
            return;
        }

        Intent sharingIntent = null;

        if (uris.size() > 1) {
            ArrayList<Uri> uriList = new ArrayList<>(uris);
            sharingIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            sharingIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
        } else {
            if (uris.size() == 0) {
                App.showError(R.string.error_sharing);
                return;
            }
            sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }

        sharingIntent.setType("application/achso");

        this.startActivity(Intent.createChooser(sharingIntent, this.getString(R.string.video_share)));
    }

}
