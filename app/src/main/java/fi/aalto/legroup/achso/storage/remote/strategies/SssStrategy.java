package fi.aalto.legroup.achso.storage.remote.strategies;

import android.accounts.Account;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.otto.Bus;

import java.io.IOException;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.Video;

/**
 * Uploads metadata from videos to a Social Semantic Server instance.
 *
 * TODO: Extract API stuff into an API wrapper.
 */
public class SssStrategy extends Strategy {

    protected final Uri endpointUrl;

    /**
     * Constructs the strategy.
     *
     * @param endpointUrl the endpoint for the metadata post request
     */
    public SssStrategy(Bus bus, Uri endpointUrl) {
        super(bus);
        this.endpointUrl = endpointUrl;
    }

    /**
     * Uploads the data of a video. Must call listener.onUploadStart() when the upload starts,
     * onUploadFinish() when done and onUploadError() if an error occurs. Calling onUploadProgress()
     * is optional.
     *
     * @param video the video whose data will be uploaded
     */
    @Override
    public void handle(Video video) throws Exception {
        try {
            String videoId = createVideo(video);

            for (Annotation annotation : video.getAnnotations()) {
                createAnnotation(videoId, annotation);
            }
        } catch (Exception e) {
            throw new Exception("Couldn't upload to SSS: " + e.getMessage(), e);
        }
    }

    /**
     * Returns true if the chain should be broken when this strategy fails, false otherwise.
     */
    @Override
    protected boolean isCritical() {
        return true;
    }

    /**
     * Stores the metadata of a video publicly.
     *
     * @param video Video whose metadata should be written.
     * @return Identifier of the created video.
     * @throws IOException
     */
    protected String createVideo(Video video) throws IOException {
        JsonObject parameters = new JsonObject();

        parameters.addProperty("uuid", video.getId().toString());
        parameters.addProperty("link", video.getVideoUri().toString());
        parameters.addProperty("label", video.getTitle());
        parameters.addProperty("genre", video.getGenre());

        // Date#getTime returns microseconds since the epoch but a UNIX timestamp is needed, i.e.
        // milliseconds since the epoch.
        parameters.addProperty("creationTime", video.getDate().getTime() / 1000);

        Location location = video.getLocation();

        if (location != null) {
            parameters.addProperty("latitude", location.getLatitude());
            parameters.addProperty("longitude", location.getLongitude());
            parameters.addProperty("accuracy", location.getAccuracy());
        }

        Uri url = endpointUrl.buildUpon().appendPath("videos").build();

        String videoUriString = post(url, parameters).get("video").getAsString();
        Uri videoUri = Uri.parse(videoUriString);

        return videoUri.getLastPathSegment();
    }

    /**
     * Stores the metadata of an annotation.
     *
     * @param videoId Identifier of the video to which the annotation should be linked.
     * @param annotation Annotation to write.
     * @return Identifier of the created annotation.
     * @throws IOException
     */
    protected String createAnnotation(String videoId, Annotation annotation) throws IOException {
        JsonObject parameters = new JsonObject();

        parameters.addProperty("label", annotation.getText());
        parameters.addProperty("timePoint", annotation.getTime());
        parameters.addProperty("x", annotation.getPosition().x);
        parameters.addProperty("y", annotation.getPosition().y);

        Uri url = endpointUrl.buildUpon()
                .appendPath("videos")
                .appendPath("videos")
                .appendPath(videoId)
                .appendPath("annotations")
                .build();

        return post(url, parameters).get("annotation").getAsString();
    }

    private JsonObject post(Uri url, JsonObject parameters) throws IOException {
        return makeRequest("POST", url, parameters);
    }

    private JsonObject makeRequest(String method, Uri url, @Nullable JsonObject parameters)
            throws IOException {

        RequestBody requestBody = null;

        if (parameters != null) {
            requestBody = RequestBody.create(MediaType.parse("application/json"),
                    parameters.toString());
        }

        Request request = new Request.Builder()
                .url(url.toString())
                .header("Accept", "application/json")
                .method(method, requestBody)
                .build();

        Account account = App.loginManager.getAccount();
        Response response = App.authenticatedHttpClient.execute(request, account);

        String responseBody = response.body().string();
        JsonObject body = new JsonParser().parse(responseBody).getAsJsonObject();

        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();

            if (body.has("message")) {
                errorMessage += ": " + body.get("message").getAsString();
            }

            throw new IOException(errorMessage);
        }

        return body.getAsJsonObject();
    }

}
