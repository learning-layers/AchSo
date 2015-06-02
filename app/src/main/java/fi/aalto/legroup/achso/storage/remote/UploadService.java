package fi.aalto.legroup.achso.storage.remote;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.remote.strategies.Strategy;

public final class UploadService extends IntentService {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";

    private Bus bus;

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
            video = App.videoRepository.getVideo(id).inflateNew();
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

        Strategy videoStrategy = App.videoStrategy;
        Strategy metadataStrategy = App.metadataStrategy;

        videoStrategy.setNext(metadataStrategy);

        bus.post(new UploadStateEvent(video.getId(), UploadStateEvent.Type.STARTED));

        videoStrategy.execute(video);

        bus.post(new UploadStateEvent(video.getId(), UploadStateEvent.Type.FINISHED));
    }

    private void postError(UUID id, String errorMessage) {
        this.bus.post(new UploadErrorEvent(null, id, errorMessage));
    }

}
