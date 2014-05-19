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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import fi.aalto.legroup.achso.activity.VideoBrowserActivity;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.LasConnection;
import fi.aalto.legroup.achso.util.xml.XmlConverter;

import static fi.aalto.legroup.achso.util.App.appendLog;

public class UploaderService extends IntentService {

    public static final String PARAM_IN = "in";
    public static final String PARAM_OUT = "out";
    public static final String PARAM_WHAT = "what";
    public static final String PARAM_ARG = "arg";

    public static final int UPLOAD_START = 0;
    public static final int UPLOAD_PROGRESS = 1;
    public static final int UPLOAD_END = 2;
    public static final int UPLOAD_ERROR = 3;


    public UploaderService() {
        super("AchSoUploaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long id = intent.getLongExtra(PARAM_IN, -1);

        Log.i("UploaderService", "Received intent to upload");


        if (id != -1) {
            SemanticVideo sem_video = VideoDBHelper.getById(id);
            switch (App.video_uploader) {
                case App.CLVITRA2:
                    uploadVideoToClViTra2Service(sem_video);
                    break;
                case App.CLVITRA:
                    //uploadVideoToClViTra(sem_video);
                    break;
                case App.AALTO_TEST_SERVER:
                    uploadVideoToAaltoTestService(sem_video);
                    break;
                case App.DEV_NULL:
                    uploadVideoToNowhere(sem_video);
            }
            switch (App.metadata_uploader) {
                case App.CLVITRA2:
                    uploadMetadataToClViTra2Service(sem_video);
                    break;
                case App.CLVITRA:
                    uploadMetadataToClViTra(sem_video);
                    break;
                case App.AALTO_TEST_SERVER:
                    uploadMetadataToAaltoTestService(sem_video);
                    break;
            }

        }
    }

    /**
     * Moved announcing upload start and progress to their own method,
     * to avoid repeating them in different uploaders and to make the video_uploader code more readable.
     *
     * This takes the content of the upload, makes it a pollable entity and attaches necessary
     * intents for broadcasting the progress of upload.
     * @param entity
     * @param transfer_id -- id (Long) of whatever file is being transferred. Broadcast receiver
     *                    decides what to do with it.
     * @return
     */
    private PollableHttpEntity enableProgressBroadcasting(HttpEntity entity, final Long transfer_id) {
        // broadcast that the upload is starting
        final LocalBroadcastManager broadcast_manager = LocalBroadcastManager.getInstance(this);
        final Long transfer_size = entity.getContentLength();
        Intent startUploadIntent = new Intent();
        startUploadIntent.setAction(VideoBrowserActivity.UploaderBroadcastReceiver
                .UPLOAD_START_ACTION);
        startUploadIntent.addCategory(Intent.CATEGORY_DEFAULT);
        startUploadIntent.putExtra(PARAM_OUT, transfer_id);
        startUploadIntent.putExtra(PARAM_WHAT, UPLOAD_START);
        broadcast_manager.sendBroadcast(startUploadIntent);
        Log.i("UploaderService", "Broadcasting intent UPLOAD_START_ACTION");
        return new PollableHttpEntity(entity, new PollableHttpEntity.ProgressListener() {
            // broadcast upload progress
            @Override
            public void transferred(long bytes) {
                Intent progressIntent = new Intent();
                progressIntent.setAction(VideoBrowserActivity.UploaderBroadcastReceiver.UPLOAD_PROGRESS_ACTION);
                progressIntent.addCategory(Intent.CATEGORY_DEFAULT);
                progressIntent.putExtra(PARAM_OUT, transfer_id);
                progressIntent.putExtra(PARAM_WHAT, UPLOAD_PROGRESS);
                int percentage = (int) (((double) bytes / transfer_size) * 100.0);
                progressIntent.putExtra(PARAM_ARG, percentage);
                broadcast_manager.sendBroadcast(progressIntent);

                // broadcast upload finish
                if (percentage == 100) {
                    Intent endIntent = new Intent();
                    endIntent.setAction(VideoBrowserActivity.UploaderBroadcastReceiver.UPLOAD_END_ACTION);
                    endIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    endIntent.putExtra(PARAM_OUT, transfer_id);
                    endIntent.putExtra(PARAM_WHAT, UPLOAD_END);
                    broadcast_manager.sendBroadcast(endIntent);
                }
            }
        });
    }

    /**
     * Helper method to announce UI that something went wrong.
     * @param errmsg
     * @param traffic_id -- id of the file that is sent. BroadcastReceiver decides what to do
     *                   with this info.
     */
    private void announceError(String errmsg, Long traffic_id) {
        // broadcast upload error
        Intent endIntent = new Intent();
        endIntent.setAction(VideoBrowserActivity.UploaderBroadcastReceiver.UPLOAD_ERROR_ACTION);
        endIntent.addCategory(Intent.CATEGORY_DEFAULT);
        endIntent.putExtra(PARAM_OUT, traffic_id);
        endIntent.putExtra(PARAM_WHAT, UPLOAD_ERROR);
        endIntent.putExtra(PARAM_ARG, errmsg);
        Log.i("UploaderService", "Broadcasting error message: " + errmsg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(endIntent);
    }

    private void uploadVideoToClViTra2Service(SemanticVideo sem_video) {
        // This is new video_uploader using the i5Cloud services
        // Now implemented a simple stub that sends video file as a PUT to some url.

        // prepare file for sending
        Long traffic_id = sem_video.getId();
        String url = "http://www.example.com/resource"; // replace this with something real
        String token = ""; // replace this with something real
        HttpClient client = new DefaultHttpClient();
        HttpPut put= new HttpPut(url);
        File file = new File(sem_video.getUri().getPath());
        FileEntity fe = new FileEntity(file, "binary/octet-stream");
        fe.setChunked(true);
        PollableHttpEntity broadcasting_entity = enableProgressBroadcasting(fe, traffic_id);
        put.setEntity(broadcasting_entity);

        put.setHeader("X-Auth-Token", token);
        put.setHeader("Content-type", "application/x-www-form-urlencoded");
        try {
            Log.i("UploaderService", "sending PUT to " + url);
            HttpResponse response = client.execute(put);
            Log.i("UploaderService", "response:" + response.getStatusLine().toString());
            appendLog("response:" + response.getStatusLine().toString());
        } catch (ClientProtocolException e) {
            announceError("Sorry, error in transfer.", traffic_id);
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
        } catch (IOException e) {
            announceError("Sorry, couldn't connect to server.", traffic_id);
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
        } catch (IllegalStateException e) {
            announceError("Bad or missing server name.", traffic_id);
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
        }
        // hmm, what to do with the response?
    }

    private void uploadVideoToClViTra(SemanticVideo sem_video) {
        final LocalBroadcastManager broadcast_manager = LocalBroadcastManager.getInstance(this);
        final Context context = this;

        String server_url = "http://merian.informatik.rwth-aachen.de:5080/ClViTra/FileUploadServlet";
        //give an unique name to the video and xml files stored on server
        String filename = sem_video.getKey();
        if (filename == null || filename.isEmpty()) {
            filename = sem_video.createKey();
            VideoDBHelper vdb = new VideoDBHelper(context);
            vdb.update(sem_video);
            vdb.close();
        }
        sem_video.setUploadStatus(SemanticVideo.UPLOADING);
        HttpClient httpclient = new DefaultHttpClient();
        //added source and file id to the http post
        HttpPost httppost = new HttpPost(server_url + "?source=achso&uid=" + filename);
        LasConnection las = (LasConnection) App.connection;
        String username = las.getClient().getUser();
        String pass = new String(Base64.encode(las.getPass().getBytes(), 0));
        appendLog(String.format("Uploading video %d to %s as file %s", sem_video.getId(), server_url, filename));
        //String pass = las.getSessionId();
        String userData = username + ":" + pass;

        //sem_video.setCreator(userData);

        MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
        multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        multipartEntity.addPart(userData, new FileBody(new File(sem_video.getUri().getPath())));
        //multipartEntity.addTextBody("source", "achso");
        multipartEntity.addBinaryBody("xml", XmlConverter.toXML(context, sem_video));

        PollableHttpEntity pollable_entity = enableProgressBroadcasting(multipartEntity.build(),
                sem_video.getId());

        httppost.setEntity(pollable_entity);
        try {
            Log.i("UploaderService", "sending POST to " + server_url);
            HttpResponse response = httpclient.execute(httppost);
            Log.i("UploaderService", "response:" + response.getStatusLine().toString());
            appendLog("response:" + response.getStatusLine().toString());
        } catch (ClientProtocolException e) {
            announceError("Sorry, error in transfer.", sem_video.getId());
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
        } catch (IOException e) {
            announceError("Sorry, couldn't connect to server.", sem_video.getId());
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
        } catch (IllegalStateException e) {
            announceError("Bad or missing server name.", sem_video.getId());
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void uploadVideoToNowhere(SemanticVideo sem_video) {
        final LocalBroadcastManager broadcast_manager = LocalBroadcastManager.getInstance(this);
        final Context context = this;

        //give an unique name to the video and xml files stored on server
        String filename = sem_video.getKey();
        if (filename == null || filename.isEmpty()) {
            filename = sem_video.createKey();
            VideoDBHelper vdb = new VideoDBHelper(context);
            vdb.update(sem_video);
            vdb.close();
        }
        Long traffic_id = sem_video.getId();
        File file = new File(sem_video.getUri().getPath());
        FileEntity fe = new FileEntity(file, "binary/octet-stream");
        fe.setChunked(true);
        PollableHttpEntity broadcasting_entity = enableProgressBroadcasting(fe, traffic_id);
        sem_video.setUploadStatus(SemanticVideo.UPLOADING);
        appendLog(String.format("Pretending to upload video %d as file %s",
                sem_video.getId(), filename));

        Log.i("UploaderService", "Pretending to send video file " + sem_video.getId());
        Log.i("UploaderService", "response is ok! Move on.");
        broadcasting_entity.getContentLength();
        Intent endIntent = new Intent();
        endIntent.setAction(VideoBrowserActivity.UploaderBroadcastReceiver.UPLOAD_END_ACTION);
        endIntent.addCategory(Intent.CATEGORY_DEFAULT);
        endIntent.putExtra(PARAM_OUT, traffic_id);
        endIntent.putExtra(PARAM_WHAT, UPLOAD_END);
        broadcast_manager.sendBroadcast(endIntent);
    }



    private void uploadVideoToAaltoTestService(SemanticVideo sem_video) {
        // This is new video_uploader using the i5Cloud services
        // Now implemented a simple stub that sends video file as a PUT to some url.

        // prepare file for sending
        Long traffic_id = sem_video.getId();
        String url = "http://www.example.com/resource"; // replace this with something real
        String token = ""; // replace this with something real
        HttpClient client = new DefaultHttpClient();
        HttpPut put= new HttpPut(url);
        File file = new File(sem_video.getUri().getPath());
        FileEntity fe = new FileEntity(file, "binary/octet-stream");
        fe.setChunked(true);
        PollableHttpEntity broadcasting_entity = enableProgressBroadcasting(fe, traffic_id);
        put.setEntity(broadcasting_entity);

        put.setHeader("X-Auth-Token", token);
        put.setHeader("Content-type", "application/x-www-form-urlencoded");
        try {
            Log.i("UploaderService", "sending PUT to " + url);
            HttpResponse response = client.execute(put);
            Log.i("UploaderService", "response:" + response.getStatusLine().toString());
            appendLog("response:" + response.getStatusLine().toString());
        } catch (ClientProtocolException e) {
            announceError("Sorry, error in transfer.", traffic_id);
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
        } catch (IOException e) {
            announceError("Sorry, couldn't connect to server.", traffic_id);
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
        } catch (IllegalStateException e) {
            announceError("Bad or missing server name.", traffic_id);
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
        }
        // hmm, what to do with the response?
    }


    private void uploadMetadataToClViTra2Service(SemanticVideo sem_video) {
        Log.i("UploaderService", "uploadMetadataToClViTra2Service -- not implemented");
    }

    private void uploadMetadataToClViTra(SemanticVideo sem_video) {
        Log.i("UploaderService", "uploadMetadataToClViTra -- not implemented");

    }

    private void uploadMetadataToAaltoTestService(SemanticVideo sem_video) {
        // This is new video_uploader using the i5Cloud services
        // Now implemented a simple stub that sends video file as a PUT to some url.

        // prepare file for sending
        Long traffic_id = sem_video.getId();
        String url = "http://achso.aalto.fi/server/upload_data"; // replace this with something real
        String token = ""; // replace this with something real
        HttpClient client = new DefaultHttpClient();
        HttpPost post= new HttpPost(url);
        String key = sem_video.getKey();
        if (key == null || key.isEmpty()) {
            key = sem_video.createKey();
            VideoDBHelper vdb = new VideoDBHelper(this);
            vdb.update(sem_video);
            vdb.close();
        }
        String json = sem_video.json_dump().toString();
        Log.i("UploaderService", json);

        StringEntity se = null;
        try {
            se = new StringEntity(json, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        post.setEntity(se);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");

        try {
            Log.i("UploaderService", "sending POST to " + url);
            HttpResponse response = client.execute(post);
            Log.i("UploaderService", "response:" + response.getStatusLine().toString());
            appendLog("response:" + response.getStatusLine().toString());
        } catch (ClientProtocolException e) {
            announceError("Sorry, error in transfer.", traffic_id);
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
        } catch (IOException e) {
            announceError("Sorry, couldn't connect to server.", traffic_id);
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
        } catch (IllegalStateException e) {
            announceError("Bad or missing server name.", traffic_id);
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
        }
        // hmm, what to do with the response?
    }


}


