package fi.aalto.legroup.achso.app;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

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
        AsyncTask.execute(new LegacyCacheClearingRunnable());
    }

    /**
     * Clears old shared .achso files from the external storage root.
     * TODO: Remove by the next release.
     */
    private static class LegacyCacheClearingRunnable implements Runnable, FileFilter {

        @Override
        public void run() {
            File legacyCache = Environment.getExternalStorageDirectory();
            File[] cacheFileList = legacyCache.listFiles(this);

            for (File file : cacheFileList) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }

        @Override
        public boolean accept(File file) {
            return file.getName().endsWith(".achso");
        }

    }

    /**
     * Runnable for trimming the cache.
     */
    private static class CacheTrimmingRunnable implements Runnable, FileFilter {

        /**
         * Cached files expire after 24 hours.
         */
        private static final DateTime THRESHOLD = DateTime.now().minusDays(1);

        private Context context;

        private CacheTrimmingRunnable(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public void run() {
            File internalCache = context.getCacheDir();

            // The external cache can be null if the filesystem is not available
            @Nullable
            File externalCache = context.getExternalCacheDir();

            File[] cacheFileList;

            if (externalCache == null || internalCache.equals(externalCache)) {
                cacheFileList = internalCache.listFiles(this);
            } else {
                File[] internalList = internalCache.listFiles(this);
                File[] externalList = externalCache.listFiles(this);

                cacheFileList = ObjectArrays.concat(internalList, externalList, File.class);
            }

            for (File file : cacheFileList) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }

        @Override
        public boolean accept(File file) {
            return new DateTime(file.lastModified()).isBefore(THRESHOLD);
        }

    }

}
