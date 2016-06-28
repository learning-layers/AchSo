package fi.aalto.legroup.achso.authoring;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;

import android.net.Uri;
import java.util.ArrayList;

public class ExportHelper {
    private JsonSerializer serializer;
    private final Uri endpointUri;

    public static class ExportPayload implements JsonSerializable {
        public String email;
        public ArrayList<Video> videos;
    }

    public static class ExportResponse implements JsonSerializable {
        public String message;
    }

    public ExportHelper(JsonSerializer serializer, Uri endpointUri) {
        this.serializer = serializer;
        this.endpointUri = endpointUri;
    }

}
