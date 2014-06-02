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

package fi.aalto.legroup.achso.upload;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import fi.aalto.legroup.achso.activity.VideoBrowserActivity;
import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.LasConnection;
import fi.aalto.legroup.achso.util.xml.XmlConverter;

import static fi.aalto.legroup.achso.upload.UploaderService.updateVideoMetadataToAalto;
import static fi.aalto.legroup.achso.util.App.appendLog;

public class PollingService extends IntentService {

    public static final String AALTO_API_URL = "http://achso.aalto.fi/server/api/";
    public static final String VIDEO_KEY = "key";
    public static final String USERID_PART = "user_id";
    private Handler poll_handler;
    private String polling_path;
    private String key;


    public PollingService() {
        super("AchSoPollingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        key = intent.getStringExtra(VIDEO_KEY);
        String user_id = intent.getStringExtra(USERID_PART);
        if (key == null) {

        }

        Log.i("PollingService", "Received intent to poll occassionally");
        //http://137.226.58.27:9080/ClViTra_2.0/rest/videos/testuser/transcoded

        polling_path = "http://137.226.58.27:9080/ClViTra_2.0/rest/videos/" + user_id +
                "/transcoded";
        Log.i("PollingService", "poll url: " + polling_path);


        poll_handler = new Handler();
        switch (App.video_uploader) {
            case App.CLVITRA2:
                Runnable poll_runner = new Runnable() {
                    @Override
                    public void run() {
                        Log.i("PollingService", "Running timed poll");
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpGet get = new HttpGet(polling_path);
                        StringBuilder builder = new StringBuilder();
                        try {
                            HttpResponse response = httpclient.execute(get);
                            StatusLine statusLine = response.getStatusLine();
                            int statusCode = statusLine.getStatusCode();
                            if (statusCode == 200) {
                                HttpEntity entity = response.getEntity();
                                InputStream content = entity.getContent();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    builder.append(line);
                                }
                            } else {
                                Log.e("PollingService", "Failed to download file");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String video_url = "";
                        String thumbnail_url = "";
                        boolean found = false;

                        if (builder.length() > 0) {
                            try {
                                JSONObject videos_json = new JSONObject(builder.toString());
                                JSONArray jsonArray = videos_json.getJSONArray("Videos");
                                for (int i = 0; i < jsonArray.length() && !found; i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    if (jsonObject.getString("Video_URL").contains(key)) {
                                        found = true;
                                        video_url = jsonObject.getString("Video_URL");
                                        thumbnail_url = jsonObject.getString("Thumbnail_URL");
                                    }
                                    ;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (found) {
                            // modify video metadata to include new video url
                            Log.i("PollingService", "Found matching Video_URL:" + video_url);
                            SemanticVideo sem_video = VideoDBHelper.getByKey(key);
                            List<String> updated_fields = new ArrayList<String>();
                            sem_video.setRemoteVideo(video_url);
                            updated_fields.add("remote_video");
                            sem_video.setRemoteThumbnail(thumbnail_url);
                            updated_fields.add("remote_thumbnail");
                            boolean success = updateVideoMetadataToAalto(sem_video, updated_fields);
                            if (success) {
                                sem_video.setUploadStatus(SemanticVideo.UPLOADED);
                                VideoDBHelper vdb = new VideoDBHelper(App.getContext());
                                vdb.update(sem_video);
                                vdb.close();
                                App.removePollingReminder(key);
                            }
                        } else {
                            // try again soon
                            Log.i("PollingService", "Try again soon: didn't find key " + key);
                            poll_handler.postDelayed(this, 5000);
                        }
                    }
                };
                poll_runner.run();
                break;
            default:

        }

    }




}


