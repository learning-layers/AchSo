package fi.aalto.legroup.achso.storage.remote;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.squareup.otto.Bus;

import java.io.IOException;

import fi.aalto.legroup.achso.app.App;

/**
 * Download and upload possibly merging video manifests with the cloud servers.
 * In practice just calls App.videoRepository.refreshOnline() in a background thread.
 */
public final class SyncService extends IntentService {

    private Bus bus;

    /**
     * Convenience method for using this service.
     */
    public static void syncWithCloudStorage(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        context.startService(intent);
    }

    public SyncService() {
        super("SyncService");

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
            App.videoRepository.refreshOnline();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
