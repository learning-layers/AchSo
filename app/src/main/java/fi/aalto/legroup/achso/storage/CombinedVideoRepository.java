package fi.aalto.legroup.achso.storage;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.webkit.URLUtil;

import com.google.common.io.Files;
import com.rollbar.android.Rollbar;
import com.squareup.otto.Bus;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.aalto.legroup.achso.entities.Group;
import fi.aalto.legroup.achso.entities.OptimizedVideo;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoReference;
import fi.aalto.legroup.achso.entities.migration.VideoMigration;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.remote.SyncRequiredEvent;
import fi.aalto.legroup.achso.storage.remote.SyncService;
import fi.aalto.legroup.achso.storage.remote.VideoHost;

public class CombinedVideoRepository implements VideoRepository {

    private static final Pattern cacheNamePattern = Pattern.compile("(.*)_original\\.json");

    protected Map<UUID, OptimizedVideo> allVideos = Collections.emptyMap();
    protected List<Group> allGroups = Collections.emptyList();

    protected Bus bus;
    protected JsonSerializer serializer;
    protected File localRoot;
    protected File cacheRoot;

    protected boolean isFirstSync = true;
    protected boolean hasVideoToUpload = false;
    protected boolean forceImportant = false;

    protected int stateNumber = 0;

    protected List<VideoHost> cloudHosts = new ArrayList<>();

    public CombinedVideoRepository(Bus bus, JsonSerializer serializer, File localRoot,
            File cacheRoot) {
        this.bus = bus;
        this.serializer = serializer;
        this.localRoot = localRoot;
        this.cacheRoot = cacheRoot;
    }

