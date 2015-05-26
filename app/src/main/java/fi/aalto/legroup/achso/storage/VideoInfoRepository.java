package fi.aalto.legroup.achso.storage;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    }

    /**
     * Compares results based on their modification dates.
     */
    final class FindResultComparator implements Comparator<FindResult> {

        @Override
        public int compare(FindResult left, FindResult right) {
            long leftModified = left.getLastModified();
            long rightModified = right.getLastModified();

            if (leftModified > rightModified) {
                return 1;
            }
            if (leftModified < rightModified) {
                return -1;
            }

            return 0;
        }

    }
    class FindResults extends AbstractList<FindResult> {

        private List<FindResult> results;

        public FindResults(List<FindResult> results) {
            this.results = results;
        }

        @Override
        public FindResult get(int location) {
            return results.get(location);
        }

        @Override
        public int size() {
            return results.size();
        }

        @Override
        public void add(int location, FindResult object) {
            results.add(location, object);
        }

        @Override
        public FindResult remove(int location) {
            return results.remove(location);
        }

        @Override
        public FindResult set(int location, FindResult object) {
            return results.set(location, object);
        }

        public void sort() {
            Collections.sort(results, new FindResultComparator());
        }

        public List<UUID> getIDs() {

            List<UUID> ids = new ArrayList<>(size());
            for (FindResult result : results) {
                ids.add(result.getId());
            }
            return ids;
        }
    }

    /**
     * Returns a list of all entity IDs with times.
     */
    public FindResults getAll() throws IOException;

    /**
     * Returns a list of all entity IDs sorted descending by modify date.
     */
    public FindResults getAllSorted() throws IOException;

    /**
     * Returns a list of all available video IDs that match the genre string
     * and their modification dates.
     */
    public FindResults getByGenreString(String genre) throws IOException;

    /**
     * Returns a list of all available video IDs that match the genre string
     * sorted by descending modification date.
     */
    public FindResults getByGenreStringSorted(String genre) throws IOException;

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
