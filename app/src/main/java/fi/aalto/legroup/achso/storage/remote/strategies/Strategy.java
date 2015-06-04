package fi.aalto.legroup.achso.storage.remote.strategies;

import com.squareup.otto.Bus;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.remote.UploadErrorEvent;

public abstract class Strategy {

    protected Bus bus;

    /**
     * Constructs the strategy.
     *
     * @param bus Event bus that should receive strategy events.
     */
    protected Strategy(Bus bus) {
        this.bus = bus;
    }

}
