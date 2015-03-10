package fi.aalto.legroup.achso.storage.remote.strategies;

import com.squareup.otto.Bus;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.remote.UploadErrorEvent;

public abstract class Strategy {

    protected Bus bus;

    private Strategy next;

    /**
     * Constructs the strategy.
     *
     * @param bus Event bus that should receive strategy events.
     */
    protected Strategy(Bus bus) {
        this.bus = bus;
    }

    /**
     * Uploads the data of a video in a blocking fashion.
     *
     * @param video Video whose data will be uploaded
     * @throws Exception On failure, with a user-friendly error message.
     */
    protected abstract void handle(Video video) throws Exception;

    /**
     * Returns true if the chain should be broken when this strategy fails, false otherwise.
     */
    protected abstract boolean isCritical();

    /**
     * @param next The next strategy in the chain or null to break the chain after this.
     */
    public final void setNext(Strategy next) {
        this.next = next;
    }

    /**
     * Starts uploading using this strategy. Propagates further along the chain afterwards.
     *
     * @param video Video whose data will be uploaded.
     */
    public final void execute(Video video) {
        try {
            handle(video);
        } catch (Exception e) {
            bus.post(new UploadErrorEvent(this, video.getId(), e.getMessage()));

            e.printStackTrace();

            if (isCritical()) {
                return;
            }
        }

        if (next != null) {
            next.execute(video);
        }
    }

}
