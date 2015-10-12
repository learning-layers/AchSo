package fi.aalto.legroup.achso.storage.remote;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import fi.aalto.legroup.achso.app.App;

/**
 * Download and upload possibly merging video manifests with the cloud servers.
 * In practice just calls App.videoRepository.refreshOnline() in a background thread.
 */
public final class SyncService extends IntentService {
    /**
     * Convenience method for using this service.
     */
    public static void syncWithCloudStorage(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        context.startService(intent);
    }

    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        App.videoRepository.refreshOnline();
    }
}
