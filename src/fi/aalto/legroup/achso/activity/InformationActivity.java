/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
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
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.fragment.InformationFragment;
import fi.aalto.legroup.achso.fragment.VideoViewerFragment;
import fi.aalto.legroup.achso.remote.RemoteResultCache;

public class InformationActivity extends FragmentActivity {

    public boolean informationChanged = false;
    public SemanticVideo semanticVideo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_information);
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState == null) {
            Long id = getIntent().getLongExtra(VideoViewerFragment.ARG_ITEM_ID, -1);
            if (id == -1) {
                semanticVideo = RemoteResultCache.getSelectedVideo();
            } else {
                semanticVideo = VideoDBHelper.getById(id);
            }
            getSupportFragmentManager().
                    beginTransaction().
                    replace(R.id.information_container, new InformationFragment()).
                    commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(informationChanged ? RESULT_OK : RESULT_CANCELED);
                finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
