package fi.aalto.legroup.achso.upload.video;

import fi.aalto.legroup.achso.database.SemanticVideo;

/**
 * Uploads a video to a specific service.
 *
 * @author Leo Nikkilä
 */
public abstract class AbstractVideoUploader {

    protected UploadListener uploadListener;

    /**
     * Sets the upload listener that will be notified of the upload status.
     *
     * @param uploadListener a listener that will be notified of the upload status
     */
    public void setUploadListener(UploadListener uploadListener) {
        this.uploadListener = uploadListener;
    }

    /**
     * Uploads the video. Must call uploadListener.onVideoUploadStart() when the upload starts,
     * onVideoUploadProgress() when the upload progresses, onVideoUploadFinish() when done and
     * onVideoUploadError() if an error occurs.
     *
     * @param video the video that will be uploaded
     */
    public abstract void upload(SemanticVideo video);

    /**
     * A listener that will be notified of the upload status.
     */
    public interface UploadListener {

        /**
         * Fired when the upload starts.
         *
         * @param video the video that is being uploaded
         */
        public void onVideoUploadStart(SemanticVideo video);

        /**
         * Fired when the upload progresses.
         *
         * @param video   the video that is being uploaded
         * @param percent upload progress as a percent value
         */
        public void onVideoUploadProgress(SemanticVideo video, int percent);

        /**
         * Fired when the upload finishes.
         *
         * @param video the video that was uploaded
         */
        public void onVideoUploadFinish(SemanticVideo video);

        /**
         * Fired when an error occurs during the upload process.
         *
         * @param video the video that was being uploaded
         * @param errorMessage a message describing the error
         */
        public void onVideoUploadError(SemanticVideo video, String errorMessage);

    }

}
