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


import android.util.Log;

import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.remote.SemanticVideoFactory;

public class AaltoConnection implements Connection {

    private static Connection con;
    private String errorCode;
    private boolean connected;
    public static final String API_URL = "http://achso.aalto.fi/server/api/";

    /**
     * Constructor
     */
    public AaltoConnection() {
        this.errorCode = new String();
        this.connected = false;

    }


    /**
     * Method to get video data that can be later expanded to a list of SemanticVideos
     * When this gets called we should already be in asynchronous thread.
     * @param query_type -- query types are defined at BrowsePagerAdapter,
     *                   but the actual implementation for formulating the query in such way that
     *                   the server can understand it is done here.
     *
     * @param query -- if there are keywords to search, locations etc. give them here. (we can
     *              change this to List<String> if necessary.)
     * @return
     */
    @Override
    public List<SemanticVideo> getVideos(int query_type, String query) {
        List<SemanticVideo> res = new ArrayList<SemanticVideo>();
        List<NameValuePair> params = new LinkedList<NameValuePair>();
        // prepare authentication arguments for http GET

        // prepare url to call and its search arguments
        switch (query_type) {

        }
        // start building http GET


        // execute GET


        // handle results

        return res;
    }

    private List<SemanticVideo> runQuery(String command, List<NameValuePair> params) {
        String url = API_URL + command;
        if (params != null && params.size() > 0) {
            url += "?" + URLEncodedUtils.format(params, "utf-8");
        }
        Log.i("AaltoConnection", "Running query "+ url);
        HttpGet get = new HttpGet(url);
        String json_string = getResponseString(get);
        Log.i("AaltoConnection", json_string);
        List<SemanticVideo> results = new ArrayList<SemanticVideo>();
        JSONArray json_array;
        try {
            json_array = new JSONArray(json_string);
            for (int i = 0; i < json_array.length(); i++) {
                JSONObject obj = json_array.getJSONObject(i);
                SemanticVideo sv = SemanticVideoFactory.buildFromJSON(obj);
                results.add(sv);

            }

        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<SemanticVideo>();
        }
        return results;
    }


    private String getResponseString(HttpGet get) {
        String response_string = "";
        HttpClient client = new DefaultHttpClient();
        try {
            HttpResponse response = client.execute(get);
            HttpEntity responseEntity = response.getEntity();
            if(responseEntity!=null) {
                response_string = EntityUtils.toString(responseEntity);
            }
            Log.i("UploaderService", "received response: (" + response.getStatusLine().getStatusCode() + ") " + response_string);

            if (response.getStatusLine().getStatusCode() != 200) {
                response_string = "";
            }
        } catch (ClientProtocolException e) {
            Log.i("UploaderService", "ClientProtocolException caught");
        } catch (IOException e) {
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
        } catch (IllegalStateException e) {
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
        }
        return response_string;
    }


}
