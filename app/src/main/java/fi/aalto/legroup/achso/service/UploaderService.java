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

import fi.aalto.legroup.achso.activity.VideoBrowserActivity;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.upload.metadata.AbstractMetadataUploader;
import fi.aalto.legroup.achso.upload.video.AbstractVideoUploader;
import fi.aalto.legroup.achso.util.App;

/**
 * FIXME: Video and metadata are uploaded independently of each other.
 *        If one of them fails, we should undo the other, maybe?
 *
 * TODO: Make uploaders extend a parent class so that we can have a single set of listener methods?
 */
public class UploaderService extends IntentService implements AbstractVideoUploader.UploadListener,
        AbstractMetadataUploader.UploadListener {

    public static final String PARAM_IN = "in";
    public static final String PARAM_OUT = "out";
    public static final String PARAM_WHAT = "what";
    public static final String PARAM_ARG = "arg";

    public static final int UPLOAD_START = 0;
    public static final int UPLOAD_PROGRESS = 1;
    public static final int UPLOAD_END = 2;
    public static final int UPLOAD_ERROR = 3;

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

            App.metadataUploader.setUploadListener(this);
            App.metadataUploader.upload(video);

            App.videoUploader.setUploadListener(this);
            App.videoUploader.upload(video);
        }
    }

    /**
     * Fired when the upload starts.
     *
     * @param video the video that is being uploaded
     */
    @Override
    public void onVideoUploadStart(SemanticVideo video) {
        Intent intent = new Intent();

        intent.setAction(VideoBrowserActivity.UploaderBroadcastReceiver.UPLOAD_START_ACTION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(PARAM_OUT, video.getId());
        intent.putExtra(PARAM_WHAT, UPLOAD_START);

        broadcastManager.sendBroadcast(intent);
    }

    /**
     * Fired when the upload progresses.
     *
     * @param video   the video that is being uploaded
     * @param percent upload progress as a percent value
     */
    @Override
    public void onVideoUploadProgress(SemanticVideo video, int percent) {
        Intent intent = new Intent();

        intent.setAction(VideoBrowserActivity.UploaderBroadcastReceiver.UPLOAD_PROGRESS_ACTION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(PARAM_OUT, video.getId());
        intent.putExtra(PARAM_WHAT, UPLOAD_PROGRESS);
        intent.putExtra(PARAM_ARG, percent);

        broadcastManager.sendBroadcast(intent);
    }

    /**
     * Fired when the upload finishes.
     *
     * @param video the video that was uploaded
     */
    @Override
    public void onVideoUploadFinish(SemanticVideo video) {
        Intent intent = new Intent();

        intent.setAction(VideoBrowserActivity.UploaderBroadcastReceiver.UPLOAD_END_ACTION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(PARAM_OUT, video.getId());
        intent.putExtra(PARAM_WHAT, UPLOAD_END);

        broadcastManager.sendBroadcast(intent);
    }

    /**
     * Fired when an error occurs during the upload process.
     *
     * @param video        the video that was being uploaded
     * @param errorMessage a message describing the error
     */
    @Override
    public void onVideoUploadError(SemanticVideo video, String errorMessage) {
        broadcastError(video, errorMessage);
    }

    /**
     * Fired when the upload starts.
     *
     * @param video the video whose metadata is being uploaded
     */
    @Override
    public void onMetadataUploadStart(SemanticVideo video) {}

    /**
     * Fired when the upload finishes.
     *
     * @param video the video whose metadata was uploaded
     */
    @Override
    public void onMetadataUploadFinish(SemanticVideo video) {}

    /**
     * Fired when an error occurs during the upload process.
     *
     * @param video        the video whose metadata was being uploaded
     * @param errorMessage a message describing the error
     */
    @Override
    public void onMetadataUploadError(SemanticVideo video, String errorMessage) {
        broadcastError(video, errorMessage);
    }

    private void broadcastError(SemanticVideo video, String errorMessage) {
        Intent intent = new Intent();

        intent.setAction(VideoBrowserActivity.UploaderBroadcastReceiver.UPLOAD_ERROR_ACTION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(PARAM_OUT, video.getId());
        intent.putExtra(PARAM_WHAT, UPLOAD_ERROR);
        intent.putExtra(PARAM_ARG, errorMessage);

        broadcastManager.sendBroadcast(intent);
    }

}


