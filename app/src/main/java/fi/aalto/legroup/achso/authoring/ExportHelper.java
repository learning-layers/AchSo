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

    public static class ExportPayload implements JsonSerializable {
        public String email;
        public ArrayList<Video> videos;

        public ExportPayload(String email, ArrayList<Video> videos) {
            this.email  = email;
            this.videos = videos;
        }
    }

    public static class ExportResponse implements JsonSerializable {
        public String message;

        public ExportResponse(String message) {
            this.message = message;
        }
    }

    public static class ExportVideosTask extends AsyncTask<ExportPayload, Void, Void> {
        private ExportCallback callback;

        public ExportVideosTask(ExportCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(ExportPayload... params) {
            try {
                ExportResponse response = App.exportHelper.exportVideos(params[0].videos, params[0].email);
                callback.success(response);
            } catch (IOException ex) {
                callback.failure(ex.getMessage());
            }
            return null;
        }
    }

    public static class ExportCallback {
        public void success(ExportResponse response) {
            System.out.println(response.message);
        }

        public void failure(String reason) {
            System.out.println(reason);
        }
    }

    public ExportHelper(JsonSerializer serializer, Uri endpointUri) {
        this.serializer = serializer;
        this.endpointUri = endpointUri;
    }

    public ExportResponse exportVideos(ArrayList<Video> videos, String email) throws IOException {
        if (!isValidEmail(email)) {
            throw new IOException("Invalid email address: " + email);
        }

        ExportPayload exportPayload = new ExportPayload(email, videos);

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
