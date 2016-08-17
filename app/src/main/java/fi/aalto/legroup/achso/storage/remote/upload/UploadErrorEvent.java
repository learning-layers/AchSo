package fi.aalto.legroup.achso.storage.remote.upload;

import java.util.UUID;

import fi.aalto.legroup.achso.storage.remote.TransferErrorEvent;

public class UploadErrorEvent extends TransferErrorEvent {

    public UploadErrorEvent(UUID videoId, String errorMessage) {
        super(videoId, errorMessage);
    }
}
