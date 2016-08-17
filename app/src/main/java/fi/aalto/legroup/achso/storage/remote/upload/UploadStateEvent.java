package fi.aalto.legroup.achso.storage.remote.upload;

import java.util.UUID;

import fi.aalto.legroup.achso.storage.remote.TransferStateEvent;

public class UploadStateEvent extends TransferStateEvent {

    public UploadStateEvent(UUID videoId, Type type) {
        super(videoId, type);
    }
}
