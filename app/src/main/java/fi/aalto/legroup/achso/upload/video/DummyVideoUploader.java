package fi.aalto.legroup.achso.upload.video;

import fi.aalto.legroup.achso.database.SemanticVideo;

/**
 * A video uploader that does nothing.
 *
 * @author Leo Nikkilä
 */
public final class DummyVideoUploader extends AbstractVideoUploader {

    /**
     * Uploads the video. Must call uploadListener.onVideoUploadStart() when the upload starts,
     * onVideoUploadProgress() when the upload progresses, onVideoUploadFinish() when done and
     * onVideoUploadError() if an error occurs.
     *
     * @param video the video that will be uploaded
     */
    @Override
    public void upload(SemanticVideo video) {
        uploadListener.onVideoUploadStart(video);
        uploadListener.onVideoUploadProgress(video, 100);
        uploadListener.onVideoUploadFinish(video);
    }

}
