package fi.aalto.legroup.achso.authoring;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;

import android.accounts.Account;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Patterns;

import java.io.IOException;
import java.util.ArrayList;

public class ExportHelper {
    private JsonSerializer serializer;
    private final Uri endpointUri;

    // Payload that is sent to the AchSo!-video exporter
    // See here: https://github.com/melonmanchan/achso-video-exporter#example-payload
    public static class ExportPayload implements JsonSerializable {
        public String email;
        public ArrayList<Video> videos;

        public ExportPayload(String email, ArrayList<Video> videos) {
            this.email  = email;
            this.videos = videos;
        }
    }

    // Response received from the exporter service
    public static class ExportResponse implements JsonSerializable {
        public String message;

        public ExportResponse(String message) {
            this.message = message;
        }
    }

    // Asynchronous task for initializing the video export process
    public static class ExportVideosTask extends AsyncTask<ExportPayload, Void, Void> {
        private ExportCallback callback;

        public ExportVideosTask(ExportCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(ExportPayload... params) {
            try {
                ExportResponse response = App.exportHelper.exportVideos(params[0]);
                callback.success(response);
            } catch (IOException ex) {
                callback.failure(ex.getMessage());
            }
            return null;
        }
    }

    public interface ExportCallback {
        void success(ExportResponse response);
        void failure(String reason);
    }

    public ExportHelper(JsonSerializer serializer, Uri endpointUri) {
        this.serializer = serializer;
        this.endpointUri = endpointUri;
    }

    public ExportResponse exportVideos(ExportPayload exportPayload) throws IOException {
        if (!isValidEmail(exportPayload.email)) {
            throw new IOException("Invalid email address: " + exportPayload.email);
        }

        String serializedPayload = serializer.write(exportPayload);

        Request request = new Request.Builder()
                .url(endpointUri.toString())
                .post(RequestBody.create(MediaType.parse("application/json"), serializedPayload))
                .build();


        Account account = App.loginManager.getAccount();
        Response response = App.authenticatedHttpClient.execute(request, account);

        if (!response.isSuccessful()) {
            throw new IOException(response.body().string());
        }

        ExportResponse result = serializer.read(ExportResponse.class, response.body().byteStream());

        return result;
    }

    // http://stackoverflow.com/questions/1819142/how-should-i-validate-an-e-mail-address
    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
