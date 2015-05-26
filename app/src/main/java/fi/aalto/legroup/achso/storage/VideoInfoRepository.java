package fi.aalto.legroup.achso.storage;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.VideoInfo;

/**
 * Provides cached read-only access to video information objects.
 */
public interface VideoInfoRepository {

    class FindResult {

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

        public static List<UUID> toIds(List<FindResult> results) {

            List<UUID> ids = new ArrayList<>(results.size());

            for (FindResult result : results) {
                ids.add(result.getId());
            }
            return ids;
        }
    }

    /**
     * Returns a list of all entity IDs with times.
     */
    public List<FindResult> getAll() throws IOException;

    /**
     * Returns a list of all entity IDs sorted descending by modify date.
     */
    public List<FindResult> getAllSorted() throws IOException;

    /**
     * Returns a list of all available video IDs that match the genre string
     * and their modification dates.
     */
    public List<FindResult> getByGenreString(String genre) throws IOException;

    /**
     * Returns a list of all available video IDs that match the genre string
     * sorted by descending modification date.
     */
    public List<FindResult> getByGenreStringSorted(String genre) throws IOException;

    /**
     * Get the time when a video with given ID has been last modified.
     */
    public abstract long getLastModifiedTime(UUID id) throws IOException;

    /**
     * Returns an information object describing an entity with the given ID.
     */
    public VideoInfo getVideoInfo(UUID id) throws IOException;

    /**
     * Invalidates the cached information object with the given ID.
     */
    public void invalidate(UUID id);

    /**
     * Invalidates the entire cache.
     */
    public void invalidateAll();
}
