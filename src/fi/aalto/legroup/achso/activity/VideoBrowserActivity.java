/**
 * Copyright 2013 Aalto university, see AUTHORS
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

package fi.aalto.legroup.achso.activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.adapter.BrowsePagerAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.fragment.BrowseFragment;
import fi.aalto.legroup.achso.fragment.VideoViewerFragment;
import fi.aalto.legroup.achso.remote.RemoteResultCache;
import fi.aalto.legroup.achso.state.IntentDataHolder;
import fi.aalto.legroup.achso.state.LoginState;
import fi.aalto.legroup.achso.upload.UploaderService;
import fi.aalto.legroup.achso.util.App;
import fi.google.zxing.integration.android.IntentIntegrator;
import fi.google.zxing.integration.android.IntentResult;

public class VideoBrowserActivity extends ActionbarActivity implements BrowseFragment.Callbacks,
        ViewPager.OnPageChangeListener {

    private List<SemanticVideo> mSelectedVideosForQrCode;
    private UploaderBroadcastReceiver mLocalReceiver = null;
    private IntentFilter mLocalFilter = null;
    private AchSoBroadcastReceiver mReceiver = null;
    private IntentFilter mFilter = null;
    private String mQrResult = null;
    private String mQuery = null;
    private FragmentStatePagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private SearchView mSearchView;
    private int mQueryType;

    protected boolean show_record() {return true;}
    protected boolean show_login() {return true;}
    protected boolean show_qr() {return true;}
    protected boolean show_search() {return true;}

    public static final int TITLE_QUERY = 1;
    public static final int QR_QUERY = 2;


    public void setSelectedVideosForQrCode(HashMap<Integer, SemanticVideo> videos) {
        mSelectedVideosForQrCode = new ArrayList<SemanticVideo>();
        for (SemanticVideo v : videos.values()) {
            mSelectedVideosForQrCode.add(v);
        }
    }

    /**
     * Creates browsing top menu and does all other initialization for browsing/search views.
     * @param menu
     * @return
     */
    @Override
    @SuppressLint("NewApi")
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i("VideoBrowserActivity", "Inflating options menu - VideoBrowserActivity");
        App.login_state.autologinIfAllowed();
        initMenu(menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem si = menu.findItem(R.id.action_search);
        assert (si != null);
        // search view is just the search box on top of the screen
        mSearchView = (SearchView) si.getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(true);
        final Context ctx = this;
        final SearchView sv = mSearchView;

        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    sv.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(sv.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            si.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    Log.i("VideoBrowserActivity", "MenuItem collapsed. Doing stuff with browse " +
                            "pager adapter");
                    mPagerAdapter = new BrowsePagerAdapter(ctx, getSupportFragmentManager());
                    mViewPager.setAdapter(mPagerAdapter);
                    mPagerAdapter.notifyDataSetChanged();
                    mQuery = null;
                    return true;
                }
            });
        } else {// Not 100% sure that this works on api lvl 11
            mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    Log.i("VideoBrowserActivity", "MenuItem closed. Doing stuff with browse " +
                            "pager adapter");
                    mPagerAdapter = new BrowsePagerAdapter(ctx, getSupportFragmentManager());
                    mViewPager.setAdapter(mPagerAdapter);
                    mPagerAdapter.notifyDataSetChanged();
                    mQuery = null;
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    protected void startReceivingBroadcasts() {
        // Start receiving system / inter app broadcasts
        if (mFilter == null || mReceiver == null) {
            mFilter = new IntentFilter();
            mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mReceiver = new AchSoBroadcastReceiver();
        }
        this.registerReceiver(mReceiver, mFilter);
        // Start receiving local broadcasts
        if (mLocalFilter == null || mLocalReceiver == null) {
            mLocalFilter = new IntentFilter();
            mLocalFilter.addAction(UploaderBroadcastReceiver.UPLOAD_START_ACTION);
            mLocalFilter.addAction(UploaderBroadcastReceiver.UPLOAD_PROGRESS_ACTION);
            mLocalFilter.addAction(UploaderBroadcastReceiver.UPLOAD_END_ACTION);
            mLocalFilter.addAction(UploaderBroadcastReceiver.UPLOAD_ERROR_ACTION);
            mLocalFilter.addAction(LoginState.LOGIN_SUCCESS);
            mLocalFilter.addAction(LoginState.LOGIN_FAILED);
            mLocalFilter.addCategory(Intent.CATEGORY_DEFAULT);
            mLocalReceiver = new UploaderBroadcastReceiver();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, mLocalFilter);

    }

    @Override
    protected void stopReceivingBroadcasts() {
        this.unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Main concern here is to set up filters so that the browse page can react to background
     * processes and system events.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mainmenu);
        Log.i("VideoBrowserActivity", "Registering receivers in onCreate.");

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new BrowsePagerAdapter(this, getSupportFragmentManager()); //, mQuery,
                //mQueryType);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(this);

        if (getIntent() != null) {
            onNewIntent(getIntent());
        }

        App.getLocation();
    }


    @Override
    public void onLocalItemSelected(long id) {
        Intent detailIntent = new Intent(this, VideoViewerActivity.class);
        detailIntent.putExtra(VideoViewerFragment.ARG_ITEM_ID, id);
        startActivity(detailIntent);
    }

    @Override
    public void onRemoteItemSelected(int positionInCache, SemanticVideo sv) {
        Intent detailIntent = new Intent(this, VideoViewerActivity.class);
        detailIntent.putExtra(VideoViewerFragment.ARG_ITEM_CACHE_POSITION, positionInCache);
        startActivity(detailIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // ActionbarActivity handles REQUEST_VIDEO_CAPTURE,
        // REQUEST_LOGIN and REQUEST_LOCATION_SERVICES.
        // The ones handled here affect the search results. (Note that the actual searching and
        // browsing is not done by calling activities/ using intents,
        // they are more about manipulating fragments here.
        super.onActivityResult(requestCode, resultCode, intent);
            switch (requestCode) {
                // Last activity was genre selection
                case REQUEST_SEMANTIC_VIDEO_GENRE:
                    if (resultCode == RESULT_OK) {
                        // Update the list view as we have more data
                        VideoDBHelper vdb = new VideoDBHelper(this);
                        vdb.updateVideoCache();
                        vdb.close();
                        if (intent == null) {
                            Log.i("itemListActivity", "something failed: camera resulted an empty intent.");
                        } else {
                            Toast.makeText(this, getString(R.string.created_new_video), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d("BACK", "Genre selection canceled");
                        // Stay in video browser action -- do nothing?
                    }
                    break;
                case IntentIntegrator.REQUEST_CODE:
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
                    if (scanResult != null) {
                        if (IntentDataHolder.From == ActionbarActivity.class) {
                            if (!scanResult.getContents().equals(mQuery)) {
                                mQuery = scanResult.getContents();
                                mQueryType = QR_QUERY;
                                RemoteResultCache.clearCache(mViewPager.getCurrentItem());
                            }
                            // last argument in next line is 'isItTitleQuery'. false means qr-query.
                            ActionBar bar = getActionBar();
                            if (bar != null) {
                                bar.setDisplayHomeAsUpEnabled(true); // Enable back button
                            }
                        }
                        /** not used currently, but keep the code if we need to do this
                         *
                         * else if (scanResult.getContents() != null
                                && IntentDataHolder.From == SemanticVideoPlayerFragment.class) {
                            mQrResult = scanResult.getContents();
                        }
                         */
                    }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateOptionsMenu();

        if (mQuery != null) {
            mSearchView.setQuery(mQuery, false);
            mSearchView.clearFocus();
        }
        if (mQrResult != null) {
            VideoDBHelper vdb = new VideoDBHelper(this);
            for (SemanticVideo v : mSelectedVideosForQrCode) {
                // Again, a place for database optimization.
                v.setQrCode(mQrResult);
                vdb.update(v);
            }
            vdb.close();
            Toast.makeText(this, getString(R.string.code_added_to_videos),
                    Toast.LENGTH_LONG).show();
            mQrResult = null;
        }
    }

    private Pair<ProgressBar, ImageView> getViewsForUploadUi(SemanticVideo sv) {
        BrowseFragment f = (BrowseFragment) mPagerAdapter.getItem(mViewPager.getCurrentItem());
        return f.getUiElementsFor(sv);
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {
        int k = Math.max(i - 1, 0);
        int m = Math.min(i + 1, mPagerAdapter.getCount() - 1);
        for (int j = k; j <= m; ++j) { // Finish action modes on both sides of
            // current fragment
            Log.i("VideoBrowserActivity", "Checking and finishing action mode for page "+ j);
            ActionMode am = ((BrowseFragment) mPagerAdapter.getItem(j)).getActionMode();
            if (am != null)
                am.finish();
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    @Override
    protected void onNewIntent(Intent i) {
        String action = i.getAction();
        if (action != null && action.equals(Intent.ACTION_SEARCH)) {
            switchToSearchPage(i);
        }
    }

    private void switchToSearchPage(Intent intent) {
        String query = intent.getStringExtra(SearchManager.QUERY);
        if (query != null && !query.equals(mQuery)) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);
            mQueryType = TITLE_QUERY;
            RemoteResultCache.clearCache(mViewPager.getCurrentItem());
        }
    }

    /**
     * Remove search query or other browse -related data and return to 'front page'
     */
    private void cleanBrowsePage() {
        mViewPager.getAdapter().notifyDataSetChanged();
        mQuery = null;
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(false); // Disable back
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                cleanBrowsePage();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        App.appendLog("Closing Ach So!");
        super.onDestroy();
    }

    public class UploaderBroadcastReceiver extends AchSoLocalBroadcastReceiver {
        public static final String UPLOAD_START_ACTION = "fi.aalto.legroup.achso.intent.action.UPLOAD_START";
        public static final String UPLOAD_PROGRESS_ACTION = "fi.aalto.legroup.achso.intent.action.UPLOAD_PROGRESS";
        public static final String UPLOAD_END_ACTION = "fi.aalto.legroup.achso.intent.action.UPLOAD_END";
        public static final String UPLOAD_ERROR_ACTION = "fi.aalto.legroup.achso.intent.action.UPLOAD_ERROR";

        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            String action = intent.getAction();
            if (action != null && (action.equals(UPLOAD_START_ACTION) || action.equals
                    (UPLOAD_PROGRESS_ACTION) ||
                action.equals(UPLOAD_END_ACTION) || action.equals(UPLOAD_ERROR_ACTION))) {

                long id = intent.getLongExtra(UploaderService.PARAM_OUT, -1);
                if (id == -1)
                    return;
                SemanticVideo sv = VideoDBHelper.getById(id);
                Pair<ProgressBar, ImageView> ui = getViewsForUploadUi(sv);
                if (ui == null)
                    return;
                if (action.equals(UPLOAD_START_ACTION)) {
                    Log.i("UploaderBroadcastReceiver", "Received upload start action ");
                    sv.setUploadStatus(SemanticVideo.UPLOADING);
                    ui.first.setVisibility(View.VISIBLE);
                    ui.second.setColorFilter(getResources().getColor(R.color.upload_icon_uploading));
                } else if (action.equals(UPLOAD_PROGRESS_ACTION)) {
                    int percentage = intent.getIntExtra(UploaderService.PARAM_ARG, 0);
                    ui.first.setProgress(percentage);
                } else if (action.equals(UPLOAD_END_ACTION)) {
                    Log.i("UploaderBroadcastReceiver", "Received upload end action ");
                    sv.setUploadStatus(SemanticVideo.UPLOADED);
                    sv.setInCloud(true);

                    VideoDBHelper vdb = new VideoDBHelper(context);
                    vdb.update(sv);
                    vdb.close();

                    ui.first.setVisibility(View.GONE);
                    ui.second.setVisibility(View.GONE);

                    Toast.makeText(context, getString(R.string.upload_successful), Toast.LENGTH_LONG).show();
                } else if (action.equals(UPLOAD_ERROR_ACTION)) {
                        Log.i("UploaderBroadcastReceiver", "Received upload error action ");
                        sv.setUploadStatus(SemanticVideo.UPLOAD_ERROR);
                        ui.first.setVisibility(View.GONE);
                        ui.second.setVisibility(View.GONE);
                        String errmsg = intent.getStringExtra(UploaderService.PARAM_ARG);
                        Toast.makeText(context, errmsg, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
