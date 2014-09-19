package fi.aalto.legroup.achso.upload.metadata;

import fi.aalto.legroup.achso.database.SemanticVideo;

/**
 * Uploads metadata from videos to a specific service.
 *
 * @author Leo Nikkilä
 */
public abstract class AbstractMetadataUploader {

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
     * Uploads the metadata of a video. Must call uploadListener.onMetadataUploadStart() when the
     * upload starts, onMetadataUploadFinish() when done and onMetadataUploadError() if an error
     * occurs.
     *
     * @param video the video whose metadata will be uploaded
     */
    public abstract void upload(SemanticVideo video);

    /**
     * A listener that will be notified of the upload status.
     */
    public interface UploadListener {

        /**
         * Fired when the upload starts.
         *
         * @param video the video whose metadata is being uploaded
         */
        public void onMetadataUploadStart(SemanticVideo video);

        /**
         * Fired when the upload finishes.
         *
         * @param video the video whose metadata was uploaded
         */
        public void onMetadataUploadFinish(SemanticVideo video);

        /**
         * Fired when an error occurs during the upload process.
         *
         * @param video the video whose metadata was being uploaded
         * @param errorMessage a message describing the error
         */
        public void onMetadataUploadError(SemanticVideo video, String errorMessage);

    }

}
