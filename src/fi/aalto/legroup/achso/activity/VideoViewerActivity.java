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

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.fragment.SemanticVideoPlayerFragment;
import fi.aalto.legroup.achso.fragment.VideoViewerFragment;
import fi.aalto.legroup.achso.state.i5LoginState;

import static fi.aalto.legroup.achso.util.App.appendLog;

/**
 * This is the activity that hosts the actual video viewer/editor
 */
public class VideoViewerActivity extends ActionbarActivity {

    public static final int REQUEST_VIDEO_INFORMATION = 6;
    private long mVideoId;
    private int mVideoPositionInSearchCache;
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    protected boolean show_record() {return true;}
    protected boolean show_login() {return false;}
    protected boolean show_qr() {return false;}
    protected boolean show_search() {return false;}


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewer);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            mVideoId = getIntent().getLongExtra(VideoViewerFragment.ARG_ITEM_ID, -1);
            if (mVideoId == -1) {
                mVideoPositionInSearchCache = getIntent().getIntExtra(VideoViewerFragment.ARG_ITEM_CACHE_POSITION, -1);
            }
            modifyCurrentFragments(false);
        }
        if (mFilter == null && mReceiver == null) {
            mFilter = new IntentFilter();
            mFilter.addAction(i5LoginState.LOGIN_SUCCESS);
            mFilter.addAction(i5LoginState.LOGIN_FAILED);
            mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mReceiver = new AchSoBroadcastReceiver();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        initMenu(menu);
        return true;
    }


    void modifyCurrentFragments(boolean replace) {
        // Clear saved SemanticVideoPlayerFragment data
        getSharedPreferences("AchSoPrefs", 0).edit().putBoolean("stateSaved", false).commit();

        // Create the detail fragment and add it to the activity
        // using a fragment transaction.
        appendLog(String.format("Opening video with id %d. ", mVideoId));

        Bundle arguments = new Bundle();
        if (mVideoId != -1) {
            arguments.putLong(VideoViewerFragment.ARG_ITEM_ID, mVideoId);
        } else {
            arguments.putLong(VideoViewerFragment.ARG_ITEM_ID, -1);
            arguments.putInt(VideoViewerFragment.ARG_ITEM_CACHE_POSITION, mVideoPositionInSearchCache);
        }
        Log.i("VideoViewerActivity", "Modifying current fragments -- new video is set to video_viewer_container");
        VideoViewerFragment fragment = new VideoViewerFragment();
        SemanticVideoPlayerFragment videofragment = new SemanticVideoPlayerFragment();
        videofragment.setEditableAnnotations(mVideoId != -1);
        fragment.setArguments(arguments);
        videofragment.setArguments(arguments);
        if (replace) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.video_viewer_container, fragment)
                    .replace(R.id.video_player_content, videofragment)
                    .commitAllowingStateLoss();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.video_viewer_container, fragment)
                    .replace(R.id.video_player_content, videofragment)
                    .commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                //NavUtils.navigateUpTo(this, new Intent(this, VideoBrowserActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        // not sure if something should be done to videoFragment here
        super.onDestroy();
    }

    @Override
    public  void onResume() {
        super.onResume();
        this.registerReceiver(mReceiver, mFilter);
    }
    @Override
    public  void onPause() {
        super.onPause();
        this.unregisterReceiver(mReceiver);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_INFORMATION) {
                modifyCurrentFragments(true); // Update the whole activity
            } else if (requestCode == REQUEST_LOGIN) {
                invalidateOptionsMenu();
                Toast.makeText(this, "Login successful", Toast.LENGTH_LONG).show();
            }
        }
    }

}
