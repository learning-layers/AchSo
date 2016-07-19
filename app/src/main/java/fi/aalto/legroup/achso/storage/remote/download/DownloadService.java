package fi.aalto.legroup.achso.storage.remote.download;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;

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
        return false;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        UUID id = (UUID) intent.getSerializableExtra(ARG_VIDEO_ID);
        Video video;

        try {
            video = App.videoRepository.getVideo(id).inflate();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.print("id: " + video.getId());

        if (App.loginManager.isLoggedOut()) {
            return;
        }

        if (video.isLocal()) {
            return;
        }

        boolean success = tryDownload(video);

        if (success) {

        } else {
            // TODO: Get more data from tryUpload?
        }
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
}
