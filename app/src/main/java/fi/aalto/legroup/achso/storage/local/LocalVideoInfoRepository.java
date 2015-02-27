package fi.aalto.legroup.achso.storage.local;

import android.net.Uri;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import fi.aalto.legroup.achso.entities.VideoInfo;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;
import fi.aalto.legroup.achso.storage.VideoRepositoryUpdatedEvent;
import fi.aalto.legroup.achso.storage.formats.mp4.Mp4Reader;
import fi.aalto.legroup.achso.storage.formats.mp4.Mp4Writer;

/**
 * Stores videos locally as serialised XML files.
 */
public final class LocalVideoInfoRepository extends AbstractLocalVideoRepository
        implements VideoInfoRepository {

    private LoadingCache<UUID, VideoInfo> cache;

    /**
     * TODO: Cache keys as well.
     */
    public LocalVideoInfoRepository(Mp4Reader reader, Mp4Writer writer, File storageDirectory,
                                    Bus bus) {
        super(reader, writer, storageDirectory);

        bus.register(this);

        this.cache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build(new VideoInfoCacheLoader());
    }

    /**
     * Returns a list of all available video IDs sorted by descending modification date.
     */
    @Override
    public List<UUID> getAll() throws IOException {
        File[] videos = storageDirectory.listFiles(this);
        List<UUID> ids = new LinkedList<>();

        if (videos == null) {
            throw new IOException("Couldn't list files in " + storageDirectory);
        }

        // Sorting is ascending by default, needs to be reversed
        Arrays.sort(videos, Collections.reverseOrder(new DateModifiedComparator()));

        for (File file : videos) {
            ids.add(getId(file));
        }

        return ids;
    }

    /**
     * Returns a list of all available video IDs that have the given genre, sorted by descending
     * modification date.
     */
    @Override
    public List<UUID> getByGenreString(String genre) throws IOException {
        List<UUID> ids = new LinkedList<>();

        for (UUID id : getAll()) {
            VideoInfo video = get(id);

            if (video.getGenre().equalsIgnoreCase(genre)) {
                ids.add(id);
            }
        }

        return ids;
    }

    /**
     * Returns a cached video with the given ID if present, otherwise loads the video.
     * @throws IOException If the video cannot be loaded.
     */
    @Override
    public VideoInfo get(UUID key) throws IOException {
        try {
            return cache.get(key);
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    /**
     * Removes all cached values.
     */
    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * Removes the cached value for the given key.
     */
    @Override
    public void invalidate(UUID id) {
        cache.invalidate(id);
    }

    /**
     * FIXME: This is really stupid. "There are only two hard things in Computer Science..."
     */
    @Subscribe
    public void onVideoRepositoryUpdated(VideoRepositoryUpdatedEvent event) {
        invalidateAll();
    }

    private final class VideoInfoCacheLoader extends CacheLoader<UUID, VideoInfo> {

        /**
         * Loads and returns a video corresponding to the given ID.
         * @throws Exception If the video cannot be loaded.
         */
        @Override
        public VideoInfo load(@Nonnull UUID id) throws Exception {
            return reader.readInfo(getFile(id));
        }

    }

}
