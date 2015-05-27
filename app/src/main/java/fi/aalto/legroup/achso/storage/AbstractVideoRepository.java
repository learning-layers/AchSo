package fi.aalto.legroup.achso.storage;

import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoInfo;

public abstract class AbstractVideoRepository implements VideoInfoRepository, VideoRepository {

    protected Bus bus;

    public AbstractVideoRepository(Bus bus) {
        this.bus = bus;
    }

    /**
     * Returns a list of all available video IDs and their modification dates.
     */
    public abstract FindResults getAll() throws IOException;

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
        // Do nothing for general case, only useful for caching repositories
    }

    /**
     * Invalidates the entire cache.
     */
    public void invalidateAll() {
        // Do nothing for general case, only useful for caching repositories
    }


    /**
     * Returns a list of all available video IDs that match the genre string
     * and their modification dates.
     */
    public FindResults getByGenreString(String genre) throws IOException {

        FindResults results = getAll();
        ArrayList<FindResult> matching = new ArrayList<>(results.size());

        for (FindResult result : results) {
            if (getVideoInfo(result.getId()).getGenre().matches(genre)) {
                matching.add(result);
            }
        }
        matching.trimToSize();

        return new FindResults(matching);
    }
}

