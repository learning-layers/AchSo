package fi.aalto.legroup.achso.upload;

import fi.aalto.legroup.achso.database.SemanticVideo;

/**
 * @author Leo Nikkilä
 */
public class DisabledUploader extends Uploader {

    /**
     * Uploads the data of a video. Must call listener.onUploadStart() when the upload starts,
     * onUploadFinish() when done and onUploadError() if an error occurs. Calling onUploadProgress()
     * is optional.
     *
     * @param video the video whose data will be uploaded
     */
    @Override
    public void upload(SemanticVideo video) {
        String error = "Uploading is temporarily disabled; will be enabled in a future version.";
        listener.onUploadError(video, error);
    }

}
