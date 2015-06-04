package fi.aalto.legroup.achso.storage.remote.upload;

import android.net.Uri;

import java.io.IOException;

import fi.aalto.legroup.achso.entities.Video;

public interface ManifestUploader {

    /**
     * Upload the manifest of the video. May do blocking network stuff, guaranteed not to be called from the UI thread.
     * @param video The video whose manifest to upload, should be treated as immutable.
     * @return The url to the manifest.
     */
    Uri uploadManifest(Video video) throws IOException;
}

