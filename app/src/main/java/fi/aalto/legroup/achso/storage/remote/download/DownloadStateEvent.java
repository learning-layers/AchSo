package fi.aalto.legroup.achso.storage.remote.download;

import java.util.UUID;

import fi.aalto.legroup.achso.storage.remote.TransferStateEvent;

public class DownloadStateEvent  extends TransferStateEvent {
    public DownloadStateEvent(UUID videoId, Type type) {
        super(videoId, type);
    }
}
