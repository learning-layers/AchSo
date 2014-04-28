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

package fi.aalto.legroup.achso.remote;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.fragment.BrowseFragment;
import fi.aalto.legroup.achso.remote.RemoteResultCache;
import fi.aalto.legroup.achso.remote.RemoteSemanticVideoFactory;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.LasConnection;
import fi.aalto.legroup.achso.util.i5Connection;
import fi.aalto.legroup.achso.util.xml.XmlConverter;
import fi.aalto.legroup.achso.util.xml.XmlObject;

public class RemoteFetchTask extends AsyncTask<String, Double, ArrayList<SemanticVideo>> {

    private final int mPage;
    private WeakReference<BrowseFragment> mFragment;
    private WeakReference<ProgressBar> mSearchProgress;
    private String mQuery;
    private int mQueryType;

    public RemoteFetchTask(BrowseFragment fragment, ProgressBar progress, int cache_page) {
        mFragment = new WeakReference<BrowseFragment>(fragment);
        mSearchProgress = new WeakReference<ProgressBar>(progress);
        mPage = cache_page;
        mQuery = fragment.getQuery();
        mQueryType = fragment.getQueryType();
    }

    @Override
    protected void onPreExecute() {
        ProgressBar pb = mSearchProgress.get();
        if (pb != null) {
            pb.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected ArrayList<SemanticVideo> doInBackground(String... arg0) {
        ArrayList<SemanticVideo> ret = new ArrayList<SemanticVideo>();

        //String videoInformations = i5Connection.getVideos(mQueryType, mQuery);
        String videoInformations = App.connection.getVideos(mQueryType, mQuery);

        Log.i("RemoteFetchTask", "Received response data: " + videoInformations);
        if (videoInformations == null || videoInformations.isEmpty()) {
            Log.i("RemoteFetchTask", "Response is empty. ");
            return ret;
        }
        Log.i("RemoteFetchTask", "Trying to convert from xml: " + videoInformations);
        XmlObject xmlobj = XmlConverter.fromXml(videoInformations);
        for (XmlObject o : xmlobj.getSubObjects()) {
            ret.add(new RemoteSemanticVideoFactory().fromXmlObject(o));
        }
        return ret;
    }

    @Override
    protected void onProgressUpdate(Double... progress) {
        ProgressBar pb = mSearchProgress.get();
        if (pb != null) {
            pb.setProgress(progress[0].intValue());
        }
    }

    @Override
    protected void onPostExecute(ArrayList<SemanticVideo> remoteVideos) {
        ProgressBar pb = mSearchProgress.get();
        if (pb != null) {
            pb.setVisibility(View.GONE);
        }
        Log.i("RemoteFetchTask", "Fetched videos, found " + remoteVideos.size());
        // lets do caching even if the page/fragment is disabled -- it is there for later use
        RemoteResultCache.setCached(mPage, remoteVideos);
        BrowseFragment f = mFragment.get();
        if (f != null) {
            f.finishRemoteVideoFetch(remoteVideos);
        }
    }

    @Override
    protected void onCancelled() {

    }
}
