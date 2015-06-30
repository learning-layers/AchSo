package fi.aalto.legroup.achso.entities.serialization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.annotation.Nonnull;

/**
 * A service for serialising objects.
 */
public abstract class Serializer<S extends Serializable> {

    /**
     * De-serialises a new object of the given type from the input stream.
     *
     * @param type  Type of the de-serialised object.
     * @param input Stream with the serialised representation.
     * @param <T>   Type of the returned object.
     * @return The de-serialised object.
     * @throws IOException If the source cannot be read.
     */
    @Nonnull
    public abstract <T extends S> T read(Class<T> type, InputStream input) throws IOException;

    /**
     * Serialises an object into the output stream.
     *
     * @param object Object to serialise.
     * @param output Stream to use for outputting the serialisation.
     * @throws IOException If the destination cannot be written to.
     */
    public abstract void write(S object, OutputStream output) throws IOException;

    /**
     * Serialises an object into a string.
     *
     * @param object Object to serialise.
     */
    public abstract String write(S object);

    /**
     * De-serialises a new object of the given type from the given URI.
     *
     * @param type  Type of the de-serialised object.
     * @param input URI with the serialised representation.
     * @param <T>   Type of the returned object.
     * @return The de-serialised object.
     * @throws IOException If the source cannot be read.
     */
    @Nonnull
    public <T extends S> T load(Class<T> type, URI input) throws IOException {
        InputStream stream = null;

        try {
            stream = input.toURL().openStream();
            return read(type, stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Serialises an object into the given (local) file.
     *
     * @param object Object to serialise.
     * @param output Local file to use for outputting the serialisation.
     * @throws IOException If the destination cannot be written to.
     */
    public void save(S object, URI output) throws IOException {
        OutputStream stream = null;

        try {
            stream = new FileOutputStream(new File(output));
            write(object, stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

}
