package fi.aalto.legroup.achso.storage.remote.download;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.remote.upload.UploadStateEvent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class DownloadService extends IntentService {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";
    private Bus bus;

    public DownloadService() {
        super("DownloadService");

        this.bus = App.bus;
    }

    public static void download(Context context, List<UUID> videos) {
        for (UUID id : videos) {
            Intent intent = new Intent(context, DownloadService.class);

            intent.putExtra(ARG_VIDEO_ID, id);

            context.startService(intent);
        }
    }

    private boolean tryDownload(Video video) {
        try {
            App.videoRepository.downloadVideo(video);
            return true;
        } catch(IOException ex) {
            return false;
        }
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
            postError(id, "User is not logged in");
            return;
        }

        if (video.isLocal()) {
            postError(id, "Video is already downloaded (Local video)");
            return;
        }

        boolean success = tryDownload(video);

        if (success) {

        } else {
            postError(id, "Failed to download video");
            // TODO: Get more data from tryUpload?
        }

        DownloadStateEvent.Type type = success
                ? UploadStateEvent.Type.SUCCEEDED
                : UploadStateEvent.Type.FAILED;

        bus.post(new DownloadStateEvent(video.getId(), type));
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

    private void postError(UUID id, String errorMessage) {
        this.bus.post(new DownloadErrorEvent(id, errorMessage));
    }
}
