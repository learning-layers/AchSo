package fi.aalto.legroup.achso.upload;

import fi.aalto.legroup.achso.database.SemanticVideo;

/**
 * An uploader that does nothing.
 *
 * @author Leo Nikkilä
 */
public final class DummyUploader extends Uploader {

    /**
     * Uploads the data of a video. Must call listener.onUploadStart() when the upload starts,
     * onUploadFinish() when done and onMetadataUploadError() if an error occurs. Calling
     * onUploadProgress() is optional.
     *
     * @param video the video whose metadata will be uploaded
     */
    @Override
    public void upload(SemanticVideo video) {
        listener.onUploadStart(video);
        listener.onUploadFinish(video);
    }

}
