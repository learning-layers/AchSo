package fi.aalto.legroup.achso.storage.local;

import android.net.Uri;

import com.google.common.io.Files;
import com.squareup.otto.Bus;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoInfo;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.AbstractVideoRepository;
import fi.aalto.legroup.achso.storage.VideoRepositoryUpdatedEvent;

public class LocalVideoRepository extends AbstractVideoRepository {

    protected JsonSerializer serializer;
    protected File storageDirectory;

    public LocalVideoRepository(Bus bus, JsonSerializer serializer, File storageDirectory) {
        super(bus);

        this.serializer = serializer;
        this.storageDirectory = storageDirectory;
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

        File[] manifests = storageDirectory.listFiles(new ManifestFileFilter());

        if (manifests == null) {
            throw new IOException("Couldn't list files in " + storageDirectory);
        }

        ArrayList<FindResult> results = new ArrayList<>(manifests.length);
        for (File manifest : manifests) {
            results.add(new FindResult(getIdFromManifest(manifest), manifest.lastModified()));
        }

        return new FindResults(results);
    }

    @Override
    public long getLastModifiedTime(UUID id) throws IOException {

        File manifest = getManifestFromId(id);
        return manifest.lastModified();
    }

    @Override
    public VideoInfo getVideoInfo(UUID id) throws IOException {

        File manifest = getManifestFromId(id);
        VideoInfo videoInfo = serializer.load(VideoInfo.class, manifest.toURI());

        videoInfo.setManifestUri(Uri.fromFile(manifest));

        return videoInfo;
    }

    @Override
    public Video getVideo(UUID id) throws IOException {

        File manifest = getManifestFromId(id);
        Video video = serializer.load(Video.class, manifest.toURI());

        video.setManifestUri(Uri.fromFile(manifest));
        video.setRepository(this);

        return video;
    }

    @Override
    public void save(Video video) throws IOException {
        serializer.save(video, getManifestFromId(video.getId()).toURI());
        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    @Override
    public void delete(UUID id) throws IOException {
        File manifest = getManifestFromId(id);

        if (manifest.delete()) {
            bus.post(new VideoRepositoryUpdatedEvent(this));
        } else {
            throw new IOException("Could not delete " + manifest);
        }
    }

    protected final class ManifestFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File directory, String fileName) {
            return Files.getFileExtension(fileName).equals("json");
        }
    }
}

