package fi.aalto.legroup.achso.upload;

import com.squareup.otto.Bus;

import fi.aalto.legroup.achso.entities.Video;

/**
 * @author Leo Nikkilä
 */
public abstract class Uploader {

    protected Bus bus;

    private Uploader next;

    /**
     * Constructs the uploader.
     *
     * @param bus Event bus that should receive uploader events.
     */
    protected Uploader(Bus bus) {
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
     * Returns true if the chain should be broken when this uploader fails, false otherwise.
     */
    protected abstract boolean isCritical();

    /**
     * @param next The next uploader in the chain or null to break the chain after this.
     */
    public final void setNext(Uploader next) {
        this.next = next;
    }

    /**
     * Starts uploading using this uploader. Propagates further along the chain afterwards.
     *
     * @param video Video whose data will be uploaded.
     */
    public final void upload(Video video) {
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
            next.upload(video);
        }
    }

}
