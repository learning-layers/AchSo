package fi.aalto.legroup.achso.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.OptimizedVideo;
import fi.aalto.legroup.achso.entities.Video;

public class VideoCollection implements VideoRepository {

    protected Map<UUID, OptimizedVideo> videoMap;
    protected List<FindResult> allResults;

    protected List<OptimizedVideo> persistentVideos;

    protected List<VideoSource> sources = new ArrayList<>();
    protected List<BlockingVideoSource> blockingSources = new ArrayList<>();
    protected Map<BlockingVideoSource, List<OptimizedVideo>> blockingSourceResultVideos = new HashMap<>();

    public void addSource(VideoSource source) {

        if (source instanceof BlockingVideoSource) {
            blockingSources.add((BlockingVideoSource)source);
        } else {
            sources.add(source);
        }
    }

    protected void setVideos(List<OptimizedVideo> videos) {
        Map<UUID, OptimizedVideo> newVideoMap = new HashMap<>(videos.size());
        List<FindResult> newAllResults = new ArrayList<>(videos.size());
        for (OptimizedVideo video : videos) {
            newVideoMap.put(video.getId(), video);
            newAllResults.add(new FindResult(video.getId(), video.getLastModified()));
        }

        synchronized (this) {
            videoMap = newVideoMap;
            allResults = newAllResults;
        }
    }

    public void updateCollectionNonBlocking() throws IOException {

        List<OptimizedVideo> newVideos = new ArrayList<OptimizedVideo>();
        List<OptimizedVideo> newPersistentVideos = new ArrayList<OptimizedVideo>();

        for (VideoSource source : sources) {
            List<OptimizedVideo> result = source.updateNonBlocking();
            newPersistentVideos.addAll(result);
            newVideos.addAll(result);
        }
        for (BlockingVideoSource source : blockingSources) {
            List<OptimizedVideo> result = source.updateNonBlocking();
            blockingSourceResultVideos.put(source, result);
            newVideos.addAll(result);
        }

        persistentVideos = newPersistentVideos;
        setVideos(newVideos);
    }

    public void updateCollectionBlocking() throws IOException {

        List<OptimizedVideo> newVideos = new ArrayList<OptimizedVideo>(persistentVideos);

        for (BlockingVideoSource source : blockingSources) {
            List<OptimizedVideo> oldResult = blockingSourceResultVideos.get(source);
            List<OptimizedVideo> newResult = source.updateBlocking(oldResult);
            newVideos.addAll(newResult);
        }

        setVideos(newVideos);
    }

    public FindResults getAll() throws IOException {
        return new FindResults(allResults);
    }

    public FindResults getByGenreString(String genre) throws IOException {

        FindResults results = getAll();
        ArrayList<FindResult> matching = new ArrayList<>(results.size());

        for (FindResult result : results) {

            OptimizedVideo video = getVideo(result.getId());
            if (video.getGenre().matches(genre)) {
                matching.add(result);
            }
        }
        matching.trimToSize();

        return new FindResults(matching);
    }

    public OptimizedVideo getVideo(UUID id) throws IOException {

        return videoMap.get(id);
    }

    public void invalidate(UUID id) {
    }

    public void invalidateAll() {
    }

    public void refresh() throws IOException {
    }

    public void save(Video video) throws IOException {
    }

    public void save(OptimizedVideo video) throws IOException {
    }

    public void delete(UUID id) throws IOException {
    }
}

