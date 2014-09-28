package fi.aalto.legroup.achso.upload;

import fi.aalto.legroup.achso.database.SemanticVideo;

/**
 * @author Leo Nikkilä
 */
public abstract class Uploader {

    protected Listener listener;

    /**
     * Sets the upload listener that will be notified of the upload status.
     *
     * @param listener a listener that will be notified of the upload status
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Uploads the data of a video. Must call listener.onUploadStart() when the upload starts,
     * onUploadFinish() when done and onUploadError() if an error occurs. Calling onUploadProgress()
     * is optional.
     *
     * @param video the video whose data will be uploaded
     */
    public abstract void upload(SemanticVideo video);

    /**
     * A listener that will be notified of the upload status.
     */
    public static interface Listener {

        /**
         * Fired when the upload starts.
         *
         * @param video video whose data is being uploaded
         */
        public void onUploadStart(SemanticVideo video);

        /**
         * Fired when the upload progresses. This is optional, and the listener should assume that
         * the progress is indeterminate unless this is called.
         *
         * @param video      video whose data is being uploaded
         * @param percentage percentage of data uploaded
         */
        public void onUploadProgress(SemanticVideo video, int percentage);

        /**
         * Fired when the upload finishes.
         *
         * @param video video whose data was uploaded
         */
        public void onUploadFinish(SemanticVideo video);

        /**
         * Fired if an error occurs during the upload process.
         *
         * @param video        video whose data was being uploaded
         * @param errorMessage message describing the error
         */
        public void onUploadError(SemanticVideo video, String errorMessage);

    }

}
