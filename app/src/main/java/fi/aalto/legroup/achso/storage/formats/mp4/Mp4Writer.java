package fi.aalto.legroup.achso.storage.formats.mp4;

import android.content.Context;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ChunkOffsetBox;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.MetaBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.StaticChunkOffsetBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.mdat.MediaDataBox;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.util.Path;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.formats.VideoWriter;

/**
 * @author Leo Nikkilä
 */
public final class Mp4Writer extends Mp4Parser implements VideoWriter<File> {

    private final JsonSerializer serializer;
    private final Context context;

    public Mp4Writer(Context context, JsonSerializer serializer) {
        this.context = context;
        this.serializer = serializer;
    }

    /**
     * Writes the video information into the file. It is assumed that the destination is an
     * existing MP4 file.
     */
    @Override
    public void write(Video video, File destination) throws IOException {
        // Serialise the video into JSON
        String manifest = serializer.write(video);

        File videoFile = new File(video.getVideoUri().getPath());
        File temporaryFile = getTemporaryFile();

        IsoFile sourceFile;
        FileOutputStream temporaryStream;

        // Android doesn't support try-with-resources, using a Closer as a poor man's substitute.
        // See https://code.google.com/p/guava-libraries/wiki/ClosingResourcesExplained#Closer
        Closer closer = Closer.create();

        try {
            sourceFile = closer.register(new IsoFile(new FileDataSourceImpl(videoFile)));
            temporaryStream = closer.register(new FileOutputStream(temporaryFile));

            long sizeDelta = writeManifest(sourceFile, manifest);

            if (needsOffsetAdjustment(sourceFile)) {
                adjustChunkOffsets(sourceFile, sizeDelta);
            }

            sourceFile.writeContainer(temporaryStream.getChannel());

            // This is really fast if the files are on the same file system, really slow otherwise.
            Files.move(temporaryFile, destination);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    /**
     * Returns a temporary file with the given file name.
     */
    private File getTemporaryFile() {
        File temporaryDirectory = context.getExternalCacheDir();

        // External cache directory is null if it's not available
        if (temporaryDirectory == null) {
            temporaryDirectory = context.getCacheDir();
        }

        String fileName = Long.toHexString(System.nanoTime());

        return new File(temporaryDirectory, fileName);
    }

    /**
     * Writes the given JSON manifest string into the file. If the file already has a manifest, it
     * will be overwritten.
     *
     * @return The number of bytes the file either grew or shrunk.
     */
    private long writeManifest(IsoFile isoFile, String manifest) {
        // The manifest is stored as UTF-8 encoded JSON inside a user-defined UUID box.
        //
        // (root)
        // └─ moov [MovieBox]
        //    └─ meta [MetaBox]
        //       ├─ hdlr (type = "null") [HandlerBox]
        //       └─ uuid (uuid = "388ed96f-47b1-499a-9e7e-ee304eb19661") [JsonManifestBox]
        //
        // There can only be one metadata box in a container, so if there is an existing box, we
        // append our content to it.

        long initialSize = 0;

        // Check for a pre-existing meta box
        MetaBox metaBox = Path.getPath(isoFile, "/moov/meta");

        if (metaBox == null) {
            metaBox = new MetaBox();
            isoFile.getMovieBox().addBox(metaBox);
        } else {
            initialSize = metaBox.getSize();
        }

        HandlerBox handlerBox = new HandlerBox();

        // Inside a `meta` box, a `null` handler indicates that the `meta` box is only used for
        // holding resources.
        handlerBox.setHandlerType("null");

        // ISO/IEC 14496-12: "There is a general handler for metadata streams of any type [...]. If
        // they are in text, then a MIME format is supplied to document their format [...]."
        //
        // The field that should be used is unclear. The name field is supposed to be
        // human-readable, but it's the only field that can hold variable-length data. Note that
        // the terminating null byte is required.
        handlerBox.setName("application/json\0");

        JsonManifestBox manifestBox = new JsonManifestBox(manifest);

        metaBox.addBox(handlerBox);
        metaBox.addBox(manifestBox);

        return metaBox.getSize() - initialSize;
    }

    /**
     * Returns whether the chunk offsets in the given file need to be adjusted if the `moov` box
     * length changes.
     *
     * @param file ISO file to be checked.
     */
    private boolean needsOffsetAdjustment(IsoFile file) {
        // The file might have two `mdat` boxes. We don't support this right now.
        if (file.getBoxes(MediaDataBox.class).size() > 1) {
            throw new IllegalArgumentException("Files with two `mdat` boxes are not supported!");
        }

        // The file might also contain a movie fragment. This is not supported either.
        if (file.getBoxes(MovieFragmentBox.class).size() > 0) {
            throw new IllegalArgumentException("Files with fragmented movies are not supported!");
        }

        // Offset correction is needed if the `moov` box is before the `mdat` box.
        for (Box box : file.getBoxes()) {
            if (box instanceof MovieBox) {
                return true;
            }

            if (box instanceof MediaDataBox) {
                return false;
            }
        }

        throw new IllegalArgumentException("Invalid file: no `moov` or `mdat` box.");
    }

    /**
     * Adjusts the chunk offsets in the given file to accommodate for the changed box length. This
     * does not support fragmented files or files with multiple media data boxes.
     *
     * @param isoFile       ISO file to adjust.
     * @param movieBoxDelta The amount the `moov` box length changed.
     */
    private void adjustChunkOffsets(IsoFile isoFile, long movieBoxDelta) {
        // Sample table boxes (`stbl`)
        List<SampleTableBox> sampleTableBoxes = isoFile.getBoxes(SampleTableBox.class, true);

        for (SampleTableBox sampleTableBox : sampleTableBoxes) {
            // Sample table boxes contain exactly one `stco` or `co64` box
            ChunkOffsetBox offsetBox = sampleTableBox.getChunkOffsetBox();
            long[] offsets = offsetBox.getChunkOffsets();

            // Adjust each offset according to the delta
            for (int i = 0; i < offsets.length; i++) {
                offsets[i] += movieBoxDelta;
            }

            // TODO: Check if the box needs to be 64-bit
            StaticChunkOffsetBox replacementOffsetBox = new StaticChunkOffsetBox();
            replacementOffsetBox.setChunkOffsets(offsets);

            sampleTableBox.getBoxes().remove(offsetBox);
            sampleTableBox.addBox(replacementOffsetBox);
        }
    }

}
