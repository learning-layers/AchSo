package fi.aalto.legroup.achso.binding.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.VideoInfo;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;

public class VideoGridLoader extends AsyncTaskLoader<List<VideoInfo>> {

    public VideoGridLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public List<VideoInfo> loadInBackground() {

        VideoInfoRepository repo = App.videoInfoRepository;

        try {
            List<UUID> ids = repo.getAll().sortDescending().getIDs();

            ArrayList<VideoInfo> infos = new ArrayList<>(ids.size());
            for (UUID id : ids) {
                VideoInfo info = repo.getVideoInfo(id);
                infos.add(info);
            }

            return infos;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}

