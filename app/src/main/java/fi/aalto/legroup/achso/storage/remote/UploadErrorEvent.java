package fi.aalto.legroup.achso.storage.remote;

import java.util.UUID;

import fi.aalto.legroup.achso.storage.remote.strategies.Strategy;

/**
 * @author Leo Nikkilä
 */
public class UploadErrorEvent {

    private Strategy strategy;
    private UUID videoId;
    private String errorMessage;

    public UploadErrorEvent(Strategy strategy, UUID videoId, String errorMessage) {
        this.strategy = strategy;
        this.videoId = videoId;
        this.errorMessage = errorMessage;
    }

    public Strategy getStrategy() {
        return this.strategy;
    }

    public UUID getVideoId() {
        return this.videoId;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

}
