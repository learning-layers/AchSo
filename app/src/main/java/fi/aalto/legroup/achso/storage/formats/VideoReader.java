package fi.aalto.legroup.achso.storage.formats;

import java.io.IOException;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoInfo;

/**
 * @author Leo Nikkilä
 */
public interface VideoReader<T> {

    public VideoInfo readInfo(T source) throws IOException;

    public Video read(T source) throws IOException;

}
