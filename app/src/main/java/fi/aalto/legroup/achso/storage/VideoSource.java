package fi.aalto.legroup.achso.storage;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.OptimizedVideo;
import fi.aalto.legroup.achso.entities.Video;

/**
 * Provides full read/write access to video root entities.
 */
public interface VideoSource {

    /**
     * Returns a list all of the videos from the source.
     */
    public List<OptimizedVideo> updateNonBlocking() throws IOException;

    /**
     * Persists an entity, overwriting an existing one with the same ID if set.
     */
    public void saveVideo(Video video) throws IOException;

    /**
     * Deletes an entity with the given ID.
     */
    public void deleteVideo(UUID id) throws IOException;
}
