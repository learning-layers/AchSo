package fi.aalto.legroup.achso.storage;

/**
 * TODO: Split this event into something more specific.
 *
 * @author Leo Nikkilä
 */
public class VideoRepositoryUpdatedEvent {

    private VideoRepository repository;

    public VideoRepositoryUpdatedEvent(VideoRepository repository) {
        this.repository = repository;
    }

    public VideoRepository getRepository() {
        return this.repository;
    }

}
