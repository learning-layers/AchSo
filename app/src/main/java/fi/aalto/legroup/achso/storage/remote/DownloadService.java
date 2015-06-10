package fi.aalto.legroup.achso.storage.remote;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.squareup.otto.Bus;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;
import fi.aalto.legroup.achso.storage.remote.strategies.OwnCloudStrategy;

public final class DownloadService extends IntentService {

    private Bus bus;

    /**
     * Convenience method for using this service.
     */
    public static void download(Context context) {
        Intent intent = new Intent(context, DownloadService.class);
        context.startService(intent);
    }

    public DownloadService() {
        super("DownloadService");

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
        try {

            VideoInfoRepository.FindResults results = ((OwnCloudStrategy) App.ownCloudStrategy).getIndex();

            for (VideoInfoRepository.FindResult result : results) {

                UUID id = result.getId();
                File file = new File(App.localStorageDirectory, id + ".json");

                ((OwnCloudStrategy) App.ownCloudStrategy).downloadManifest(file, id);
            }

            App.localVideoRepository.refresh();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
