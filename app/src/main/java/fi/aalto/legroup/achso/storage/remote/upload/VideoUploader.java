package fi.aalto.legroup.achso.storage.remote.upload;

import android.net.Uri;

import java.io.IOException;

import fi.aalto.legroup.achso.entities.Video;

public interface VideoUploader {

    class VideoUploadResult {

        private Uri videoUrl;
        private Uri thumbUrl;

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

        public Uri getVideoUrl() {
            return videoUrl;
        }
        public Uri getThumbUrl() {
            return thumbUrl;
        }
    };

    /**
     * This is called if some later part of the upload chain fails. This means that the url which the video was uploaded to will be forgotten and never used. To be more friendly to the server the resource can be cleaned up at this point. May do blocking network stuff, guaranteed not to be called from the UI thread.
     * @param video Video which was succesfully uploaded with this uploader, but failed to upload other data.
     * @param videoUrl The returned url from the uploadVideo(video).getVideoUrl() of this uploader
     * @param thumbUrl The returned url from the uploadVideo(video).getThumbUrl() of this uploader
     */
    void uploadCancelledCleanVideo(Video video, Uri videoUrl, Uri thumbUrl);

    /**
     * Upload the video data of the video. May do blocking network stuff, guaranteed not to be called from the UI thread.
     * @param video The video to upload, should be treated as immutable.
     * @return The url to the video and possibly to the thumbnail if provided from the server.
     */
    VideoUploadResult uploadVideo(Video video) throws IOException;
}

