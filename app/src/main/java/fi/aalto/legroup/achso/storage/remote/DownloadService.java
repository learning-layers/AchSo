package fi.aalto.legroup.achso.storage.remote;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.squareup.otto.Bus;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.storage.CombinedVideoRepository;

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
        // TODO: Don't do this
        ((CombinedVideoRepository)App.videoRepository).refreshOnline();
    }
}
