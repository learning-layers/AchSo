package fi.aalto.legroup.achso.upload;

import com.squareup.otto.Bus;

import fi.aalto.legroup.achso.entities.Video;

/**
 * @author Leo Nikkilä
 */
public class DisabledUploader extends Uploader {

    /**
     * Constructs the uploader.
     *
     * @param bus Event bus that should receive uploader events.
     */
    protected DisabledUploader(Bus bus) {
        super(bus);
    }

    /**
     * Uploads the data of a video in a blocking fashion.
     *
     * @param video Video whose data will be uploaded
     * @throws Exception On failure, with a user-friendly error message.
     */
    @Override
    public void handle(Video video) throws Exception {
        String error = "Uploading is temporarily disabled; will be enabled in a future version.";
        throw new Exception(error);
    }

    /**
     * Returns true if the chain should be broken when this uploader fails, false otherwise.
     */
    @Override
    protected boolean isCritical() {
        return true;
    }

}
