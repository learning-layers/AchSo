package fi.aalto.legroup.achso.storage;

import java.io.IOException;
import java.util.List;
import java.util.Collection;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Group;
import fi.aalto.legroup.achso.entities.OptimizedVideo;

/**
 * Provides cached read-only access to video information objects.
 */
public interface VideoInfoRepository {

    /**
     * Returns all the videos in the repository.
     */
    public Collection<OptimizedVideo> getAll() throws IOException;

    /**
     * Returns all the videos in the repository.
     */
    public Collection<Group> getGroups() throws IOException;

    /**
     * Returns an information object describing an entity with the given ID.
     */
    public OptimizedVideo getVideo(UUID id) throws IOException;

    /**
     * Returns an information object describing an entity with the given ID.
     */
    public void addVideos(List<OptimizedVideo> videos);
}
