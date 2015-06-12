package fi.aalto.legroup.achso.storage;

import java.io.IOException;
import java.util.List;

import fi.aalto.legroup.achso.entities.OptimizedVideo;

/**
 * Provides full read/write access to video root entities.
 */
public interface BlockingVideoSource extends VideoSource {

    /**
     * Returns a list all of the videos from the source.
     */
    public List<OptimizedVideo> updateBlocking(List<OptimizedVideo> nonBlockingResult) throws
            IOException;


}

