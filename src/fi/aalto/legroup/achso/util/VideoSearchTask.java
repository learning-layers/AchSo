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

package fi.aalto.legroup.achso.util;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import fi.aalto.legroup.achso.adapter.VideoThumbAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.remote.RemoteResultCache;
import fi.aalto.legroup.achso.remote.RemoteSemanticVideoFactory;
import fi.aalto.legroup.achso.util.xml.XmlConverter;
import fi.aalto.legroup.achso.util.xml.XmlObject;

public class VideoSearchTask extends AsyncTask<String, Double, ArrayList<SemanticVideo>> {

    private final int mPage;
    private Context mCtx;
    private View mView;
    private ProgressBar mSearchProgress;
    private boolean mIsGrid;

    public VideoSearchTask(Context ctx, View view, ProgressBar progress, boolean grid,
                           int cache_page) {
        mCtx = ctx;
        mView = view;
        mSearchProgress = progress;
        mIsGrid = grid;
        mPage = cache_page;
    }

    @Override
    protected ArrayList<SemanticVideo> doInBackground(String... args) {
        ArrayList<SemanticVideo> ret = new ArrayList<SemanticVideo>();
        if (args.length < 1) return ret;
        DefaultHttpClient httpClient = new DefaultHttpClient();

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("title", args[0]));
        String query = URLEncodedUtils.format(params, "utf-8");
        URI url;
        try {
            url = URIUtils.createURI("http", "example.server.com", 1234, "search", query, null);
        } catch (URISyntaxException e) {
            return ret;
        }

        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("accept", "application/json");

        HttpResponse response;
        String output;
        try {
            response = httpClient.execute(getRequest);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                json.append(line);
            }
            httpClient.getConnectionManager().shutdown();
            output = json.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return ret;
        }
        if (output != null) {
            try {
                JSONObject jsonobj = new JSONObject(output);
                JSONArray searchResult = jsonobj.getJSONArray("searchResult");
                for (int i = 0; i < searchResult.length(); ++i) {
                    if (isCancelled()) break;
                    JSONObject obj = searchResult.getJSONObject(i);
                    XmlObject xml = XmlConverter.fromXml(obj.getString("xml"));
                    //RemoteSemanticVideo rsv=new RemoteSemanticVideoFactory().fromXmlObject(xml);
                    ret.add(new RemoteSemanticVideoFactory().fromXmlObject(xml));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    @Override
    protected void onPostExecute(ArrayList<SemanticVideo> remoteVideos) {
        mSearchProgress.setVisibility(View.GONE);

        RemoteResultCache.setCached(mPage, remoteVideos);
        if (mIsGrid) {
            GridView v = (GridView) mView;
            v.setAdapter(new VideoThumbAdapter(mCtx, remoteVideos));
            ((ArrayAdapter) v.getAdapter()).notifyDataSetChanged();
        } else {
            ListView v = (ListView) mView;
            ((VideoThumbAdapter) v.getAdapter()).addAll(remoteVideos);
            ((ArrayAdapter) v.getAdapter()).notifyDataSetChanged();
        }
    }
}
