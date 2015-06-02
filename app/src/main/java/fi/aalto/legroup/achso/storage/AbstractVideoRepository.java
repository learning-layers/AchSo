package fi.aalto.legroup.achso.storage;

import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.OptimizedVideo;
import fi.aalto.legroup.achso.entities.Video;

public abstract class AbstractVideoRepository implements VideoRepository {

    protected Bus bus;

    public AbstractVideoRepository(Bus bus) {
        this.bus = bus;
    }

    /**
     * Returns a list of all available video IDs and their modification dates.
     */
    @Override
    public abstract FindResults getAll() throws IOException;
    /**
     * Returns the video for a given ID.
     */
    @Override
    public abstract OptimizedVideo getVideo(UUID id) throws IOException;

    /**
     * Saves a video to the repository.
     */
    @Override
    public abstract void save(Video video) throws IOException;

    /**
     * Deletes a video from the repository.
     */
    @Override
    public abstract void delete(UUID id) throws IOException;

    /**
     * Invalidates the cached information object with the given ID.
     */
    @Override
    public void invalidate(UUID id) {
        // Do nothing for general case, only useful for caching repositories
    }

    /**
     * Invalidates the entire cache.
     */
    @Override
    public void invalidateAll() {
        // Do nothing for general case, only useful for caching repositories
    }


    /**
     * Returns a list of all available video IDs that match the genre string
     * and their modification dates.
     */
    @Override
    public FindResults getByGenreString(String genre) throws IOException {

        FindResults results = getAll();
        ArrayList<FindResult> matching = new ArrayList<>(results.size());

        for (FindResult result : results) {

            OptimizedVideo video = getVideo(result.getId());
            if (video.getGenre().matches(genre)) {
                matching.add(result);
            }
        }
        matching.trimToSize();

        return new FindResults(matching);
    }

}

