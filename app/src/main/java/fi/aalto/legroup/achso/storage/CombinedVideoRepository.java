package fi.aalto.legroup.achso.storage;

import android.net.Uri;

import com.squareup.otto.Bus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

import fi.aalto.legroup.achso.entities.OptimizedVideo;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.remote.VideoHost;

public class CombinedVideoRepository implements VideoRepository {

    private static final Pattern cacheNamePattern = Pattern.compile("(.*)_original\\.json");

    protected Map<UUID, OptimizedVideo> allVideos = Collections.emptyMap();
    protected List<FindResult> allResults = Collections.emptyList();

    protected Bus bus;
    protected JsonSerializer serializer;
    protected File localRoot;
    protected File cacheRoot;

    protected List<VideoHost> cloudHosts = new ArrayList<>();

    public CombinedVideoRepository(Bus bus, JsonSerializer serializer, File localRoot, File cacheRoot) {
        this.bus = bus;
        this.serializer = serializer;
        this.localRoot = localRoot;
        this.cacheRoot = cacheRoot;
    }

    public void addHost(VideoHost host) {
        cloudHosts.add(host);
    }

    private List<UUID> getCacheIds() {
        String[] entries = cacheRoot.list();
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

    private File getOriginalCacheFile(UUID id) {
        return getCacheFile(id, "original");
    }

    private File getModifiedCacheFile(UUID id) {
        return getCacheFile(id, "modified");
    }

    protected Video readVideoFromFile(File file) throws IOException {
        Video video = serializer.load(Video.class, file.toURI());
        video.setManifestUri(Uri.fromFile(file));
        video.setRepository(this);
        return video;
    }

    protected void writeVideoToFile(Video video, File file) throws IOException {
        serializer.save(video, file.toURI());
    }

    protected void updateVideos(List<OptimizedVideo> videos) {
        Map<UUID, OptimizedVideo> newAllVideos = new HashMap<>();
        List<FindResult> newAllResults = new ArrayList<>();

        for (OptimizedVideo video : videos) {
            newAllVideos.put(video.getId(), video);
            newAllResults.add(new FindResult(video.getId(), video.getLastModified()));
        }

        allVideos = newAllVideos;
        allResults = newAllResults;

        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    /**
     * Populate the video list with local data.
     * Note: This is a fast operation and should be done on statup.
     */
    public void refreshOffline() {

        List<OptimizedVideo> videos = new ArrayList<>();

        // Add the local videos
        File[] localFiles = localRoot.listFiles();
        for (File file : localFiles) {

            try {
                Video video = readVideoFromFile(file);
                videos.add(new OptimizedVideo(video));
            } catch (IOException e) {
                e.printStackTrace();
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

            try {
                Video video = readVideoFromFile(newest);
                videos.add(new OptimizedVideo(video));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        updateVideos(videos);
    }

    /**
     * Syncs the videos with the cloud and populate the video list with remote data.
     * - Download new and remotely modified videos
     * - Upload locally modified videos
     * - Resolve conflicts (merge)
     * Note: This operation is slow and should be only called from a background thread.
     */
    public void refreshOnline() {

        List<OptimizedVideo> videos = new ArrayList<>();

        // Add the local videos
        // TODO: These can be cached
        File[] localFiles = localRoot.listFiles();
        for (File file : localFiles) {

            try {
                Video video = readVideoFromFile(file);
                videos.add(new OptimizedVideo(video));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Set<UUID> addedVideoIds = new HashSet<>();

        for (VideoHost host : cloudHosts) {

            List<VideoInfoRepository.FindResult> results = null;

            try {
                results = host.getIndex();
            } catch (IOException e) {
                continue;
            }

            for (VideoInfoRepository.FindResult result : results) {

                UUID id = result.getId();

                File localFileOriginal = getOriginalCacheFile(id);
                File localFileModified = getModifiedCacheFile(id);

                Video video = null;

                if (localFileModified.exists()) {

                    // We have local modifications to the video, let's sync them online

                    boolean didUpload = false;
                    Video resultVideo = null;

                    Video originalVideo;
                    Video modifiedVideo;

                    try {
                        originalVideo = readVideoFromFile(localFileOriginal);
                        modifiedVideo = readVideoFromFile(localFileModified);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }

                    for (int tries = 0; tries < 10; tries++) {

                        Video cloudVideo;

                        try {
                            cloudVideo = host.downloadVideoManifest(id);
                            cloudVideo.setRepository(this);
                        } catch (IOException e) {
                            // These tries refer to only merge tries, if the downloading fails
                            // just quit instead of trying again
                            break;
                        }

                        String cloudTag = cloudVideo.getVersionTag();

                        Video videoToUpload;
                        if (cloudTag.equals(originalVideo.getVersionTag())) {
                            // The online video is still the one we started editing from, so we
                            // can safely upload the new one
                            videoToUpload = modifiedVideo;

                        } else {
                            // The online video has been updated since we started modifying it so
                            // we need to merge the changes.
                            // TODO: videoToUpload = Video.merge(cloudVideo, modifiedVideo, originalVideo);
                            videoToUpload = modifiedVideo;
                        }

                        // Actually try to replace the online video with the new one
                        VideoHost.ManifestUploadResult uploadResult;
                        try {
                            uploadResult = host.uploadVideoManifest(videoToUpload, cloudTag);
                        } catch (IOException e) {
                            // If the uploading fails we don't retry. The method should return null
                            // instead of throwing in the case of tag mismatch.
                            break;
                        }

                        // We are uploading and checking a tag so the video online can actually
                        // change between downloading it and uploading a new version. So the
                        // video is conditionally uploaded if the video online matches the one we
                        // downloaded. If it doesn't we download the new video and try again.
                        if (uploadResult != null) {
                            didUpload = true;
                            resultVideo = videoToUpload;
                            resultVideo.setVersionTag(uploadResult.versionTag);
                            break;
                        }
                    }

                    try {
                        if (didUpload) {
                            writeVideoToFile(resultVideo, localFileOriginal);
                            if (localFileModified.delete()) {
                                // For logging purposes thrown and caught
                                throw new IOException("Failed to delete modified video");
                            }

                            video = resultVideo;
                        } else {
                            video = modifiedVideo;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (!localFileOriginal.exists() ||
                        localFileOriginal.lastModified() < result.getLastModified()) {

                    try {
                        // Update the cached original video
                        Video cloudVideo = host.downloadVideoManifest(id);
                        writeVideoToFile(cloudVideo, localFileOriginal);

                        video = cloudVideo;
                    } catch (IOException e) {
                        // If it fails we can just skip this video and continue downloading the
                        // rest.
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // Just read the cached video file
                        video = readVideoFromFile(localFileOriginal);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (video != null) {
                    addedVideoIds.add(video.getId());
                    videos.add(new OptimizedVideo(video));
                }
            }
        }

        // Add the cached remote videos
        List<UUID> cacheIds = getCacheIds();
        for (UUID id : cacheIds) {
            if (addedVideoIds.contains(id)) {
                continue;
            }

            File original = getOriginalCacheFile(id);
            File modified = getModifiedCacheFile(id);

            File newest;
            if (modified.exists()) {
                newest = modified;
            } else {
                newest = original;
            }

            try {
                Video video = readVideoFromFile(newest);
                videos.add(new OptimizedVideo(video));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // TODO: Add also offline cached unavailable videos

        updateVideos(videos);
    }

    public void saveVideo(Video video) throws IOException {

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

        serializer.save(video, targetFile.toURI());

        // Do partial update
        if (!allVideos.containsKey(video.getId())) {
            allResults.add(new FindResult(video.getId(), new Date().getTime()));
        }

        allVideos.put(video.getId(), new OptimizedVideo(video));
        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    /**
     * Uploads the video manifest to some cloud host.
     * Note: The video data itself (and thumbnail) should be uploaded somewhere before this, since
     * their urls will be written in the manifest.
     */
    public void uploadVideo(Video video) throws IOException {

        VideoHost.ManifestUploadResult result = null;
        IOException firstException = null;
        for (VideoHost host : cloudHosts) {

            try {
                result = host.uploadVideoManifest(video, null);
                // Stop at the first uploader which succeeds.
                break;
            } catch (IOException e) {
                if (firstException == null){
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

        video.setVersionTag(result.versionTag);
        video.setManifestUri(result.url);

        UUID id = video.getId();

        File cacheFile = getOriginalCacheFile(id);
        writeVideoToFile(video, cacheFile);

        File localFile = getLocalVideoFile(id);
        if (!localFile.delete()) {
            // For logging purposes
            throw new IOException("Failed to delete local file");
        }
    }

    // Video repository API

    @Override
    public void refresh() throws IOException {
        refreshOffline();
    }

    @Override
    public void save(Video video) throws IOException {
        saveVideo(video);
        video.setRepository(this);
    }

    @Override
    public void save(OptimizedVideo video) throws IOException {
        saveVideo(video.inflate());
        video.setRepository(this);
    }

    @Override
    public void delete(UUID id) throws IOException {
        // TODO
    }

    @Override
    public FindResults getAll() throws IOException {
        return new FindResults(allResults);
    }

    @Override
    public FindResults getByGenreString(String genre) throws IOException {
        return new FindResults(allResults);
    }

    @Override
    public OptimizedVideo getVideo(UUID id) throws IOException {
        return allVideos.get(id);
    }

    @Override
    public void invalidate(UUID id) {
    }

    @Override
    public void invalidateAll() {
    }
}

