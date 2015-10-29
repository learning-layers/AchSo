package fi.aalto.legroup.achso.storage.remote.strategies;

import android.accounts.Account;
import android.net.Uri;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Group;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoReference;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.remote.VideoHost;

public class AchRailsStrategy implements VideoHost {

    class JsonVideoReference implements JsonSerializable {
        public String uuid;
        public int revision;
    }
    class JsonGroupList implements JsonSerializable {
        public List<Group> groups;
    }
    class JsonVideoReferences implements JsonSerializable {
        public JsonVideoReference[] videos;
    }

    private JsonSerializer serializer;
    private Uri endpointUrl;

    public AchRailsStrategy(JsonSerializer serializer, Uri endpointUrl) {
        this.serializer = serializer;
        this.endpointUrl = endpointUrl;
    }

    Request.Builder buildVideosRequest() {
        return new Request.Builder()
            .url(endpointUrl.buildUpon().appendPath("videos.json").toString());
    }
    Request.Builder buildVideosRequest(UUID id) {
        return new Request.Builder()
            .url(endpointUrl.buildUpon()
                    .appendPath("videos")
                    .appendPath(id.toString() + ".json")
                    .toString());
    }
    Request.Builder buildGroupsRequest() {
        return new Request.Builder()
                .url(endpointUrl.buildUpon().appendPath("groups.json").toString());
    }

    private Response executeRequestNoFail(Request request) throws IOException {
        Account account = App.loginManager.getAccount();
        return App.authenticatedHttpClient.execute(request, account);
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
        Request request = buildVideosRequest().get().build();
        Response response = executeRequest(request);
        JsonVideoReferences videos = serializer.read(JsonVideoReferences.class,
                response.body().byteStream());
        List<VideoReference> references = new ArrayList<>(videos.videos.length);
        for (JsonVideoReference video : videos.videos) {
            references.add(new VideoReference(UUID.fromString(video.uuid), video.revision));
        }
        return references;
    }

    @Override
    public List<Group> getGroups() throws IOException {
        Request request = buildGroupsRequest().get().build();
        Response response = executeRequest(request);
        JsonGroupList groups = serializer.read(JsonGroupList.class, response.body().byteStream());
        return groups.groups;
    }

    @Override
    public Video downloadVideoManifest(UUID id) throws IOException {

        Request request = buildVideosRequest(id).get().build();
        Response response = executeRequest(request);

        Video video = serializer.read(Video.class, response.body().byteStream());

        video.setManifestUri(Uri.parse(request.uri().toString()));
        video.setLastModified(response.headers().getDate("Last-Modified"));
        return video;
    }

    @Override
    public Video uploadVideoManifest(Video video) throws IOException {
        
        Request.Builder requestBuilder = buildVideosRequest(video.getId());

        String serializedVideo = serializer.write(video);
        Request request = requestBuilder
                .put(RequestBody.create(MediaType.parse("application/json"), serializedVideo))
                .build();

        Response response = executeRequest(request);

        Video uploadedVideo = serializer.read(Video.class, response.body().byteStream());

        uploadedVideo.setManifestUri(Uri.parse(request.uri().toString()));
        uploadedVideo.setLastModified(response.headers().getDate("Last-Modified"));
        return uploadedVideo;
    }

    @Override
    public void deleteVideoManifest(UUID id) throws IOException {

    }

    @Override
    public Video findVideoByVideoUri(Uri videoUri) throws IOException {

        Uri url = endpointUrl.buildUpon()
                .appendPath("videos")
                .appendPath("find.json")
                .appendQueryParameter("video", videoUri.toString())
                .build();

        Request request = new Request.Builder()
               .url(url.toString())
                .get().build();

        Response response = executeRequest(request);

        if (response.isSuccessful()) {
            Video video = serializer.read(Video.class, response.body().byteStream());
            return video;
        } else if (response.code() == 404) {
            return null;
        } else {
            throw new IOException(response.message());
        }
    }
}
