package fi.aalto.legroup.achso.storage.remote.upload;

import android.net.Uri;

import java.io.IOException;

import fi.aalto.legroup.achso.entities.Video;

public interface ThumbnailUploader {

    class ThumbnailUploadResult {

        private Uri thumbUrl;

        public ThumbnailUploadResult() {
            this(null);
        }
        public ThumbnailUploadResult(Uri thumbUrl) {
            this.thumbUrl = thumbUrl;
        }

        public Uri getThumbUrl() {
            return thumbUrl;
        }
    };


    /**
     * This is called if some later part of the upload chain fails. This means that the url which the thumbnail was uploaded to will be forgotten and never used. To be more friendly to the server the resource can be cleaned up at this point. May do blocking network stuff, guaranteed not to be called from the UI thread.
     * @param video Video whose thumbnail was succesfully uploaded with this uploader, but failed to upload other data.
     * @param result The returned result from the uploadThumb(video) of this uploader
     */
    void uploadCancelledCleanThumb(Video video, ThumbnailUploadResult result) throws IOException;

    /**
     * Upload the thumbnail of the video. May do blocking network stuff, guaranteed not to be called from the UI thread.
     * @param video The video whose thumbnail to upload, should be treated as immutable.
     * @return The url to the thumbnail.
     */
    ThumbnailUploadResult uploadThumb(Video video) throws IOException;
}

