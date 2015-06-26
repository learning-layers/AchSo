package fi.aalto.legroup.achso.storage.remote;

import java.util.UUID;

public class UploadErrorEvent {

    private UUID videoId;
    private String errorMessage;

    public UploadErrorEvent(UUID videoId, String errorMessage) {
        this.videoId = videoId;
        this.errorMessage = errorMessage;
    }

    public UUID getVideoId() {
        return this.videoId;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

}
