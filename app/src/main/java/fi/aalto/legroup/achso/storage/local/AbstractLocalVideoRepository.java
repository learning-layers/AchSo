package fi.aalto.legroup.achso.storage.local;

import com.google.common.io.Files;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Comparator;
import java.util.UUID;

import fi.aalto.legroup.achso.storage.formats.mp4.Mp4Reader;
import fi.aalto.legroup.achso.storage.formats.mp4.Mp4Writer;

/**
 * An abstract local video repository that dictates the general behaviour.
 */
public abstract class AbstractLocalVideoRepository implements FilenameFilter {

    protected Mp4Reader reader;
    protected Mp4Writer writer;
    protected File storageDirectory;

    public AbstractLocalVideoRepository(Mp4Reader reader, Mp4Writer writer, File storageDirectory) {
        this.reader = reader;
        this.writer = writer;
        this.storageDirectory = storageDirectory;
    }

    /**
     * Returns the video file for the given ID.
     */
    public File getFile(UUID id) {
        return new File(storageDirectory, id + ".mp4");
    }

    /**
     * Returns the ID for the given video file.
     */
    public UUID getId(File file) throws IOException {
        String basename = Files.getNameWithoutExtension(file.getName());
        return UUID.fromString(basename);
    }

    /**
     * Accepts only .mp4 files when querying files in a directory.
     */
    @Override
    public boolean accept(File directory, String fileName) {
        return Files.getFileExtension(fileName).equals("mp4");
    }

    /**
     * Compares files based on their modification dates.
     */
    protected static final class DateModifiedComparator implements Comparator<File> {

        @Override
        public int compare(File left, File right) {
            // Room for improvement: this quick and dirty implementation calls File.lastModified()
            // even if the value has previously been retrieved for that file.

            long leftModified = left.lastModified();
            long rightModified = right.lastModified();

            if (leftModified > rightModified) {
                return 1;
            }

            if (leftModified < rightModified) {
                return -1;
            }

            return 0;
        }

    }

}
