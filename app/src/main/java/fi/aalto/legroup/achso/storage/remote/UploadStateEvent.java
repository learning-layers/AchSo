package fi.aalto.legroup.achso.storage.remote;

import java.util.UUID;

public class UploadStateEvent {

    private UUID videoId;
    private Type type;

    public UploadStateEvent(UUID videoId, Type type) {
        this.videoId = videoId;
        this.type = type;
    }

    public UUID getVideoId() {
        return this.videoId;
    }

    public Type getType() {
        return this.type;
    }

    public static enum Type {
        STARTED,
        SUCCEEDED,
        FAILED,
    }

}
