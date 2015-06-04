package fi.aalto.legroup.achso.storage.local;

import android.net.Uri;

import com.google.common.io.Files;
import com.squareup.otto.Bus;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.OptimizedVideo;
import fi.aalto.legroup.achso.entities.PooledVideo;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.AbstractVideoRepository;
import fi.aalto.legroup.achso.storage.VideoRepositoryUpdatedEvent;

public class OptimizedLocalVideoRepository extends AbstractVideoRepository {

    private HashMap<UUID, OptimizedVideo> videos;
    private ArrayList<FindResult> allResults;
    private static final PooledVideo savePool = new PooledVideo();

    protected JsonSerializer serializer;
    protected File storageDirectory;

    public OptimizedLocalVideoRepository(Bus bus, JsonSerializer serializer, File storageDirectory) {
        super(bus);

        this.serializer = serializer;
        this.storageDirectory = storageDirectory;
    }

    public void refresh() throws IOException {

        File[] manifests = storageDirectory.listFiles(new ManifestFileFilter());

        if (manifests == null) {
            throw new IOException("Couldn't list files in " + storageDirectory);
        }

        videos = new HashMap<>(manifests.length * 2);
        allResults = new ArrayList<>(manifests.length);
        for (File manifest : manifests) {
            
            Video video = null;
            try {
                video = serializer.load(Video.class, manifest.toURI());
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            video.setManifestUri(Uri.fromFile(manifest));
            video.setRepository(this);
            video.setLastModified(new Date(manifest.lastModified()));

            OptimizedVideo optimized = new OptimizedVideo(video);
            videos.put(optimized.getId(), optimized);

            allResults.add(new FindResult(getIdFromManifest(manifest), manifest.lastModified()));
        }
    }

    protected File getManifestFromId(UUID id) {
        return new File(storageDirectory, id.toString() + ".json");
    }

    private UUID getIdFromManifest(File manifest) {
        String basename = Files.getNameWithoutExtension(manifest.getName());
        return UUID.fromString(basename);
    }

    @Override
    public FindResults getAll() throws IOException {
        return new FindResults(allResults);
    }

    @Override
    public OptimizedVideo getVideo(UUID id) {
        return videos.get(id);
    }

    @Override
    public void save(Video video) throws IOException {
        videos.put(video.getId(), new OptimizedVideo(video));

        serializer.save(video, getManifestFromId(video.getId()).toURI());

        // FIXME: This is stupid
        refresh();
        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    @Override
    public void save(OptimizedVideo video) throws IOException {
        save(video.inflate(savePool));
        savePool.release();
    }

    @Override
    public void delete(UUID id) throws IOException {
        File manifest = getManifestFromId(id);

        if (manifest.delete()) {

            // FIXME: This is stupid
            refresh();

            bus.post(new VideoRepositoryUpdatedEvent(this));
        } else {
            throw new IOException("Could not delete " + manifest);
        }
    }

    protected final static class ManifestFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File directory, String fileName) {
            return Files.getFileExtension(fileName).equals("json");
        }
    }
}
