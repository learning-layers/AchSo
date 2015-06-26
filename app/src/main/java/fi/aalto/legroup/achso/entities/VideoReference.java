package fi.aalto.legroup.achso.entities;

import java.util.UUID;

public class VideoReference {

    /**
     * @param id           ID of the video manifest.
     * @param lastModified UNIX timestamp of when the video manifest has been modified.
     */
    public VideoReference(UUID id, long lastModified) {
        this.id = id;
        this.lastModified = lastModified;
    }

    private UUID id;
    private long lastModified;

    public UUID getId() {
        return id;
    }

    public long getLastModified() {
        return lastModified;
    }
}
