package fi.aalto.legroup.achso.storage;

import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;

/**
 * Provides full read/write access to video root entities.
 */
public interface VideoRepository extends VideoInfoRepository {

    /**
     * Update the video repository with new data, but without doing any networking.
     */
    public void refreshOffline();

    /**
     * Update the video repository with new data from the internet. Should also upload modified
     * video manifests at this point.
     */
    public void refreshOnline();

    /**
     * Persists an entity, overwriting an existing one with the same ID if set.
     */
    public void save(Video video) throws IOException;

    /**
     * Deletes an entity with the given ID.
     */
    public void delete(UUID id) throws IOException;

    /**
     * Upload a video. May throw if the repository doesn't support uploading.
     */
    public void uploadVideo(Video video) throws IOException;
}
