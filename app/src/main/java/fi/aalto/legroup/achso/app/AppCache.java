package fi.aalto.legroup.achso.app;

import android.content.Context;
import android.os.AsyncTask;

import com.google.common.collect.ObjectArrays;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileFilter;

import javax.annotation.Nullable;

/**
 * Convenience methods for manipulating the app cache.
 */
public final class AppCache {

    private AppCache() {
        // Static access only
    }

    /**
     * Returns a new cache file with the given name.
     */
    public static File newFile(Context context, String name) {
        return new File(getCache(context), name);
    }

    /**
     * Returns the external cache directory. If the external filesystem is not available, returns
     * the internal cache directory.
     */
    public static File getCache(Context context) {
        File externalCache = context.getExternalCacheDir();

        if (externalCache == null) {
            return context.getCacheDir();
        } else {
            return externalCache;
        }
    }

    /**
     * Asynchronously clears expired files from the internal and external app caches.
     */
    public static void trim(Context context) {
        AsyncTask.execute(new CacheTrimmingRunnable(context));
    }

    /**
     * Runnable for trimming the cache.
     */
    private static class CacheTrimmingRunnable implements Runnable {

        private Context context;

        private CacheTrimmingRunnable(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public void run() {
            FileFilter cacheFilter = new CacheExpirationFileFilter();
            File internalCache = context.getCacheDir();

            // The external cache can be null if the filesystem is not available
            @Nullable
            File externalCache = context.getExternalCacheDir();

            File[] cacheFileList;

            if (externalCache == null || internalCache.equals(externalCache)) {
                cacheFileList = internalCache.listFiles(cacheFilter);
            } else {
                File[] internalList = internalCache.listFiles(cacheFilter);
                File[] externalList = externalCache.listFiles(cacheFilter);

                cacheFileList = ObjectArrays.concat(internalList, externalList, File.class);
            }

            for (File file : cacheFileList) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }

    }

    /**
     * Filter for expired cache files.
     */
    private static class CacheExpirationFileFilter implements FileFilter {

        /**
         * Cached files expire after 24 hours.
         */
        private static final DateTime THRESHOLD = DateTime.now().minusDays(1);

        @Override
        public boolean accept(File file) {
            return new DateTime(file.lastModified()).isBefore(THRESHOLD);
        }

    }

}
