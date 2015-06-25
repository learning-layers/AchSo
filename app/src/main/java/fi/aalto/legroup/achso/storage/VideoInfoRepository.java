package fi.aalto.legroup.achso.storage;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
     * Compares results based on their modification dates.
     */
    final class FindResultComparator implements Comparator<FindResult> {

        @Override
        public int compare(FindResult left, FindResult right) {
            long leftModified = left.getLastModified();
            long rightModified = right.getLastModified();

            // Compare ascending date for default
            if (leftModified > rightModified) {
                return 1;
            }
            if (leftModified < rightModified) {
                return -1;
            }

            return 0;
        }

    }
    public static class FindResults extends AbstractList<FindResult> {

        private List<FindResult> results;

        /**
         * Create a new view into results with some useful methods
         * @param results A reference to a list to use internally (NOTE: Not copied)
         */
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

        /**
         * Sort the results from old to new
         * NOTE: Modifies the underlying collection and returns self for performance! Clone first if
         * you want to get a sorted version without sorting the original collection.
         */
        public FindResults sortAscending() {
            Collections.sort(results, new FindResultComparator());
            return this;
        }

        /**
         * Sort the results from new to old
         * NOTE: Modifies the underlying collection and returns self for performance! Clone first if
         * you want to get a sorted version without sorting the original collection.
         */
        public FindResults sortDescending() {
            Collections.sort(results, Collections.reverseOrder(new FindResultComparator()));
            return this;
        }

        /**
         * Get rid of the time data and return only the IDs.
         */
        public List<UUID> getIDs() {

            List<UUID> ids = new ArrayList<>(size());
            for (FindResult result : results) {
                ids.add(result.getId());
            }
            return ids;
        }
    }

    /**
     * Returns all the videos in the repository.
     */
    public List<OptimizedVideo> getAll() throws IOException;

    /**
     * Returns an information object describing an entity with the given ID.
     */
    public OptimizedVideo getVideo(UUID id) throws IOException;
}
