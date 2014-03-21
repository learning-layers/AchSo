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

import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.Dialog;

import static fi.aalto.legroup.achso.util.App.appendLog;

public class GenreSelectionActivity extends Activity {

    private Uri mVideoUri;
    private DialogInterface.OnClickListener mGenreClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            finishWithVideo(SemanticVideo.Genre.values()[which], true);
        }
    };

    private static String ordinal(int i) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];

        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genreselection);
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        mVideoUri = Uri.parse(getIntent().getStringExtra("videoUri"));
        Log.i("GenreSelectionActivity", "VideoUri is sent around. Now its: " + mVideoUri);
        Dialog.getGenreDialog(this, mGenreClick).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void finishWithVideo(SemanticVideo.Genre g, boolean use_qr_code) {
        VideoDBHelper vdb = new VideoDBHelper(this);
        int count = vdb.getNumberOfVideosToday();
        String dayname = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(new Date());
        String creator = null;
        if (App.login_state.isIn()) {
            creator = App.login_state.getUser();
        }
        String vid_name = ordinal(count + 1) + " video of " + dayname;
        SemanticVideo newvideo = new SemanticVideo(vid_name, mVideoUri, g, creator);
        Intent result = new Intent();
        vdb.insert(newvideo);
        vdb.close();
        result.putExtra("video_id", newvideo.getId());
        setResult(RESULT_OK, result);
        appendLog(String.format("Created video %s in genre %s to uri %s", vid_name ,g.name(), mVideoUri.toString()));
        finish();
    }
}
