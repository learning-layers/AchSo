package fi.aalto.legroup.achso.repositories.local;

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
import fi.aalto.legroup.achso.repositories.VideoInfoRepository;
import fi.aalto.legroup.achso.repositories.VideoRepositoryUpdatedEvent;
import fi.aalto.legroup.achso.serialization.json.JsonSerializerService;

/**
 * Stores videos locally as serialised XML files.
 *
 * @author Leo Nikkilä
 */
public final class LocalVideoInfoRepository extends AbstractLocalVideoRepository
        implements VideoInfoRepository {

    protected LoadingCache<UUID, VideoInfo> cache;

    /**
     * TODO: Cache keys as well.
     */
    public LocalVideoInfoRepository(Bus bus, JsonSerializerService serializer, File storage) {
        super(serializer, storage);

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
        File[] manifests = storageDirectory.listFiles(this);
        LinkedList<UUID> ids = new LinkedList<>();

        if (manifests == null) throw new IOException("Couldn't list files in " + storageDirectory);

        // Sorting is ascending by default, needs to be reversed
        Arrays.sort(manifests, Collections.reverseOrder(new DateModifiedComparator()));

        for (File manifest : manifests) {
            ids.add(getIdFromManifest(manifest));
        }

        return ids;
    }

    /**
     * Returns a list of all available video IDs sorted by descending modification date.
     */
    @Override
    public List<UUID> getByGenreString(String genre) throws IOException {
        List<UUID> ids = this.getAll();
        LinkedList<UUID> matching = new LinkedList<>();

        for(UUID id : ids) {
            VideoInfo info = this.get(id);
            if(info.getGenre().matches(genre)) {
                matching.add(id);
            }
        }

        return matching;
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
            return serializer.load(VideoInfo.class, getManifestFromId(id).toURI());
        }

    }

}
