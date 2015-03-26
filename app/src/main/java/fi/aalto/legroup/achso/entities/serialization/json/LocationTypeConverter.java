package fi.aalto.legroup.achso.entities.serialization.json;

import android.location.Location;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public final class LocationTypeConverter implements JsonSerializer<Location>,
        JsonDeserializer<Location> {

    private static final String LATITUDE_KEY = "latitude";
    private static final String LONGITUDE_KEY = "longitude";
    private static final String ACCURACY_KEY = "accuracy";

    @Override
    public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        object.addProperty(LATITUDE_KEY, src.getLatitude());
        object.addProperty(LONGITUDE_KEY, src.getLongitude());
        object.addProperty(ACCURACY_KEY, src.getAccuracy());

        return object;
    }

    @Override
    public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject object = (JsonObject) json;

        Location location = new Location("deserialized");

        location.setLatitude(object.get(LATITUDE_KEY).getAsDouble());
        location.setLongitude(object.get(LONGITUDE_KEY).getAsDouble());
        location.setAccuracy(object.get(ACCURACY_KEY).getAsFloat());

        return location;
    }

}
