package fi.aalto.legroup.achso.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.adapter.VideoBrowserTabAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.fragment.VideoBrowserFragment;
import fi.aalto.legroup.achso.helper.AuthenticationHelper;
import fi.aalto.legroup.achso.helper.QRHelper;
import fi.aalto.legroup.achso.helper.SettingsHelper;
import fi.aalto.legroup.achso.helper.VideoHelper;
import fi.aalto.legroup.achso.service.UploaderService;
import fi.aalto.legroup.achso.state.LoginManager;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.view.VideoGridItemView;

public class MainActivity extends FragmentActivity {

    private VideoBrowserTabAdapter tabAdapter;
    private ViewPager tabs;
    private ActionBar actionBar;
    private VideoDBHelper databaseHelper;
    private SearchView searchView;
    private MenuItem searchItem;

    protected IntentFilter globalFilter;
    protected GlobalBroadcastReceiver globalReceiver;
    protected IntentFilter localFilter;
    protected LocalBroadcastReceiver localReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        this.databaseHelper = new VideoDBHelper(this);
        this.databaseHelper.updateVideoCache();

        this.tabAdapter = new VideoBrowserTabAdapter(this, this.getSupportFragmentManager());
        this.tabs = (ViewPager) this.findViewById(R.id.pager);
        this.tabs.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar = getActionBar();
                actionBar.setSelectedNavigationItem(position);
                tabAdapter.closeContextualActionMode();
            }
        });
        this.tabs.setAdapter(this.tabAdapter);
        this.actionBar = this.getActionBar();
        this.actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                actionBar.setSelectedNavigationItem(tab.getPosition());
                tabs.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

            }

        };

        this.actionBar.addTab(this.actionBar.newTab().setText(getString(R.string.my_videos)).setTabListener(tabListener));
        for (SemanticVideo.Genre genre : SemanticVideo.Genre.values()) {
            this.actionBar.addTab(this.actionBar.newTab().setText(SemanticVideo.genreStrings.get(genre)).setTabListener(tabListener));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main_menubar, menu);

        SearchManager manager = (SearchManager) this.getSystemService(Context.SEARCH_SERVICE);
        this.searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        this.searchItem = menu.findItem(R.id.action_search);
        this.searchView.setSearchableInfo(manager.getSearchableInfo(this.getComponentName()));


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
            case R.id.action_new_video:
                VideoHelper.videoByRecording(this);
                break;
            case R.id.action_read_qrcode:
                QRHelper.readQRCodeForSearching(this, this.searchItem);
                break;
            case R.id.action_add_video:
                VideoHelper.videoByChoosingFile(this);
                break;
            case R.id.action_login:
                AuthenticationHelper.login(this);
                break;
            case R.id.action_logout:
                AuthenticationHelper.logout(this);
                break;
            case R.id.action_about:
                SettingsHelper.showAboutDialog(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case VideoHelper.ACTIVITY_VIDEO_BY_RECORDING:
                VideoHelper.videoByRecordingResult(this, resultCode, data);
                break;
            case VideoHelper.ACTIVITY_VIDEO_BY_PICKING:
                VideoHelper.videoByChoosingFileResult(this, resultCode, data);
                break;
            case VideoHelper.ACTIVITY_GENRE_SELECTION:
                return;

            case AuthenticationHelper.ACTIVITY_LOGIN:
                AuthenticationHelper.loginResult(this);
                break;
            default:
                QRHelper.readQRCodeResult(this, requestCode, resultCode, data);
                break;
        }
    }

    /**
     * Receives events broadcast by other applications, e.g. network state.
     */
    public class GlobalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                // Try to automatically log in when connected and not logged in.
                if (App.isConnected() && App.loginManager.isLoggedOut())
                    App.loginManager.login();

                // Log out when connectivity is lost, but remember auto-login
                if (App.isDisconnected() && App.loginManager.isLoggedIn())
                    App.loginManager.logout();

                invalidateOptionsMenu();
            }
        }

    }

    public class LocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(LoginManager.ACTION_LOGIN_STATE_CHANGED)) {
                invalidateOptionsMenu();
                AuthenticationHelper.loginStateDidChange(getApplicationContext());
            } else if (action.equals(LoginManager.ACTION_LOGIN_ERROR)) {
                invalidateOptionsMenu();
                AuthenticationHelper.loginDidFail(getApplicationContext(), intent);
            }
        }
    }

    private BroadcastReceiver onNotice = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(UploaderService.UPLOAD_PROGRESS_ACTION)) {
                long id = intent.getLongExtra(UploaderService.PARAM_OUT, -1);
                SemanticVideo video = VideoDBHelper.getById(id);
                VideoBrowserFragment fragment = tabAdapter.getFragmentAtIndex(tabs.getCurrentItem());
                VideoGridItemView view = fragment.getViewForVideo(video);
                int progress = intent.getIntExtra(UploaderService.PARAM_ARG, -1);
                view.setProgress(progress);
                return;
            }

            if (action.equals(UploaderService.UPLOAD_END_ACTION)) {
                long id = intent.getLongExtra(UploaderService.PARAM_OUT, -1);
                SemanticVideo video = VideoDBHelper.getById(id);
                VideoBrowserFragment fragment = tabAdapter.getFragmentAtIndex(tabs.getCurrentItem());
                VideoGridItemView view = fragment.getViewForVideo(video);
                view.setProgress(100);
                return;
            }
        }
    };

    protected void startReceivingBroadcasts() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UploaderService.UPLOAD_PROGRESS_ACTION);
        filter.addAction(UploaderService.UPLOAD_END_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(this.onNotice, filter);

        if (this.globalFilter == null) {
            this.globalFilter = new IntentFilter();
            this.globalFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            this.globalReceiver = new GlobalBroadcastReceiver();
        }
        this.registerReceiver(this.globalReceiver, this.globalFilter);

        if (this.localFilter == null) {
            this.localFilter = new IntentFilter();
            this.localFilter.addAction(LoginManager.ACTION_LOGIN_STATE_CHANGED);
            this.localFilter.addAction(LoginManager.ACTION_LOGIN_ERROR);
            this.localReceiver = new LocalBroadcastReceiver();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(this.localReceiver, this.localFilter);
    }

    protected void stopReceivingBroadcasts() {
        this.unregisterReceiver(this.globalReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.localReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.onNotice);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopReceivingBroadcasts();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.searchItem != null) {
            MenuItemCompat.collapseActionView(this.searchItem);
        }

        if(this.tabs != null && this.tabAdapter != null) {
            int position = this.tabs.getCurrentItem();
            VideoBrowserFragment fragment = this.tabAdapter.getFragmentAtIndex(position);

            if(fragment != null) {
                fragment.setVideos(this.tabAdapter.getVideosForPosition(position));
            }
        }

        startReceivingBroadcasts();
    }
}
