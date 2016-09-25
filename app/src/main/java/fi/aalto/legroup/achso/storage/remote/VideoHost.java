package fi.aalto.legroup.achso.storage.remote;

import android.net.Uri;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Group;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoReference;

/**
 * Online video storage.
 */
public interface VideoHost {

    public List<VideoReference> getIndex() throws IOException;

    public List<Group> getGroups() throws IOException;

    public void unshareVideo(UUID videoId, int groupId) throws IOException;

    public void shareVideo(UUID videoId, int groupId) throws IOException, JSONException;

    public ArrayList<Video> findVideosByQuery(String query) throws  IOException;

    /**
     * Download video manifest data with a given ID.
     * @return A Video object describing the video. Note: Should have `manifestUri` set and also
     * `versionTag` if one is available.
     * @throws IOException
     */
    public Video downloadVideoManifest(UUID id) throws IOException;

    public void downloadCachedFiles(Video video, Uri thumbUri, Uri videoUri) throws IOException;

    /**
     * Persists an entity, overwriting an existing one with the same ID if set.
     */
    public Video uploadVideoManifest(Video video) throws IOException;

    /**
     * Deletes an entity with the given ID.
     */
    public void deleteVideoManifest(UUID id) throws IOException;

    /**
     * Finds a video by the video source uri.
     */
    public Video findVideoByVideoUri(Uri videoUri) throws IOException;
}
