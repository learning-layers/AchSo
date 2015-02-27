package fi.aalto.legroup.achso.entities.serialization.json;

import android.net.Uri;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public final class UriTypeConverter implements JsonSerializer<Uri>, JsonDeserializer<Uri> {

    @Override
    public JsonElement serialize(Uri src, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public Uri deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        return Uri.parse(json.getAsString());
    }

}
