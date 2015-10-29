package fi.aalto.legroup.achso.entities;

import java.util.UUID;

public class VideoReference {

    /**
     * @param id       ID of the video manifest.
     * @param revision Latest revision number for the video.
     */
    public VideoReference(UUID id, int revision) {
        this.id = id;
        this.revision = revision;
    }

    private UUID id;
    private int revision;

    public UUID getId() {
        return id;
    }

    public long getRevision() {
        return revision;
    }
}
