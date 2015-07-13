package fi.aalto.legroup.achso.storage.remote.strategies;

import android.net.Uri;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoReference;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.remote.VideoHost;

public class AchRailsStrategy implements VideoHost {

    class JsonVideoReference implements JsonSerializable {
        public String uuid;
        public String last_modified;
    }
    class JsonVideoReferences implements JsonSerializable {
        JsonVideoReference[] videos;
    }

    private JsonSerializer serializer;
    private Uri endpointUrl;

    public AchRailsStrategy(JsonSerializer serializer, Uri endpointUrl) {
        this.serializer = serializer;
        this.endpointUrl = endpointUrl;
    }

    Request.Builder buildRequest() {
        return new Request.Builder()
            .url(endpointUrl.buildUpon().appendPath("videos.json").toString())
            .header("Authorization", Credentials.basic("test.user@example.com", "testuser"));
    }
    Request.Builder buildRequest(UUID id) {
        return new Request.Builder()
            .url(endpointUrl.buildUpon().appendPath("videos").appendPath(id.toString() + ".json").toString())
            .header("Authorization", Credentials.basic("test.user@example.com", "testuser"));
    }

    private Response executeRequestNoFail(Request request) throws IOException {
        // TODO: Switch to authenticatedHttpClient for OIDC
        return App.httpClient.newCall(request).execute();
    }

    private Response validateResponse(Response response) throws IOException {
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }
        return response;
    }

    private Response executeRequest(Request request) throws IOException {

        return validateResponse(executeRequestNoFail(request));
    }

    @Override
    public List<VideoReference> getIndex() throws IOException {
        Request request = buildRequest().get().build();
        Response response = executeRequest(request);
        JsonVideoReferences videos = serializer.read(JsonVideoReferences.class,
                response.body().byteStream());
        List<VideoReference> references = new ArrayList<>(videos.videos.length);
        for (JsonVideoReference video : videos.videos) {
            references.add(new VideoReference(UUID.fromString(video.uuid),
                    Date.parse(video.last_modified)));
        }
            return references;
    }

    @Override
    public Video downloadVideoManifest(UUID id) throws IOException {
        return null;
    }

    @Override
    public ManifestUploadResult uploadVideoManifest(Video video,
            String expectedVersionTag) throws IOException {
        return null;
    }

    @Override
    public void deleteVideoManifest(UUID id) throws IOException {

    }
}
