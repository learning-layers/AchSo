package fi.aalto.legroup.achso.storage.remote.download;

import java.util.UUID;

import fi.aalto.legroup.achso.storage.remote.TransferErrorEvent;

public class DownloadErrorEvent extends TransferErrorEvent {
    public DownloadErrorEvent(UUID videoId, String errorMessage) {
        super(videoId, errorMessage);
    }
}
