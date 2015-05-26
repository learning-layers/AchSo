package fi.aalto.legroup.achso.storage;

import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoInfo;

public abstract class AbstractVideoRepository implements VideoInfoRepository, VideoRepository {

    protected Bus bus;

    public AbstractVideoRepository(Bus bus) {
        this.bus = bus;
    }

    /**
     * Compares results based on their modification dates.
     */
    protected static final class FindResultComparator implements Comparator<FindResult> {

        @Override
        public int compare(FindResult left, FindResult right) {
            // Room for improvement: this quick and dirty implementation calls File.lastModified()
            // even if the value has previously been retrieved for that file.

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

    /**
     * Returns a list of all available video IDs and their modification dates.
     */
    public abstract List<FindResult> getAll() throws IOException;

    /**
     * Get the time when a video with given ID has been last modified.
     */
    public abstract long getLastModifiedTime(UUID id) throws IOException;

    /**
     * Returns the video info for a given ID.
     */
    public abstract VideoInfo getVideoInfo(UUID id) throws IOException;

    /**
     * Returns the video for a given ID.
     */
    public abstract Video getVideo(UUID id) throws IOException;

    /**
     * Saves a video to the repository.
     */
    public abstract void save(Video video) throws IOException;

    /**
     * Deletes a video from the repository.
     */
    public abstract void delete(UUID id) throws IOException;

    /**
     * Invalidates the cached information object with the given ID.
     */
    public void invalidate(UUID id) {
    }

    /**
     * Invalidates the entire cache.
     */
    public void invalidateAll() {
    }

    /**
     * Returns a list of all available video IDs sorted by descending modification date.
     */
    public List<FindResult> getAllSorted() throws IOException {

        List<FindResult> results = getAll();
        Collections.sort(results, new FindResultComparator());

        return results;
    }

    /**
     * Returns a list of all available video IDs that match the genre string
     * and their modification dates.
     */
    public List<FindResult> getByGenreString(String genre) throws IOException {

        List<FindResult> results = getAll();
        ArrayList<FindResult> matching = new ArrayList<>(results.size());

        for (FindResult result : results) {
            if (getVideoInfo(result.getId()).getGenre().matches(genre)) {
                matching.add(result);
            }
        }
        matching.trimToSize();

        return matching;
    }

    /**
     * Returns a list of all available video IDs that match the genre string
     * sorted by descending modification date.
     */
    public List<FindResult> getByGenreStringSorted(String genre) throws IOException {

        List<FindResult> results = getByGenreString(genre);
        Collections.sort(results, new FindResultComparator());

        return results;
    }
}

