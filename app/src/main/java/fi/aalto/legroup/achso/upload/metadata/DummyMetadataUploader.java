package fi.aalto.legroup.achso.upload.metadata;

import fi.aalto.legroup.achso.database.SemanticVideo;

/**
 * A metadata uploader that does nothing.
 *
 * @author Leo Nikkilä
 */
public final class DummyMetadataUploader extends AbstractMetadataUploader {

    /**
     * Uploads the metadata of a video. Must call uploadListener.onMetadataUploadStart() when the
     * upload starts, onMetadataUploadFinish() when done and onMetadataUploadError() if an error
     * occurs.
     *
     * @param video the video whose metadata will be uploaded
     */
    @Override
    public void upload(SemanticVideo video) {
        uploadListener.onMetadataUploadStart(video);
        uploadListener.onMetadataUploadFinish(video);
    }

}
