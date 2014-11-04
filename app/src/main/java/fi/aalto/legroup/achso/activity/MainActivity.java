package fi.aalto.legroup.achso.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.adapter.VideoBrowserTabAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.menu.AuthenticationHelper;
import fi.aalto.legroup.achso.menu.NewVideoHelper;
import fi.aalto.legroup.achso.state.LoginManager;
import fi.aalto.legroup.achso.util.App;

public class MainActivity extends FragmentActivity {

    private VideoBrowserTabAdapter tabAdapter;
    private ViewPager tab;
    private ActionBar actionBar;
    private VideoDBHelper databaseHelper;
    private NewVideoHelper mainMenuHelper;

    protected IntentFilter globalFilter;
    protected GlobalBroadcastReceiver globalReceiver;
    protected IntentFilter localFilter;
    protected LocalBroadcastReceiver localReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mainMenuHelper = new NewVideoHelper();

        this.databaseHelper = new VideoDBHelper(this);
        this.databaseHelper.updateVideoCache();

        this.tabAdapter = new VideoBrowserTabAdapter(this, this.getSupportFragmentManager(), this.databaseHelper);
        this.tab = (ViewPager) this.findViewById(R.id.pager);
        this.tab.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar = getActionBar();
                actionBar.setSelectedNavigationItem(position);
            }
        });
        this.tab.setAdapter(this.tabAdapter);
        this.actionBar = this.getActionBar();
        this.actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {

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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_new_video:
                NewVideoHelper.record(this);
                break;
            case R.id.action_login:
                AuthenticationHelper.login(this);
                break;
            case R.id.action_logout:
                AuthenticationHelper.logout(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case NewVideoHelper.ACTIVITY_NUMBER:
                NewVideoHelper.result(this, resultCode, data);
                break;
            case AuthenticationHelper.LOGIN_ACTIVITY_NUMBER:
                AuthenticationHelper.loginResult(this, resultCode, data);
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

                switch (App.loginManager.getState()) {
                    case LOGGED_IN:
                        String name = App.loginManager.getUserInfo().get("name").getAsString();
                        String welcome = String.format(getString(R.string.logged_in_as), name);

                        Toast.makeText(getApplicationContext(), welcome,
                                Toast.LENGTH_LONG).show();
                        break;

                    case LOGGED_OUT:
                        Toast.makeText(getApplicationContext(), R.string.logged_out,
                                Toast.LENGTH_LONG).show();
                        break;
                }
            } else if (action.equals(LoginManager.ACTION_LOGIN_ERROR)) {
                invalidateOptionsMenu();

                String error = intent.getStringExtra(LoginManager.KEY_MESSAGE);
                String message = String.format(getString(R.string.login_error), error);

                Toast.makeText(getApplicationContext(), message,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void startReceivingBroadcasts() {
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
    }

    @Override
    public void onPause() {
        super.onPause();
        stopReceivingBroadcasts();
    }

    @Override
    public void onResume() {
        super.onResume();
        startReceivingBroadcasts();
    }
}
