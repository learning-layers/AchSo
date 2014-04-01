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

package fi.aalto.legroup.achso.upload;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.achso.activity.MainMenuActivity;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
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

    private long mFileSize;

    public UploaderService() {
        super("AchSoUploaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long id = intent.getLongExtra(PARAM_IN, -1);

        Log.i("UploaderService", "Received intent to upload");

        if (id != -1) {
            SemanticVideo sem_video = VideoDBHelper.getById(id);
            uploadToClViTra(sem_video);
        }
    }

    private void uploadToClViTra2Service(SemanticVideo sem_video) {
        // This is new uploader using the i5Cloud services


    }

    private void uploadToClViTra(SemanticVideo sem_video) {
        final LocalBroadcastManager broadcast_manager = LocalBroadcastManager.getInstance(this);
        final Context context = this;

        String server_url = "http://merian.informatik.rwth-aachen.de:5080/ClViTra/FileUploadServlet";
        Intent startIntent = new Intent();
        startIntent.setAction(MainMenuActivity.UploaderBroadcastReceiver.UPLOAD_START_ACTION);
        startIntent.addCategory(Intent.CATEGORY_DEFAULT);
        startIntent.putExtra(PARAM_OUT, sem_video.getId());
        startIntent.putExtra(PARAM_WHAT, UPLOAD_START);
        broadcast_manager.sendBroadcast(startIntent);
        //give an unique name to the video and xml files stored on server
        String filename = new String(UUID.randomUUID() + "");
        sem_video.setUploading(true);
        sem_video.setUploaded(false);
        sem_video.setUploadPending(false);
        HttpClient httpclient = new DefaultHttpClient();
        //added source and file id to the http post
        HttpPost httppost = new HttpPost(server_url + "?source=achso&uid=" + filename);
        LasConnection las = LasConnection.getConnection();
        String username = las.getClient().getUser();
        String pass = new String(Base64.encode(las.getPass().getBytes(), 0));
        appendLog(String.format("Uploading video %d to %s as file %s", sem_video.getId(), server_url, filename));
        //String pass = las.getSessionId();
        String userData = username + ":" + pass;

        sem_video.setCreator(userData);

        MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
        multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        multipartEntity.addPart(userData, new FileBody(new File(sem_video.getUri().getPath())));
        //multipartEntity.addTextBody("source", "achso");
        multipartEntity.addBinaryBody("xml", XmlConverter.toXML(context, sem_video));

        PollableHttpEntity pollable_entity = new PollableHttpEntity(multipartEntity.build(), new PollableHttpEntity.ProgressListener() {
            @Override
            public void transferred(long bytes, SemanticVideo sem_video) {
                Intent progressIntent = new Intent();
                progressIntent.setAction(MainMenuActivity.UploaderBroadcastReceiver.UPLOAD_PROGRESS_ACTION);
                progressIntent.addCategory(Intent.CATEGORY_DEFAULT);
                progressIntent.putExtra(PARAM_OUT, sem_video.getId());
                progressIntent.putExtra(PARAM_WHAT, UPLOAD_PROGRESS);
                int percentage = (int) (((double) bytes / mFileSize) * 100.0);
                progressIntent.putExtra(PARAM_ARG, percentage);
                broadcast_manager.sendBroadcast(progressIntent);

                if (percentage == 100) {
                    Intent endIntent = new Intent();
                    endIntent.setAction(MainMenuActivity.UploaderBroadcastReceiver.UPLOAD_END_ACTION);
                    endIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    endIntent.putExtra(PARAM_OUT, sem_video.getId());
                    endIntent.putExtra(PARAM_WHAT, UPLOAD_END);
                    broadcast_manager.sendBroadcast(endIntent);
                }
            }
        }, sem_video);
        httppost.setEntity(pollable_entity);
        mFileSize = pollable_entity.getContentLength();
        boolean success = false;
        String errmsg = null;
        try {
            Log.i("UploaderService", "sending POST to " + server_url);
            HttpResponse response = httpclient.execute(httppost);
            Log.i("UploaderService", "response:" + response.getStatusLine().toString());
            appendLog("response:" + response.getStatusLine().toString());
            success = true;
        } catch (ClientProtocolException e) {
            errmsg = "Sorry, error in transfer.";
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
        } catch (IOException e) {
            errmsg = "Sorry, couldn't connect to server.";
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
        } catch (IllegalStateException e) {
            errmsg = "Bad or missing server name.";
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
        }

        if (!success) {
            Intent endIntent = new Intent();
            endIntent.setAction(MainMenuActivity.UploaderBroadcastReceiver.UPLOAD_ERROR_ACTION);
            endIntent.addCategory(Intent.CATEGORY_DEFAULT);
            endIntent.putExtra(PARAM_OUT, sem_video.getId());
            endIntent.putExtra(PARAM_WHAT, UPLOAD_ERROR);
            endIntent.putExtra(PARAM_ARG, errmsg);
            broadcast_manager.sendBroadcast(endIntent);
        }
    }
}


