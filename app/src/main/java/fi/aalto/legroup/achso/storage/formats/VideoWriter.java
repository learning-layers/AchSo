package fi.aalto.legroup.achso.storage.formats;

import java.io.IOException;

import fi.aalto.legroup.achso.entities.Video;

/**
 * @author Leo Nikkilä
 */
public interface VideoWriter<T> {

    public void write(Video video, T destination) throws IOException;

}
