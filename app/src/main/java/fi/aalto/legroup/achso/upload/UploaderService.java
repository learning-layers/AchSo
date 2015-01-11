package fi.aalto.legroup.achso.upload;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.util.App;

public class UploaderService extends IntentService {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";

    private Bus bus;

    /**
     * Convenience method for using this service.
     */
    public static void upload(Context context, List<UUID> videos) {
        for (UUID id : videos) {
            Intent intent = new Intent(context, UploaderService.class);

            intent.putExtra(ARG_VIDEO_ID, id);

            context.startService(intent);
        }
    }

    public UploaderService() {
        super("AchSoUploaderService");

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

    @Override
    protected void onHandleIntent(Intent intent) {
        UUID id = (UUID) intent.getSerializableExtra(ARG_VIDEO_ID);
        Video video;

        try {
            video = App.videoRepository.get(id);
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

        Uploader videoUploader = App.videoUploader;
        Uploader metadataUploader = App.metadataUploader;

        videoUploader.setNext(metadataUploader);

        bus.post(new UploadStateEvent(video.getId(), UploadStateEvent.Type.STARTED));

        videoUploader.upload(video);

        bus.post(new UploadStateEvent(video.getId(), UploadStateEvent.Type.FINISHED));
    }

    private void postError(UUID id, String errorMessage) {
        this.bus.post(new UploadErrorEvent(null, id, errorMessage));
    }

}
