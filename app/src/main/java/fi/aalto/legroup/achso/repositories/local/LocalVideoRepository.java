package fi.aalto.legroup.achso.repositories.local;

import com.squareup.otto.Bus;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.repositories.VideoRepository;
import fi.aalto.legroup.achso.repositories.VideoRepositoryUpdatedEvent;
import fi.aalto.legroup.achso.serialization.json.JsonSerializerService;

/**
 * @author Leo Nikkilä
 */
public final class LocalVideoRepository extends AbstractLocalVideoRepository
        implements VideoRepository {

    private Bus bus;

    public LocalVideoRepository(Bus bus, JsonSerializerService serializer, File storageDirectory) {
        super(serializer, storageDirectory);

        this.bus = bus;
    }

    /**
     * Returns an entity with the given ID.
     */
    @Override
    public Video get(UUID id) throws IOException {
        Video video = serializer.load(Video.class, getManifestFromId(id).toURI());

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