    public void clear() {
        allVideos.clear();
        allGroups.clear();
        cloudHosts.clear();
        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    public void setCacheRoot(File path) {
        cacheRoot = path;
    }

    private List<UUID> getCacheIds() {
        String[] entries = cacheRoot.list();
        if (entries == null) {
            return Collections.emptyList();
        }

        ArrayList<UUID> results = new ArrayList<>(entries.length);

        for (String entry : entries) {
            Matcher matcher = cacheNamePattern.matcher(entry);
            if (matcher.matches()) {
                try {
                    UUID id = UUID.fromString(matcher.group(1));
                    results.add(id);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }

        results.trimToSize();
        return results;
    }

    private File getLocalVideoFile(UUID id) {
        return new File(localRoot, id + ".json");
    }

    private File getCacheFile(UUID id, String suffix) {
        return new File(cacheRoot, id + "_" + suffix + ".json");
    }

    private UUID getIdFromFile(File file) {
        final int uuidLength = 36;
        return UUID.fromString(file.getName().substring(0, uuidLength));
    }

    private File getOriginalCacheFile(UUID id) {
        return getCacheFile(id, "original");
    }

    private File getModifiedCacheFile(UUID id) {
        return getCacheFile(id, "modified");
    }

    protected Video readVideoFromFile(File file) throws IOException {
        Video video = serializer.load(Video.class, file.toURI());
        video.setManifestUri(Uri.fromFile(file));
        video.setLastModified(new Date(file.lastModified()));
        video.setRepository(this);


        // Sanity test to check if user deleted video file from gallery
        // Missing thumb nail icon is fine, since you can still watch the local video
        if (video.isLocal()) {
            Uri videoUri = video.getVideoUri();
            File sanityCheckFile = new File(videoUri.getPath());

            if (!sanityCheckFile.exists()) {

                // Also remove thumb file;
                File thumbFile = new File(video.getThumbUri().getPath());
                File videoFile = getLocalVideoFile(video.getId());

                thumbFile.delete();
                videoFile.delete();
                allVideos.remove(video.getId());
                bus.post(new VideoRepositoryUpdatedEvent(this));

                throw new IOException("Local video file not found at " + videoUri);
            }
        }

        return video;
    }

    protected void writeVideoToFile(Video video, File file) throws IOException {
        serializer.save(video, file.toURI());
    }

    protected void updateVideos(List<OptimizedVideo> videos, List<Group> groups) {
        Map<UUID, OptimizedVideo> newAllVideos = new HashMap<>();

        for (OptimizedVideo video : videos) {
            newAllVideos.put(video.getId(), video);
        }

        allVideos = newAllVideos;
        allGroups = groups;

        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    @Override
    public void addVideos(List<OptimizedVideo> videos) {
        for (OptimizedVideo video: videos) {
            if (!doesVideoExist(video.getId())) {
                allVideos.put(video.getId(), video);
            }
        }
        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    private File[] safeListFiles(File root, FilenameFilter filter) {
        File[] files = root.listFiles(filter);
        if (files != null) {
            return files;
        } else {
            return new File[0];
        }
    }

    /**
     * Add a host to the repository to sync to.
     */
    public void addHost(VideoHost host) {
        cloudHosts.add(host);
    }

    /**
     * Migrate all videos to the current format version.
     */
    public void migrateVideos(Context context) {
        Collection<OptimizedVideo> videos = allVideos.values();

        for (OptimizedVideo optimizedVideo : videos) {

            if (optimizedVideo.isRemote())
                continue;

            List<VideoMigration> migrations = VideoMigration.get(optimizedVideo.getFormatVersion());
            if (migrations.isEmpty())
                continue;

            Video video = optimizedVideo.inflate();

            for (VideoMigration migration : migrations) {
                migration.migrate(context, video);
            }

            video.setFormatVersion(Video.VIDEO_FORMAT_VERSION);
            video.save();
        }
    }

    /**
     * Populate the video list with local data.
     * Note: This is a fast operation and should be done on statup.
     */
    @Override
    public void refreshOffline() {

        List<OptimizedVideo> videos = new ArrayList<>();

        // Add the local videos
        File[] localFiles = safeListFiles(localRoot, new ManifestFileFilter());
        for (File file : localFiles) {

            OptimizedVideo video = tryLoadOrReUseVideo(file, getIdFromFile(file));
            if (video != null) {
                videos.add(video);
            }
        }

        // Add the cached remote videos
        List<UUID> cacheIds = getCacheIds();
        for (UUID id : cacheIds) {
            File original = getOriginalCacheFile(id);
            File modified = getModifiedCacheFile(id);

            File newest;
            if (modified.exists()) {
                newest = modified;
            } else {
                newest = original;
            }

            OptimizedVideo video = tryLoadOrReUseVideo(newest, id);
            if (video != null) {
                videos.add(video);
            }
        }

        updateVideos(videos, Collections.<Group>emptyList());
    }

    /**
     * Load a video from a file, or use one that has been already loaded.
     * Note: This should be only used when you know that the file you would load has the same
     * version of the video as is currently loaded.
     * @return Video if could successfully load, null if failed.
     */
    protected OptimizedVideo tryLoadOrReUseVideo(File file, UUID id) {

        try {
            OptimizedVideo video = allVideos.get(id);
            if (video != null) {
                return video;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        try {
            Video video = readVideoFromFile(file);
            return new OptimizedVideo(video);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Syncs the videos with the cloud and populate the video list with remote data.
     * - Download new and remotely modified videos
     * - Upload locally modified videos
     * - Resolve conflicts (merge)
     * Note: This operation is slow and should be only called from a background thread.
     */
    @Override
    public void refreshOnline() {

        int startState = this.stateNumber;

        List<OptimizedVideo> videos = new ArrayList<>();

        hasVideoToUpload = false;

        // Add the local videos
        // TODO: These can be cached
        File[] localFiles = safeListFiles(localRoot, new ManifestFileFilter());
        for (File file : localFiles) {

            OptimizedVideo localVideo = tryLoadOrReUseVideo(file, getIdFromFile(file));
            if (localVideo != null) {
                videos.add(localVideo);
            }
        }

        Set<UUID> addedVideoIds = new HashSet<>();
        List<Group> groups = new ArrayList<>();

        for (VideoHost host : cloudHosts) {

            List<VideoReference> results;

            try {
                groups.addAll(host.getGroups());
            } catch (IOException e) {
                // Pass
            }

            try {
                results = host.getIndex();
            } catch (IOException e) {
                continue;
            }

            for (VideoReference indexVideo : results) {

                UUID id = indexVideo.getId();

                File localFileOriginal = getOriginalCacheFile(id);
                File localFileModified = getModifiedCacheFile(id);

                try {

                    OptimizedVideo resultVideo = null;

                    if (localFileModified.exists()) {

                        Video localVideo = readVideoFromFile(localFileModified);
                        Video uploadedVideo = host.uploadVideoManifest(localVideo);
                        writeVideoToFile(uploadedVideo, localFileOriginal);

                        resultVideo = new OptimizedVideo(uploadedVideo);
                        localFileModified.delete();

                    } else if (localFileOriginal.exists()) {

                        OptimizedVideo localVideo = tryLoadOrReUseVideo(localFileOriginal, indexVideo.getId());
                        if (localVideo != null && localVideo.getRevision() == indexVideo.getRevision()) {
                            resultVideo = localVideo;
                        }
                    }

                    if (resultVideo == null) {
                        Video downloadedVideo = host.downloadVideoManifest(indexVideo.getId());
                        writeVideoToFile(downloadedVideo, localFileOriginal);

                        resultVideo = new OptimizedVideo(downloadedVideo);
                    }

                    resultVideo.setRepository(this);
                    addedVideoIds.add(resultVideo.getId());
                    videos.add(resultVideo);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception error) {
                    Rollbar.reportException(error);
                }
            }
        }

        // Remove unexistant cached videos
        List<UUID> cacheIds = getCacheIds();
        for (UUID id : cacheIds) {
            if (addedVideoIds.contains(id)) {
                continue;
            }

            File original = getOriginalCacheFile(id);
            File modified = getModifiedCacheFile(id);

            original.delete();
            modified.delete();
        }

        // TODO: Remove this
        if (this.stateNumber != startState) {
            // Temporary fix for handling modified state while refreshing online. Fixes disappearing
            // video after recording. Should make real cancel support for refreshing.
            this.refreshOnline();
        } else {
            isFirstSync = false;
            forceImportant = false;
            updateVideos(videos, groups);
        }
    }

    private void stateModified() {
        this.stateNumber = (this.stateNumber + 1) % 10000;
    }

    public void save(Video video) throws IOException {

        if (!video.getIsTemporary()) {
            UUID id = video.getId();
            File localFile = getLocalVideoFile(id);
            File cacheFile = getOriginalCacheFile(id);

            File targetFile;

            if (localFile.exists()) {
                // The video is local, we can just overwrite it
                targetFile = localFile;
            } else if (cacheFile.exists()) {
                // The video is from the cloud, save it as the modified one
                targetFile = getModifiedCacheFile(id);
            } else {
                // The video doesn't exist in any repository, add it to local one.
                targetFile = localFile;
            }

            if (video.isRemote()) {
                hasVideoToUpload = true;
            }

            this.stateModified();
            serializer.save(video, targetFile.toURI());

            // Do partial update
            allVideos.put(video.getId(), new OptimizedVideo(video));
        } else {
            for (VideoHost host: cloudHosts) {
                Video updatedVideo = host.uploadVideoManifest(video);
                allVideos.put(updatedVideo.getId(), new OptimizedVideo(updatedVideo));
            }
        }

        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    /**
     * Uploads the video manifest to some cloud host.
     * Note: The video data itself (and thumbnail) should be uploaded somewhere before this, since
     * their urls will be written in the manifest.
     */
    public void uploadVideo(Video video) throws IOException {

        Video result = null;

        IOException firstException = null;
        for (VideoHost host : cloudHosts) {

            try {
                result = host.uploadVideoManifest(video);
                // Stop at the first uploader which succeeds.
                break;
            } catch (IOException e) {
                if (firstException == null) {
                    firstException = e;
                }
                e.printStackTrace();
            }
        }

        if (result == null) {
            if (firstException != null) {
                throw new IOException("Failed to upload video", firstException);
            } else {
                throw new IOException("No host found to upload video to");
            }
        }

        UUID id = result.getId();

        File cacheFile = getOriginalCacheFile(id);
        writeVideoToFile(result, cacheFile);

        File localFile = getLocalVideoFile(id);
        if (!localFile.delete()) {
            // For logging purposes
            throw new IOException("Failed to delete local file");
        }
    }

    @Override
    public void findVideoByVideoUri(Uri uri, String type, VideoCallback callback) {
        if (type != null && type.equals("video/mp4")) {
            for (OptimizedVideo video : allVideos.values()) {
                if (video.getVideoUri().equals(uri)) {
                    callback.found(video.inflate());
                    return;
                }
            }

            new FindVideoTask(callback).execute(uri);
        } else if (uri.getScheme().equals("achso")) {
            UUID id = UUID.fromString(uri.getSchemeSpecificPart());

            try {
                OptimizedVideo video = getVideo(id);
                callback.found(video.inflate());
            } catch (IOException ignored) {
                new FindVideoByIdTask(callback).execute(id);
            }
        } else {
            callback.notFound();
        }
    }

    @Override
    public void findOnlineVideoByQuery(String query, VideoListCallback callback) {
        new QueryVideoOnlineTask(callback).execute(query);
    }

    private abstract class BaseVideoFindTask<T> extends AsyncTask<T, Void, Video> {
        private VideoCallback callback;

        public BaseVideoFindTask(VideoCallback callback) {
            this.callback = callback;
        }

        @Override
        protected void onPostExecute(Video video) {
            if (video != null) {
                callback.found(video);
            } else {
                callback.notFound();
            }
        }
    }

    private class QueryVideoOnlineTask extends  AsyncTask<String, Void, Void> {
        private VideoListCallback callback;

        public QueryVideoOnlineTask(VideoListCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(String... params) {
            for (VideoHost host: cloudHosts) {
                try {
                    ArrayList<Video> list = host.findVideosByQuery(params[0]);
                    callback.found(list);
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                    callback.notFound();
                }
            }
            return null;
        }
    }

    private class FindVideoTask extends BaseVideoFindTask<Uri> {

        public FindVideoTask(VideoCallback callback) {
            super(callback);
        }

        @Override
        protected Video doInBackground(Uri... params) {
            Uri uri = params[0];

            for (VideoHost host : cloudHosts) {
                try {
                    Video video = host.findVideoByVideoUri(uri);
                    if (video != null) {
                        return video;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }


    private class FindVideoByIdTask extends BaseVideoFindTask<UUID> {

        public FindVideoByIdTask(VideoCallback callback) {
            super(callback);
        }

        @Override
        protected Video doInBackground(UUID... params) {
            refreshOnline();
            try {
                OptimizedVideo video = getVideo(params[0]);
                return video.inflate();
            } catch (IOException e) {
                return null;
            }
        }
    }

    private class DeleteVideoTask extends AsyncTask<UUID, Void, Void> {

        @Override
        protected Void doInBackground(UUID... params) {

            for (UUID id : params) {
                for (VideoHost host : cloudHosts) {
                    try {
                        host.deleteVideoManifest(id);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            bus.post(new SyncRequiredEvent(CombinedVideoRepository.this));
        }
    }

    @Override
    public void delete(UUID id) throws IOException {
        OptimizedVideo video = getVideo(id);

        this.stateModified();

        if (video.isLocal()) {
            if (!getLocalVideoFile(id).delete()) {
                throw new IOException("Failed to delete file");
            }

            File thumbFile = new File(video.getThumbUri().getPath());
            File videoFile = new File(video.getVideoUri().getPath());

            // Doesn't matter if these fail, not necessary operation
            thumbFile.delete();
            videoFile.delete();

            // Remove the video from memory, if deleting has failed we have thrown before this
            allVideos.remove(id);
            bus.post(new VideoRepositoryUpdatedEvent(this));

        } else {
            new DeleteVideoTask().execute(id);
        }
    }

    @Override
    public Collection<OptimizedVideo> getAll() throws IOException {
        return allVideos.values();
    }

    @Override
    public Collection<Group> getGroups() throws IOException {
        return allGroups;
    }

    @Override @NonNull
    public OptimizedVideo getVideo(UUID id) throws IOException {
        OptimizedVideo video = allVideos.get(id);
        if (video == null)
            throw new IOException("Video not found");
        return video;
    }

    @Override
    public boolean doesVideoExist(UUID id) {
        try {
            getVideo(id);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public void forceNextSyncImportant() {
        forceImportant = true;
    }

    @Override
    public boolean hasImportantSyncPending() {
        return isFirstSync || hasVideoToUpload || forceImportant;
    }

    protected final static class ManifestFileFilter implements FilenameFilter {
        @Override
        public boolean accept(File directory, String fileName) {
            return Files.getFileExtension(fileName).equals("json");
        }
    }
}

