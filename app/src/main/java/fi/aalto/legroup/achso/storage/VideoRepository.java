package fi.aalto.legroup.achso.storage;

import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;

/**
 * Provides full read/write access to video root entities.
 */
public interface VideoRepository {

    /**
     * Returns an entity with the given ID.
     */
    public Video getVideo(UUID id) throws IOException;

    /**
     * Persists an entity, overwriting an existing one with the same ID if set.
     */
    public void save(Video video) throws IOException;

    /**
     * Deletes an entity with the given ID.
     */
    public void delete(UUID id) throws IOException;
}
