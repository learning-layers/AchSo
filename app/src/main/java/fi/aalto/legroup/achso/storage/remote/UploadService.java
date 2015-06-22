package fi.aalto.legroup.achso.storage.remote;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.CombinedVideoRepository;
import fi.aalto.legroup.achso.storage.remote.upload.MetadataUploader;
import fi.aalto.legroup.achso.storage.remote.upload.ThumbnailUploader;
import fi.aalto.legroup.achso.storage.remote.upload.VideoUploader;

public final class UploadService extends IntentService {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";

    private Bus bus;
    private List<VideoUploader> videoUploaders;
    private List<ThumbnailUploader> thumbUploaders;
    private List<MetadataUploader> metadataUploaders;

    /**
     * Convenience method for using this service.
     */
    public static void upload(Context context, List<UUID> videos) {
        for (UUID id : videos) {
            Intent intent = new Intent(context, UploadService.class);

            intent.putExtra(ARG_VIDEO_ID, id);

            context.startService(intent);
        }
    }

    public UploadService() {
        super("UploadService");

        // TODO: Initialize these in somewhere which makes sense.
        videoUploaders = new ArrayList<>();
        thumbUploaders = new ArrayList<>();
        metadataUploaders = new ArrayList<>();

        // I'm sorry...
        // Don't try this at home
        videoUploaders.add((VideoUploader) App.ownCloudStrategy);
        thumbUploaders.add((ThumbnailUploader) App.ownCloudStrategy);
        thumbUploaders.add(App.shareServerStrategy);
        videoUploaders.add(App.shareServerStrategy);
        //manifestSources.add(App.shareServerSource);
        //metadataUploaders.add((MetadataUploader) App.metadataStrategy);

        // TODO: Inject instead
        this.bus = App.bus;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.bus.register(this);
    }

    @Override
    public void onDestroy() {
        this.bus.unregister(this);
        super.onDestroy();
    }

    private boolean tryUpload(Video video) {

        // Try to upload the Video mp4 data somewhere.

        VideoUploader videoHost = null;
        ThumbnailUploader thumbnailHost = null;

        VideoUploader.VideoUploadResult videoResult = null;

        for (VideoUploader uploader : videoUploaders) {
            try {
                videoResult = uploader.uploadVideo(video);

                videoHost = uploader;

                // Stop at the first uploader which succeeds.
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (videoResult == null) {

            // No cleanup required

            return false;
        }
        video.setVideoUri(videoResult.getVideoUrl());

        // If the video uploader didn't support creating a thumbnail, try to upload it somewhere
        // else.

        ThumbnailUploader.ThumbnailUploadResult thumbResult = null;
        Uri thumbUrl = null;

        if (videoResult.getThumbUrl() != null) {

            // Use the thumbnail provided by the video uploader
            thumbUrl = videoResult.getThumbUrl();

        } else {

            // Upload the thumbnail somewhere else
            for (ThumbnailUploader uploader : thumbUploaders) {
                try {
                    thumbResult = uploader.uploadThumb(video);
                    thumbnailHost = uploader;
                    thumbUrl = thumbResult.getThumbUrl();

                    // Stop at the first uploader which succeeds.
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (thumbUrl == null) {

            // Cleanup, it doesn't really matter if it succeeds or not so just ignore the error
            try {
                videoHost.uploadCancelledCleanVideo(video, videoResult);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }
        video.setThumbUri(thumbUrl);

        // Now we have the video and thumbnail urls and they are stored in the Video object, we
        // can serialize it to json with the new data and upload that.

        // FIXME: Don't do this
        boolean didUpload = false;
        try {
            ((CombinedVideoRepository)App.videoRepository).uploadVideo(video);
            didUpload = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (didUpload) {

            // Cleanup, it doesn't matter if it succeeds _but_ it would be good that we try to cleanup every resource even if an earlier one fails.
            try {
                if (thumbnailHost != null)
                    thumbnailHost.uploadCancelledCleanThumb(video, thumbResult);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                videoHost.uploadCancelledCleanVideo(video, videoResult);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        // In the end just run through some other uploaders that just receive metadata instead of
        // hosting the video data.

        for (MetadataUploader uploader : metadataUploaders) {

            try {
                uploader.uploadMetadata(video);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        UUID id = (UUID) intent.getSerializableExtra(ARG_VIDEO_ID);
        Video video;

        try {
            video = App.videoRepository.getVideo(id).inflate();
        } catch (IOException e) {
            e.printStackTrace();
            postError(id, "Could not load video.");
            return;
        }

        if (App.loginManager.isLoggedOut()) {
            postError(id, getString(R.string.not_loggedin_nag_title));
            return;
        }

        if (video.isRemote()) {
            postError(id, "Video already uploaded.");
            return;
        }

        bus.post(new UploadStateEvent(video.getId(), UploadStateEvent.Type.STARTED));

        boolean success = tryUpload(video);

        if (success) {
            video.save();
        } else {
            // TODO: Get more data from tryUpload?
            postError(video.getId(), "Failed to upload video.");
        }

        bus.post(new UploadStateEvent(video.getId(), UploadStateEvent.Type.FINISHED));
    }

    private void postError(UUID id, String errorMessage) {
        this.bus.post(new UploadErrorEvent(null, id, errorMessage));
    }

}
