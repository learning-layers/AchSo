package fi.aalto.legroup.achso.storage.remote;

import android.net.Uri;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;

/**
 * Online video storage.
 */
public interface VideoHost {

    public class ManifestUploadResult {
        public Uri url;
        public String versionTag;

        public ManifestUploadResult(Uri url, String versionTag) {
            this.url = url;
            this.versionTag = versionTag;
        }
    }

    public List<VideoInfoRepository.FindResult> getIndex() throws IOException;

    public Video downloadVideoManifest(UUID id) throws IOException;

    /**
     * Persists an entity, overwriting an existing one with the same ID if set.
     * @param expectedVersionTag Version tag that the cloud video should have, uploading fails and this returns null if the version in the cloud doesn't match the tag. If null upload always.
     */
    public ManifestUploadResult uploadVideoManifest(Video video, String expectedVersionTag) throws IOException;

    /**
     * Deletes an entity with the given ID.
     */
    public void deleteVideoManifest(UUID id) throws IOException;
}
