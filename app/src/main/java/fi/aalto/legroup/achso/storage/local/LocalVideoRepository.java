package fi.aalto.legroup.achso.storage.local;

import android.net.Uri;

import com.squareup.otto.Bus;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.VideoRepository;
import fi.aalto.legroup.achso.storage.VideoRepositoryUpdatedEvent;

/**
 * @author Leo Nikkilä
 */
public final class LocalVideoRepository extends AbstractLocalVideoRepository
        implements VideoRepository {

    private Bus bus;

    public LocalVideoRepository(Bus bus, JsonSerializer serializer, File storageDirectory) {
        super(serializer, storageDirectory);

        this.bus = bus;
    }

    /**
     * Returns an entity with the given ID.
     */
    @Override
    public Video get(UUID id) throws IOException {
        File manifest = getManifestFromId(id);
        Video video = serializer.load(Video.class, manifest.toURI());

        video.setManifestUri(Uri.fromFile(manifest));
        video.setRepository(this);

        return video;
    }

    /**
     * Persists an entity with the given ID, overwriting a previous one if set.
     */
    @Override
    public void save(Video video) throws IOException {
        serializer.save(video, getManifestFromId(video.getId()).toURI());
        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    /**
     * Deletes an entity with the given ID.
     */
    @Override
    public void delete(UUID id) throws IOException {
        File manifest = getManifestFromId(id);

        if (manifest.delete()) {
            bus.post(new VideoRepositoryUpdatedEvent(this));
        } else {
            throw new IOException("Could not delete " + manifest);
        }
    }

}
