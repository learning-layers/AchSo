package fi.aalto.legroup.achso.entities.serialization.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.joda.time.DateTime;

import java.lang.reflect.Type;
import java.util.Date;

public final class DateTypeConverter implements JsonSerializer<Date>, JsonDeserializer<Date> {

    @Override
    public JsonElement serialize(Date src, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(new DateTime(src).toString());
    }

    @Override
    public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        return new DateTime(json.getAsString()).toDate();
    }

}
