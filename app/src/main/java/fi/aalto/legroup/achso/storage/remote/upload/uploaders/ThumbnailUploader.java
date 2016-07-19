package fi.aalto.legroup.achso.storage.remote.upload.uploaders;

import android.net.Uri;

import java.io.IOException;

import fi.aalto.legroup.achso.entities.Video;

public interface ThumbnailUploader {

    /**
     * Delete a thumbnail from the server.
     * @param video Video whose thumbnail to delete.
     */
    void deleteThumb(Video video) throws IOException;

    /**
     * Upload the thumbnail of the video. May do blocking network stuff, guaranteed not to be called from the UI thread.
     * @param video The video whose thumbnail to upload, should be treated as immutable.
     * @return The url to the thumbnail.
     */
    Uri uploadThumb(Video video) throws IOException;
}

