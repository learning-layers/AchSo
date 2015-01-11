package fi.aalto.legroup.achso.repositories.local;

import com.google.common.io.Files;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Comparator;
import java.util.UUID;

import fi.aalto.legroup.achso.serialization.json.JsonSerializerService;

/**
 * An abstract local video repository that dictates the general behaviour.
 *
 * @author Leo Nikkilä
 */
public abstract class AbstractLocalVideoRepository implements FilenameFilter {

    protected JsonSerializerService serializer;
    protected File storageDirectory;

    public AbstractLocalVideoRepository(JsonSerializerService serializer, File storageDirectory) {
        this.serializer = serializer;
        this.storageDirectory = storageDirectory;
    }

    /**
     * Returns the manifest file for the given ID.
     */
    public File getManifestFromId(UUID id) {
        return new File(storageDirectory, id.toString() + ".json");
    }

    /**
     * Returns the ID for the given manifest file.
     */
    public UUID getIdFromManifest(File manifest) {
        String basename = Files.getNameWithoutExtension(manifest.getName());
        return UUID.fromString(basename);
    }

    /**
     * Accepts only .json files when querying files in a directory.
     */
    @Override
    public boolean accept(File directory, String fileName) {
        return Files.getFileExtension(fileName).equals("json");
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

            if (leftModified > rightModified) return 1;
            if (leftModified < rightModified) return -1;

            return 0;
        }

    }

}
