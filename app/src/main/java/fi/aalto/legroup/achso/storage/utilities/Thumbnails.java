package fi.aalto.legroup.achso.storage.utilities;

import android.net.Uri;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Wrapper for Android's ThumbnailUtils that implements a disk cache for thumbnails.
 */
public final class Thumbnails {

    /**
     * Cache directory base name. The exact location depends on which location is available.
     */
    private static final String CACHE_DIRECTORY_NAME = "video_thumbs";

    /**
     * Maximum cache size in bytes. Default is 25 MB.
     */
    private static final int MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024;

    private static Cache<UUID, File> cache;

    private Thumbnails() {
        // Static access only
    }

    public static File load(Uri videoUri) {
        if (cache == null) {
            ThumbnailCacher thumbnailCacher = new ThumbnailCacher();

            cache = CacheBuilder.newBuilder()
                    .maximumWeight()
                    .expireAfterAccess(1, TimeUnit.HOURS)
                    .removalListener(thumbnailCacher)
                    .weigher(thumbnailCacher)
                    .build(thumbnailCacher);
        }
    }

    private static class ThumbnailCacher extends CacheLoader<UUID, File> implements
            RemovalListener<UUID, File>, Weigher<UUID, File> {

        /**
         * Loads the thumbnail file corresponding to the given key into the file cache.
         * @throws Exception If the cache entry cannot be loaded.
         */
        @Override
        public File load(@Nonnull UUID key) throws Exception {
            return null;
        }

        /**
         * Deletes the file when the key/value pair is removed from the cache.
         */
        @Override
        public void onRemoval(@Nonnull RemovalNotification<UUID, File> notification) {

        }

        /**
         * Returns a byte weight value for the given key/value pair.
         */
        @Override
        public int weigh(@Nonnull UUID key, @Nonnull File value) {
            return (int) (value.length());
        }

    }

}
