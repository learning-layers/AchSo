package fi.aalto.legroup.achso.storage;

import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoInfo;

/**
 * Wraps another AbstractVideoRepository instance and adds support for caching the data.
 */
public class CachedVideoRepository extends AbstractVideoRepository {

    /**
     * How much data to already fetch when listing videos with getAll() or getByGenreString()
     */
    public enum PrefetchLevel {
        PREFETCH_NONE,
        PREFETCH_VIDEO_INFO,
        PREFETCH_VIDEO,
    }

    /**
     * Cached information of a video's manifest. Has three levels of caching:
     * 1. Last modified time
     * 2. VideoInfo structure
     * 3. Video structure
     */
    protected static class CacheEntry {

        public CacheEntry(long cacheTime, long lastModified) {
            this.cacheTime = cacheTime;
            this.lastModified = lastModified;
        }

        private long cacheTime;
        private long lastModified;
        private VideoInfo videoInfo;
        private Video video;

        public long getCacheTime() { return cacheTime; }

        public void setCacheTime(long cacheTime) { this.cacheTime = cacheTime; }

        public long getLastModified() { return lastModified; }

        public VideoInfo getVideoInfo() { return videoInfo; }

        public Video getVideo() { return video; }

        public boolean isCacheTimeOlderThan(long modified) {
            return lastModified < modified;
        }

        public boolean isModifyTimeOlderThan(long modified) {
            return lastModified < modified;
        }

        public VideoInfo cacheVideoInfo(VideoInfo videoInfo) {

            this.videoInfo = videoInfo;

            // Invalidate the video object if it existed, since we have a newer VideoInfo.
            this.video = null;

            return videoInfo;
        }

        public Video cacheVideo(Video video) {

            this.video = video;

            // VideoInfos are subsets of Videos so update the VideoInfo at the same time.
            this.videoInfo = (VideoInfo) video;

            return video;
        }
    }

    protected AbstractVideoRepository inner;

    protected Map<UUID, CacheEntry> cache = new HashMap<>();
    protected PrefetchLevel prefetchLevel = PrefetchLevel.PREFETCH_VIDEO_INFO;
    protected long expireDuration = 60 * 5;

    public CachedVideoRepository(Bus bus, AbstractVideoRepository inner) {
        super(bus);
        this.inner = inner;
    }

    public PrefetchLevel getPrefetchLevel() { return prefetchLevel; }

    public void setPrefetchLevel(PrefetchLevel level) { prefetchLevel = level; }

    public long getExpireDuration() { return expireDuration; }

    public void setExpireDuration(long durationInSeconds) { expireDuration = durationInSeconds; }

    /**
     * The current time to use for cache expiration.
     */
    protected long getTime() {
        return new Date().getTime();
    }

    /**
     * Videos in the cache whose timestamp is older than this should be considered outdated and
     * need to be re-cached.
     */
    protected long getCacheExpireTime() {

        return getTime() - expireDuration;
    }

    /**
     * Try to find a video from the cache and add it if not found, does not prefetch because
     * usually when this is called we want to find the VideoInfo or Video immediately afterwards.
     */
    protected CacheEntry getOrCache(UUID id) throws IOException {

        CacheEntry entry = cache.get(id);
        if (entry != null && !entry.isCacheTimeOlderThan(getCacheExpireTime())) {
            return entry;
        }

        return cache.put(id, new CacheEntry(getTime(), inner.getLastModifiedTime(id)));
    }

    /**
     * Iterate through all the videos and add them to the cache, prefetching if wanted.
     */
    protected FindResults updateCache() throws IOException {

        FindResults results = inner.getAll();

        for (FindResult result : results) {

            UUID id = result.getId();
            long lastModified = result.getLastModified();

            CacheEntry cachedEntry = cache.get(id);
            if (cachedEntry == null || cachedEntry.isModifyTimeOlderThan(lastModified)) {

                CacheEntry newEntry = new CacheEntry(getTime(), lastModified);

                switch (prefetchLevel) {
                    case PREFETCH_NONE:
                        // Do nothing
                        break;
                    case PREFETCH_VIDEO_INFO:
                        newEntry.cacheVideoInfo(inner.getVideoInfo(id));
                        break;
                    case PREFETCH_VIDEO:
                        newEntry.cacheVideo(inner.getVideo(id));
                        break;
                }
                cache.put(id, newEntry);
            } else {

                // The cached entry is still valid, extend lifetime.
                cachedEntry.setCacheTime(getTime());
            }

        }

        return results;
    }

    @Override
    public FindResults getAll() throws IOException {

        return updateCache();
    }

    @Override
    public long getLastModifiedTime(UUID id) throws IOException {

        CacheEntry entry = getOrCache(id);
        return entry.getLastModified();

    }

    @Override
    public VideoInfo getVideoInfo(UUID id) throws IOException {

        CacheEntry entry = getOrCache(id);

        VideoInfo cachedVideoInfo = entry.getVideoInfo();
        if (cachedVideoInfo != null) {
            return cachedVideoInfo;
        }

        VideoInfo newVideoInfo = inner.getVideoInfo(id);
        return entry.cacheVideoInfo(newVideoInfo);
    }

    @Override
    public Video getVideo(UUID id) throws IOException {

        CacheEntry entry = getOrCache(id);
        Video cachedVideo = entry.getVideo();

        if (cachedVideo != null) {
            return cachedVideo;
        }

        Video newVideo = inner.getVideo(id);
        newVideo.setRepository(this);

        return entry.cacheVideo(newVideo);
    }

    @Override
    public void save(Video video) throws IOException {

        inner.save(video);
        invalidate(video.getId());
    }

    @Override
    public void delete(UUID id) throws IOException {

        inner.delete(id);
        invalidate(id);
    }

    @Override
    public void invalidate(UUID id) {

        cache.remove(id);
    }

    @Override
    public void invalidateAll() {

        // There's no need to remove the cache, this checks if the videos are outdated and
        // fetches only the outdated ones
        try {
            updateCache();
        } catch (IOException e) {

            // If the updating of the cache somehow fails invalidate it by clearing
            cache.clear();
        }
    }
}

