package fi.aalto.legroup.achso.storage;

import android.net.Uri;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;

/**
 * Online video storage.
 */
public interface VideoHost {

    public List<VideoInfoRepository.FindResult> getIndex() throws IOException;

    public Video downloadVideoManifest(UUID id) throws IOException;

    /**
     * Persists an entity, overwriting an existing one with the same ID if set.
     */
    public Uri uploadVideoManifest(Video video) throws IOException;

    /**
     * Deletes an entity with the given ID.
     */
    public void deleteVideoManifest(UUID id) throws IOException;
}
