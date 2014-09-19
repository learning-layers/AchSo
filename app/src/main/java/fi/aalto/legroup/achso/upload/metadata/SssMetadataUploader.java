package fi.aalto.legroup.achso.upload.metadata;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.util.App;

/**
 * Uploads metadata from videos to a Social Semantic Server instance.
 *
 * @author Leo Nikkilä
 */
public class SssMetadataUploader extends AbstractMetadataUploader {

    private final String TAG = getClass().getSimpleName();

    private String endpointUrl;

    /**
     * Constructs the uploader.
     *
     * @param endpointUrl    the endpoint for the metadata post request
     */
    public SssMetadataUploader(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    /**
     * Uploads the metadata of a video. Must call uploadListener.onMetadataUploadStart() when the
     * upload starts, onMetadataUploadFinish() when done and onMetadataUploadError() if an error
     * occurs.
     *
     * @param video the video whose metadata will be uploaded
     */
    @Override
    public void upload(SemanticVideo video) {
        JsonObject jsonRequest = new JsonObject();

        Log.w(TAG, "Upload not yet implemented, sending an empty request body!");

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                jsonRequest.toString());

        uploadListener.onMetadataUploadStart(video);

        Request request = new Request.Builder()
                .url(endpointUrl)
                .header("Accept", "application/json")
                .post(requestBody)
                .build();

        try {
            Response response = App.httpClient.newCall(request).execute();
            String jsonBody = response.body().string();
            JsonObject body = new JsonParser().parse(jsonBody).getAsJsonObject();

            if (response.isSuccessful()) {
                uploadListener.onMetadataUploadFinish(video);
            } else {
                String firstErrorMessage = body.getAsJsonArray("errorMsg").get(0).getAsString();
                uploadListener.onMetadataUploadError(video, firstErrorMessage);
            }
        } catch (Exception e) {
            uploadListener.onMetadataUploadError(video, "Could not upload metadata: " +
                    e.getMessage());
        }
    }

}
