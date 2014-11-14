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
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import fi.aalto.legroup.achso.activity.MainActivity;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.upload.Uploader;
import fi.aalto.legroup.achso.util.App;

/**
 * FIXME: Video and metadata are uploaded independently of each other.
 *        If one of them fails, we should undo the other, maybe?
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

    public static final String UPLOAD_START_ACTION = "fi.aalto.legroup.achso.intent.action.UPLOAD_START";
    public static final String UPLOAD_PROGRESS_ACTION = "fi.aalto.legroup.achso.intent.action.UPLOAD_PROGRESS";
    public static final String UPLOAD_END_ACTION = "fi.aalto.legroup.achso.intent.action.UPLOAD_END";
    public static final String UPLOAD_ERROR_ACTION = "fi.aalto.legroup.achso.intent.action.UPLOAD_ERROR";
    public static final String UPLOAD_FINALIZED_ACTION = "fi.aalto.legroup.achso.intent" +
            ".action.UPLOAD_FINALIZED";

    private LocalBroadcastManager broadcastManager;

    public UploaderService() {
        super("AchSoUploaderService");
        broadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long id = intent.getLongExtra(PARAM_IN, -1);

        if (id != -1) {
            SemanticVideo video = VideoDBHelper.getById(id);

            App.metadataUploader.setListener(new MetadataUploaderListener());
            App.metadataUploader.upload(video);

            App.videoUploader.setListener(new VideoUploaderListener());
            App.videoUploader.upload(video);
        }
    }

    protected void broadcastError(SemanticVideo video, String errorMessage) {
        Intent intent = new Intent();

        intent.setAction(UPLOAD_ERROR_ACTION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(PARAM_OUT, video.getId());
        intent.putExtra(PARAM_WHAT, UPLOAD_ERROR);
        intent.putExtra(PARAM_ARG, errorMessage);

        broadcastManager.sendBroadcast(intent);
    }

    protected class VideoUploaderListener implements Uploader.Listener {

        /**
         * Fired when the upload starts.
         *
         * @param video video whose data is being uploaded
         */
        @Override
        public void onUploadStart(SemanticVideo video) {
            Intent intent = new Intent();

            intent.setAction(UPLOAD_START_ACTION);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.putExtra(PARAM_OUT, video.getId());
            intent.putExtra(PARAM_WHAT, UPLOAD_START);

            broadcastManager.sendBroadcast(intent);
        }

        /**
         * Fired when the upload progresses. This is optional, and the listener should assume that
         * the progress is indeterminate unless this is called.
         *
         * @param video      video whose data is being uploaded
         * @param percentage percentage of data uploaded
         */
        @Override
        public void onUploadProgress(SemanticVideo video, int percentage) {
            Intent intent = new Intent(UPLOAD_PROGRESS_ACTION);

            intent.putExtra(PARAM_OUT, video.getId());
            intent.putExtra(PARAM_WHAT, UPLOAD_PROGRESS);
            intent.putExtra(PARAM_ARG, percentage);

            LocalBroadcastManager.getInstance(UploaderService.this).sendBroadcast(intent);
            broadcastManager.sendBroadcast(intent);
        }

        /**
         * Fired when the upload finishes.
         *
         * @param video video whose data was uploaded
         */
        @Override
        public void onUploadFinish(SemanticVideo video) {
            Intent intent = new Intent(UPLOAD_END_ACTION);

            intent.putExtra(PARAM_OUT, video.getId());
            intent.putExtra(PARAM_WHAT, UPLOAD_END);

            broadcastManager.sendBroadcast(intent);
        }

        /**
         * Fired if an error occurs during the upload process.
         *
         * @param video        video whose data was being uploaded
         * @param errorMessage message describing the error
         */
        @Override
        public void onUploadError(SemanticVideo video, String errorMessage) {
            broadcastError(video, errorMessage);
        }

    }

    protected class MetadataUploaderListener implements Uploader.Listener {

        /**
         * Fired if an error occurs during the upload process.
         *
         * @param video        video whose data was being uploaded
         * @param errorMessage message describing the error
         */
        @Override
        public void onUploadError(SemanticVideo video, String errorMessage) {
            broadcastError(video, errorMessage);
        }

        /*
         * The following events are not used by this uploader.
         */

        @Override
        public void onUploadStart(SemanticVideo video) {}

        @Override
        public void onUploadProgress(SemanticVideo video, int percentage) {}

        @Override
        public void onUploadFinish(SemanticVideo video) {}

    }

}


