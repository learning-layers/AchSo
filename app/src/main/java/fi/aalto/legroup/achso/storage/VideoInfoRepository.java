package fi.aalto.legroup.achso.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.OptimizedVideo;

/**
 * Provides cached read-only access to video information objects.
 */
public interface VideoInfoRepository {

    /**
     * Result type of doing queries for videos. In most cases the last modify date is easily
     * obtained when iterating all the videos and it almost always used immediately afterwards so
     * keep them stored together with the ids.
     */
    class FindResult {

        /**
         * @param id           ID of the video manifest.
         * @param lastModified UNIX timestamp of when the video manifest has been modified.
         */
        public FindResult(UUID id, long lastModified) {
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

    /**
     * Returns all the videos in the repository.
     */
    public Collection<OptimizedVideo> getAll() throws IOException;

    /**
     * Returns an information object describing an entity with the given ID.
     */
    public OptimizedVideo getVideo(UUID id) throws IOException;
}
