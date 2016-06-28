package fi.aalto.legroup.achso.authoring;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;

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

    public void exportVideos(ArrayList<Video> videos, String email) {
        if (!isValidEmail(email)) {
            return;
        }

    }

    // http://stackoverflow.com/questions/1819142/how-should-i-validate-an-e-mail-address
    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
