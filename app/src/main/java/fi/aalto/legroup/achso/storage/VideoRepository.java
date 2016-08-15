package fi.aalto.legroup.achso.storage;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.playback.PlayerActivity;

/**
 * Provides full read/write access to video root entities.
 */
public interface VideoRepository extends VideoInfoRepository {

    interface VideoCallback {
        public void found(Video video);
        public void notFound();
    }

    interface VideoListCallback {
        public void found(ArrayList<Video> videos);
        public void notFound();
    }

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
     * Flags the next sync to be "important" ie. every call to hasImportantSyncPending() returns
     * true after calling this before a sync is done.
     */
    public void forceNextSyncImportant();

    /**
     * Returns if the next call to refreshOnline() results in changes that are "important" such
     * as retrieving content for the first time in a while or uploading user changes.
     */
    public boolean hasImportantSyncPending();

    /**
     * Persists an entity, overwriting an existing one with the same ID if set.
     */
    public void save(Video video, VideoCallback callback) throws IOException;

    /**
     * Deletes an entity with the given ID.
     */
    public void delete(UUID id) throws IOException;

    /**
     * Upload a video. May throw if the repository doesn't support uploading.
     */
    public void uploadVideo(Video video) throws IOException;

    /**
     * Downloads a video.
     */
    public void downloadVideo(Video video) throws IOException;

    public File getThumbCacheFile(UUID id);

    public File getVideoCacheFile(UUID id);

    /**
     *
     * @return A valid Video or null if not found.
     */
    public void findVideoByVideoUri(Uri videoUri, String type, VideoCallback callback);

    public void findOnlineVideoByQuery(String query, VideoListCallback callback);

    /**
     * Checks whether video storage contains a video with a certain ID.
     */
    public boolean doesVideoExist(UUID id);

    /**
     * Migrate all videos to the current format version.
     */
    public void migrateVideos(Context context);
}
