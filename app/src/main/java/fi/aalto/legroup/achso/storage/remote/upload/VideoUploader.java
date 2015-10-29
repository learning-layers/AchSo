package fi.aalto.legroup.achso.storage.remote.upload;

import android.net.Uri;

import java.io.IOException;

import fi.aalto.legroup.achso.entities.Video;

public interface VideoUploader {

    class VideoUploadResult {

        public Uri videoUrl;
        public Uri thumbUrl;

        public VideoUploadResult() {
            this(null, null);
        }
        public VideoUploadResult(Uri videoUrl) {
            this(videoUrl, null);
        }
        public VideoUploadResult(Uri videoUrl, Uri thumbUrl) {
            this.videoUrl = videoUrl;
            this.thumbUrl = thumbUrl;
        }
    };

    /**
     * Delete the video data from the server.
     * @param video Video whose thumbnail to delete.
     */
    void deleteVideo(Video video) throws IOException;

    /**
     * Upload the video data of the video. May do blocking network stuff, guaranteed not to be called from the UI thread.
     * @param video The video to upload, should be treated as immutable.
     * @return The url to the video and possibly to the thumbnail if provided from the server.
     */
    VideoUploadResult uploadVideo(Video video) throws IOException;
}

