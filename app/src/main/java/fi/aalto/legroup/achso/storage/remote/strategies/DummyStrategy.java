package fi.aalto.legroup.achso.storage.remote.strategies;

import android.net.Uri;

import java.io.IOException;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.remote.upload.ManifestUploader;
import fi.aalto.legroup.achso.storage.remote.upload.ThumbnailUploader;
import fi.aalto.legroup.achso.storage.remote.upload.VideoUploader;

/**
 * Always succeeds to "upload".
 */
public class DummyStrategy implements VideoUploader, ThumbnailUploader, ManifestUploader {

    private boolean uploadsThumbWithVideo;
    private Uri baseUrl;

    public DummyStrategy() {
        this(false, Uri.parse("http://example-host.com/achso/"));
    }
    public DummyStrategy(boolean uploadsThumbWithVideo, Uri baseUrl) {

        this.uploadsThumbWithVideo = uploadsThumbWithVideo;
        this.baseUrl = baseUrl;
    }

    private Uri getUrl(String suffix) {
        return baseUrl.buildUpon().appendPath(suffix).build();
    }

    @Override
    public VideoUploader.VideoUploadResult uploadVideo(Video video) throws IOException {

        Uri videoUrl = getUrl("videos/" + video.getId() + ".mp4");
        Uri thumbUrl = null;

        if (uploadsThumbWithVideo)
            thumbUrl = getUrl("videos/" + video.getId() + "_thumb.jpg");

        return new VideoUploadResult(videoUrl, thumbUrl);
    }

    @Override
    public Uri uploadThumb(Video video) throws IOException {

        return getUrl("thumbnails/" + video.getId() + ".jpg");
    }

    @Override
    public Uri uploadManifest(Video video) throws IOException {

        return getUrl("manifests/" + video.getId() + ".json");
    }

    @Override
    public void uploadCancelledCleanVideo(Video video, Uri videoUrl, Uri thumbUrl) {

        // Nothing to clean...
    }

    @Override
    public void uploadCancelledCleanThumb(Video video, Uri thumbUrl) {

        // Nothing to clean...
    }
}

