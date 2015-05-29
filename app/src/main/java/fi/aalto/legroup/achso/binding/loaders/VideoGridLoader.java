package fi.aalto.legroup.achso.binding.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.VideoInfo;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;

public class VideoGridLoader extends AsyncTaskLoader<List<VideoInfo>> {

    AtomicInteger newestLoadIndex = new AtomicInteger();
    private static final String TAG = "VideoGridLoader";
    private String genreName;

    public VideoGridLoader(Context context, String genreName) {
        super(context);
        this.genreName = genreName;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public List<VideoInfo> loadInBackground() {

        VideoInfoRepository repo = App.videoInfoRepository;

        // Invalidate other active loads and store our own index so we can be invalidated
        int loadIndex = newestLoadIndex.incrementAndGet();

        Log.d(TAG, "Started loading " + loadIndex);
        try {
            VideoInfoRepository.FindResults results;

            if (genreName.equals("")) {
                results = repo.getAll();
            } else {
                results = repo.getByGenreString(genreName);
            }

            List<UUID> ids = results.sortDescending().getIDs();

            Log.d(TAG, "Got ids " + loadIndex);

            ArrayList<VideoInfo> infos = new ArrayList<>(ids.size());
            for (UUID id : ids) {

                // Only load if this thread is the newest loading task

                VideoInfo info = repo.getVideoInfo(id);
                infos.add(info);

                Log.d(TAG, "Loaded video " + loadIndex + ": " + ids.indexOf(id));
            }

            Log.d(TAG, "Done " + loadIndex);
            return infos;
        } catch (IOException e) {
            Log.d(TAG, "Load failed " + loadIndex + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Kinda hackish override, v4 support library doesn't provide isLoadInBackgroundCanceled(), so
     * it's reimplemented here with AtomicIntegers
     */
    @Override
    public boolean cancelLoad() {

        // Invalidate other active loads
        int cancelIndex = newestLoadIndex.getAndIncrement();
        Log.d(TAG, "Cancel loading video " + cancelIndex);

        return super.cancelLoad();
    }
}

