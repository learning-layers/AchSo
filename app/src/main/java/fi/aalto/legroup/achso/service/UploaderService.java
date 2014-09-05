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

package fi.aalto.legroup.achso.service;

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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import fi.aalto.legroup.achso.activity.VideoBrowserActivity;
import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.upload.PollableHttpEntity;
import fi.aalto.legroup.achso.util.AaltoConnection;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.LasConnection;
import fi.aalto.legroup.achso.util.xml.XmlConverter;

import static fi.aalto.legroup.achso.util.App.addPollingReminder;
import static fi.aalto.legroup.achso.util.App.appendLog;
import static fi.aalto.legroup.achso.util.App.doPendingPolls;

/**
 * TODO: Production URLs should be moved to secrets.xml.
 */
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
            boolean metadata_success = false;
            boolean video_success = false;
            switch (App.metadata_uploader) {
                case App.CLVITRA2:
                    metadata_success = uploadMetadataToClViTra2Service(sem_video);
                    break;
                case App.CLVITRA:
                    metadata_success = uploadMetadataToClViTra(sem_video);
                    break;
                case App.AALTO_TEST_SERVER:
                    metadata_success = uploadMetadataToAaltoTestService(sem_video);
                    break;
            }
            if (metadata_success) {
                switch (App.video_uploader) {
                    case App.CLVITRA2:
                        video_success = uploadVideoToClViTra2(sem_video);
                        break;
                    case App.CLVITRA:
                        video_success = uploadVideoToClViTra(sem_video);
                        break;
                    case App.AALTO_TEST_SERVER:
                        video_success = uploadVideoToAaltoTestService(sem_video);
                        break;
                    case App.DEV_NULL:
                        video_success = uploadVideoToNowhere(sem_video);
                }
                if (video_success) {
                    // start polling... maybe every 10s
                    Log.i("UploaderService", "Launching polling intent");
                    addPollingReminder(sem_video.getKey(), "testuser");
                    doPendingPolls();
                }
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


    private boolean uploadVideoToClViTra2(SemanticVideo sem_video) {
        // This is new video_uploader using the i5Cloud services
        // Now implemented a simple stub that sends video file as a PUT to some url.

        // prepare file for sending
        Long traffic_id = sem_video.getId();
        String url = "http://137.226.58.27:9080/ClViTra_2.0/rest/upload";
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);
        String file_path = sem_video.getUri().getPath();
        String suffix = file_path.substring(file_path.lastIndexOf("."));
        String file_name = sem_video.getKey() + suffix;
        File file = new File(file_path);

        MultipartEntityBuilder multipart= MultipartEntityBuilder.create();
        multipart.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        multipart.addBinaryBody("file", file, ContentType.DEFAULT_BINARY,
                file_name);
        PollableHttpEntity broadcasting_entity = enableProgressBroadcasting(multipart.build(),
                traffic_id);
        post.setEntity(broadcasting_entity);

        // String token = ""; // replace this with something real
        //post.setHeader("X-Auth-Token", token);
        //post.setHeader("User", App.login_state.getUser());
        post.setHeader("User", "testuser");
        try {
            Log.i("UploaderService", "sending POST to " + url);
            HttpResponse response = client.execute(post);
            Log.i("UploaderService", "response:" + response.getStatusLine().toString());
            appendLog("response:" + response.getStatusLine().toString());
            return true;
        } catch (ClientProtocolException e) {
            announceError("Sorry, error in transfer.", traffic_id);
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
            return false;
        } catch (IOException e) {
            announceError("Sorry, couldn't connect to server.", traffic_id);
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            announceError("Bad or missing server name.", traffic_id);
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean uploadVideoToClViTra(SemanticVideo sem_video) {
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
        String file_path = sem_video.getUri().getPath();
        String suffix = file_path.substring(file_path.lastIndexOf("."));
        String file_name = sem_video.getKey() + suffix;
        multipartEntity.addPart(userData, new FileBody(new File(file_path),
                ContentType.DEFAULT_BINARY, file_name));
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
            return true;
        } catch (ClientProtocolException e) {
            announceError("Sorry, error in transfer.", sem_video.getId());
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
            return false;
        } catch (IOException e) {
            announceError("Sorry, couldn't connect to server.", sem_video.getId());
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            announceError("Bad or missing server name.", sem_video.getId());
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean uploadVideoToNowhere(SemanticVideo sem_video) {
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
        return true;
    }



    private boolean uploadVideoToAaltoTestService(SemanticVideo sem_video) {
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
            return true;
        } catch (ClientProtocolException e) {
            announceError("Sorry, error in transfer.", traffic_id);
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
            return false;
        } catch (IOException e) {
            announceError("Sorry, couldn't connect to server.", traffic_id);
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            announceError("Bad or missing server name.", traffic_id);
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
        // hmm, what to do with the response?
    }


    private boolean uploadMetadataToClViTra2Service(SemanticVideo sem_video) {
        Log.i("UploaderService", "uploadMetadataToClViTra2Service -- not implemented");
        return false;
    }

    private boolean uploadMetadataToClViTra(SemanticVideo sem_video) {
        Log.i("UploaderService", "uploadMetadataToClViTra -- not implemented");
        return false;
    }

    private String getUniqueKeyFromAaltoTestService(SemanticVideo sem_video) {
        String key = "";
        Long traffic_id = sem_video.getId();
        String url = AaltoConnection.API_URL + "get_unique_id";
        HttpGet get = new HttpGet(url);

        Log.i("UploaderService", "Asking new key for video:" + traffic_id);

        key = getResponseString(get, traffic_id);

        return key;
    }

    private String getResponseString(HttpGet get, Long traffic_id) {
        String response_string = "";
        HttpClient client = new DefaultHttpClient();
        try {
            HttpResponse response = client.execute(get);
            HttpEntity responseEntity = response.getEntity();
            if(responseEntity!=null) {
                response_string = EntityUtils.toString(responseEntity);
            }
            Log.i("UploaderService", "received response: ("+response.getStatusLine()
                            .getStatusCode() + ") " + response_string);
            appendLog("response:" + response_string);
            if (response.getStatusLine().getStatusCode() != 200) {
                response_string = "";
            }
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
        return response_string;
    }

    private boolean uploadMetadataToAaltoTestService(SemanticVideo sem_video) {
        Long traffic_id = sem_video.getId();
        String url = "http://achso.aalto.fi/server/api/upload_video_metadata"; // replace this with something
        // real
        //String token = ""; // replace this with something real
        HttpClient client = new DefaultHttpClient();
        HttpPost post= new HttpPost(url);
        String key = sem_video.getKey();
        if (key == null || key.isEmpty() || key.length() > 64) {
            key = getUniqueKeyFromAaltoTestService(sem_video);
            if (key == null || key.length()==0) {
                Log.i("UploaderService", "Failed to get an unique key for video -- cancel upload");
                return false;
            }
            VideoDBHelper vdb = new VideoDBHelper(this);
            sem_video.setKey(key);
            List<Annotation> annotations = vdb.getAnnotationsById(traffic_id);
            for (Annotation annotation: annotations) {
                Log.i("UploaderService", "Setting annotation ( "+ annotation.getVideoKey() + " ) to use new key: " + key);
                annotation.setVideoKey(key);
                Log.i("UploaderService", "Now it is "+ annotation.getVideoKey());
                vdb.update(annotation);
            }
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
            return true;
        } catch (ClientProtocolException e) {
            announceError("Sorry, error in transfer.", traffic_id);
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
            return false;
        } catch (IOException e) {
            announceError("Sorry, couldn't connect to server.", traffic_id);
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            announceError("Bad or missing server name.", traffic_id);
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    static boolean updateVideoMetadataToAalto(SemanticVideo sem_video, List<String> fields) {
        Long traffic_id = sem_video.getId();
        String url = "http://achso.aalto.fi/server/api/update_video_metadata"; // replace this with
        // something
        // real
        //String token = ""; // replace this with something real
        HttpClient client = new DefaultHttpClient();
        HttpPost post= new HttpPost(url);
        String key = sem_video.getKey();
        String json = sem_video.json_dump(fields).toString();
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
            return true;
        } catch (ClientProtocolException e) {
            Log.i("UploaderService", "ClientProtocolException caught");
            appendLog("ClientProtocolException caught");
            return false;
        } catch (IOException e) {
            Log.i("UploaderService", "IOException caught:" + e.getMessage());
            appendLog("IOException caught:" + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            Log.i("UploaderService", "IllegalStateException caught:" + e.getMessage());
            appendLog("IllegalStateException caught:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


}


