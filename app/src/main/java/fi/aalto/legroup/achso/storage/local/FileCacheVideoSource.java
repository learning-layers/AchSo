package fi.aalto.legroup.achso.storage.local;

import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.OptimizedVideo;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.BlockingVideoSource;
import fi.aalto.legroup.achso.storage.VideoHost;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;

public class FileCacheVideoSource implements BlockingVideoSource {

    JsonSerializer serializer;
    VideoHost host;
    File root;

    public FileCacheVideoSource(JsonSerializer serializer, VideoHost host, File root) {
        this.serializer = serializer;
        this.host = host;
        this.root = root;
    }

    private File getCacheFile(UUID id) {
        return new File(root, id.toString() + ".json");
    }

    private List<File> getCachedFiles() {

        File[] manifests = root.listFiles();
        return Arrays.asList(manifests);

    }

    private Video readVideo(File file) throws IOException {

        Video video = serializer.load(Video.class, file.toURI());
        video.setManifestUri(Uri.fromFile(file));
        return video;
    }

    private void writeVideo(File file, Video video) throws IOException {

        serializer.save(video, file.toURI());
    }

    @Override
    public List<OptimizedVideo> updateNonBlocking() throws IOException {

        List<OptimizedVideo> videos = new ArrayList<>();

        // Only return the currently cached files
        List<File> cachedVideoFiles = getCachedFiles();
        for (File file : cachedVideoFiles) {

            Video video = readVideo(file);
            video.setLastModified(new Date(file.lastModified()));
            videos.add(new OptimizedVideo(video));
        }
        return videos;
    }

    @Override
    public List<OptimizedVideo> updateBlocking(List<OptimizedVideo> nonBlockingResult) throws IOException {

        Map<UUID, OptimizedVideo> results = new HashMap<>(nonBlockingResult.size());
        for (OptimizedVideo video : nonBlockingResult) {
            results.put(video.getId(), video);
        }

        Set<File> validFiles = new HashSet<>();

        List<OptimizedVideo> videos = new ArrayList<>();

        // Update the videos from the host
        List<VideoInfoRepository.FindResult> infos = host.getIndex();
        for (VideoInfoRepository.FindResult info : infos) {

            UUID id = info.getId();
            OptimizedVideo oldVideo = results.get(id);

            File cacheFile = getCacheFile(id);
            validFiles.add(cacheFile);

            if (oldVideo == null || oldVideo.getLastModified() < info.getLastModified()) {

                // Need to download and cache.
                Video video = host.downloadVideoManifest(id);
                video.setLastModified(new Date(info.getLastModified()));
                writeVideo(cacheFile, video);
                videos.add(new OptimizedVideo(video));
            }
        }

        // Purge non-existing videos
        List<File> cachedVideoFiles = getCachedFiles();
        for (File file : cachedVideoFiles) {

            if (!validFiles.contains(file)) {

                // This file is not anymore in the repository, so it can be deleted
                file.delete();
            }
        }
        
        return videos;
    }

    @Override
    public void saveVideo(Video video) throws IOException {

        host.uploadVideoManifest(video);
        
        File cacheFile = getCacheFile(video.getId());
        writeVideo(cacheFile, video);
    }

    @Override
    public void deleteVideo(UUID id) throws IOException {

        host.deleteVideoManifest(id);

        File cacheFile = getCacheFile(id);
        cacheFile.delete();
    }

}

