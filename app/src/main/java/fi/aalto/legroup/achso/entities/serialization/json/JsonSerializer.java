package fi.aalto.legroup.achso.entities.serialization.json;

import android.location.Location;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

import javax.annotation.Nonnull;

import fi.aalto.legroup.achso.entities.serialization.Serializer;

public final class JsonSerializer extends Serializer<JsonSerializable> {

    private Gson gson;

    public JsonSerializer() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriTypeConverter())
                .registerTypeAdapter(Location.class, new LocationTypeConverter())
                .registerTypeAdapter(Date.class, new DateTypeConverter())
                .create();
    }

    /**
     * De-serialises a new object of the given type from the input stream.
     *
     * @param type  Type of the de-serialised object.
     * @param input Stream with the serialised representation.
     * @return The de-serialised object.
     * @throws IOException If the source cannot be read.
     */
    @Nonnull
    @Override
    public <T extends JsonSerializable> T read(Class<T> type, InputStream input)
            throws IOException {

        InputStreamReader reader = null;

        try {
            reader = new InputStreamReader(input);

            T object = gson.fromJson(reader, type);

            if (object == null) {
                throw new IOException("Got EOF when trying to read " + input);
            }

            return object;
        } catch (Exception e) {
            throw new IOException("Could not read " + type.toString(), e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * Serialises an object into the output stream.
     *
     * @param object Object to serialise.
     * @param output Stream to use for outputting the serialisation.
     * @throws IOException If the destination cannot be written to.
     */
    @Override
    public void write(JsonSerializable object, OutputStream output) throws IOException {
        OutputStreamWriter writer = null;

        try {
            writer = new OutputStreamWriter(output);
            gson.toJson(object, writer);
        } catch (Exception e) {
            throw new IOException("Could not write " + object, e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Serialises an object into a string.
     *
     * @param object Object to serialise.
     */
    @Override
    public String write(JsonSerializable object) {
        return gson.toJson(object);
    }

}
