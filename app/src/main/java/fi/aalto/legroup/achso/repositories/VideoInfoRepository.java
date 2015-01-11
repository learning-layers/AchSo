package fi.aalto.legroup.achso.repositories;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.VideoInfo;

/**
 * Provides cached read-only access to video information objects.
 *
 * @author Leo Nikkilä
 */
public interface VideoInfoRepository {

    /**
     * Returns a list of all entity IDs.
     */
    public List<UUID> getAll() throws IOException;

    /**
     *
     * @param genre
     * @return
     * @throws IOException
     */
    public List<UUID> getByGenreString(String genre) throws IOException;

    /**
     * Returns an information object describing an entity with the given ID.
     */
    public VideoInfo get(UUID id) throws IOException;

    /**
     * Invalidates the cached information object with the given ID.
     */
    public void invalidate(UUID id);

    /**
     * Invalidates the entire cache.
     */
    public void invalidateAll();

}
