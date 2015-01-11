package fi.aalto.legroup.achso.upload;

import java.util.UUID;

/**
 * @author Leo Nikkilä
 */
public class UploadErrorEvent {

    private Uploader uploader;
    private UUID videoId;
    private String errorMessage;

    public UploadErrorEvent(Uploader uploader, UUID videoId, String errorMessage) {
        this.uploader = uploader;
        this.videoId = videoId;
        this.errorMessage = errorMessage;
    }

    public Uploader getUploader() {
        return this.uploader;
    }

    public UUID getVideoId() {
        return this.videoId;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

}
