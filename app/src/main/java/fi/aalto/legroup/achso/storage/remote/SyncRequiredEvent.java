package fi.aalto.legroup.achso.storage.remote;

import fi.aalto.legroup.achso.storage.VideoRepository;

public class SyncRequiredEvent {
    private final VideoRepository repository;

    public SyncRequiredEvent(VideoRepository repository) {
        this.repository = repository;
    }

    public VideoRepository getRepository() {
        return repository;
    }
}

