package fi.aalto.legroup.achso.entities.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

}
