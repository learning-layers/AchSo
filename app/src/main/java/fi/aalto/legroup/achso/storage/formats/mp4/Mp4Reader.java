package fi.aalto.legroup.achso.storage.formats.mp4;

import com.coremedia.iso.IsoFile;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.boxes.apple.AppleLongDescriptionBox;

import java.io.File;
import java.io.IOException;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoInfo;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.formats.VideoReader;

/**
 * @author Leo Nikkilä
 */
public final class Mp4Reader extends Mp4Parser implements VideoReader<File> {

    private final JsonSerializer serializer;

    public Mp4Reader(JsonSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public VideoInfo readInfo(File source) throws IOException {
        DataSource dataSource = new FileDataSourceImpl(source);
        IsoFile file = new IsoFile(dataSource);

        String manifest = readLongDescription(file);

        return serializer.read(VideoInfo.class, manifest);
    }

    @Override
    public Video read(File source) throws IOException {
        DataSource dataSource = new FileDataSourceImpl(source);
        IsoFile file = new IsoFile(dataSource);

        String manifest = readLongDescription(file);

        return serializer.read(Video.class, manifest);
    }

    /**
     * Returns the contents of the first `ldes` box in the given file, parsed as UTF-8.
     * @throws IOException If the file cannot be read.
     */
    private static String readLongDescription(IsoFile file) throws IOException {
        // TODO LOL
        AppleLongDescriptionBox box = null;

        if (box == null) {
            throw new IOException("Could not find an `ldes` box.");
        }

        return box.getValue();
    }

}
