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

package fi.aalto.legroup.achso.util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

import fi.aalto.legroup.achso.adapter.ImageAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.remote.RemoteSemanticVideoFactory;
import fi.aalto.legroup.achso.util.xml.XmlConverter;
import fi.aalto.legroup.achso.util.xml.XmlObject;
import fi.aalto.legroup.achso.view.ExpandableGridView;

public class MultimediaOperationsTask extends AsyncTask<String, Double, ArrayList<SemanticVideo>> {

    private Context mCtx;
    private View mView;
    private ProgressBar mSearchProgress;
    private boolean mIsGrid;

    public MultimediaOperationsTask(Context ctx, View view, ProgressBar progress, boolean grid) {
        mCtx = ctx;
        mView = view;
        mSearchProgress = progress;
        mIsGrid = grid;
    }

    @Override
    protected void onPreExecute() {
        mSearchProgress.setVisibility(View.VISIBLE);
    }

    @Override
    protected ArrayList<SemanticVideo> doInBackground(String... arg0) {
        ArrayList<SemanticVideo> ret = new ArrayList<SemanticVideo>();
        String videoInformations = LasConnection.getConnection().getVideoInformations();
        if (videoInformations == null)
            return ret;
        XmlObject xmlobj = XmlConverter.fromXml(videoInformations);
        for (XmlObject o : xmlobj.getSubObjects()) {
            ret.add(new RemoteSemanticVideoFactory().fromXmlObject(o));
        }
        return ret;
    }

    @Override
    protected void onProgressUpdate(Double... progress) {
        mSearchProgress.setProgress(progress[0].intValue());
    }

    @Override
    protected void onPostExecute(ArrayList<SemanticVideo> remoteVideos) {
        mSearchProgress.setVisibility(View.GONE);
        Log.i("MultimediaOperationsTask", "Executed search, found " + remoteVideos.size()
                + "videos.");
        SearchResultCache.lastSearch = remoteVideos;

        if (mIsGrid) {
            ExpandableGridView v = (ExpandableGridView) mView;
            ImageAdapter adapter = new ImageAdapter(mCtx, remoteVideos);
            v.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        } else {
            ListView v = (ListView) mView;
            ((ImageAdapter) v.getAdapter()).addAll(remoteVideos);
            ((ArrayAdapter) v.getAdapter()).notifyDataSetChanged();
        }
    }

    @Override
    protected void onCancelled() {

    }
}
