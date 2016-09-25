package fi.aalto.legroup.achso.storage.remote.strategies;

import android.accounts.Account;
import android.net.Uri;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Downloader;

import org.apache.http.protocol.RequestUserAgent;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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
import okio.BufferedSink;
import okio.Okio;

public class AchRailsStrategy implements VideoHost {

    class JsonVideoReference implements JsonSerializable {
        public String uuid;
        public int revision;
    }
    class JsonGroupList implements JsonSerializable {
        public List<Group> groups;
    }

    class JsonVideoList implements  JsonSerializable {
        public ArrayList<Video> videos;
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

    Request.Builder buildOwnGroupsRequest() {
        return new Request.Builder()
                .url(endpointUrl.buildUpon().appendPath("groups").appendPath("own.json").toString());
    }

    Request.Builder buildGroupShareRequest(UUID videoId) {
        return new Request.Builder()
                .url(endpointUrl.buildUpon().appendPath("videos").appendPath(videoId.toString()).appendPath("shares").toString());
    }

    Request.Builder buildGroupUnshareRequest(UUID videoId, int groupId) {
        return new Request.Builder()
                .url(endpointUrl.buildUpon().appendPath("videos").appendPath(videoId.toString()).appendPath("shares").appendPath(Integer.toString(groupId)).toString());
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
        Request request = buildOwnGroupsRequest().get().build();
        Response response = executeRequest(request);
        JsonGroupList groups = serializer.read(JsonGroupList.class, response.body().byteStream());
        return groups.groups;
    }

    @Override
    public void unshareVideo(UUID videoId, int groupId) throws IOException {
        Request request = buildGroupUnshareRequest(videoId, groupId).delete().build();
        Response response = executeRequest(request);
        System.out.println("unshare code:" + response.code());
    }

    @Override
    public void shareVideo(UUID videoId, int groupId) throws IOException, JSONException {
        JSONObject obj = new JSONObject();
        obj.put("group", groupId);
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), obj.toString());
        Request request = buildGroupShareRequest(videoId).post(body).build();
        Response response = executeRequest(request);
        System.out.println("share code:" + response.code());
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
    private void downloadFile(Uri uri, File endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(uri.toString())
                .get().build();

        Response response = App.httpClient.newCall(request).execute();
        BufferedSink sink = Okio.buffer(Okio.sink(endpoint));
        sink.writeAll(response.body().source());
        sink.close();
    }

    @Override
    public void downloadCachedFiles(Video video, Uri thumbUri, Uri videoUri) throws IOException {
        UUID uuid = video.getId();
        File thumbFile = App.videoRepository.getThumbCacheFile(uuid);
        File videoFile = App.videoRepository.getVideoCacheFile(uuid);

        downloadFile(thumbUri, thumbFile);
        downloadFile(videoUri, videoFile);

        video.setCacheThumbUri(android.net.Uri.parse((thumbFile.toURI().toString())));
        video.setCacheVideoUri(android.net.Uri.parse((videoFile.toURI().toString())));

        video.save(null);
    }

    public Video downloadVideoManifestIfNewerThan(UUID id, int revision, boolean isView) throws IOException {
        Request request = new Request.Builder()
                .url(endpointUrl.buildUpon()
                        .appendPath("videos")
                        .appendPath(id.toString() + ".json")
                        .appendQueryParameter("newer_than_rev", Integer.toString(revision))
                        .appendQueryParameter("is_view", isView ? "1" : "0")
                        .toString())
                .get().build();

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
        Request request = buildVideosRequest(id).delete().build();
        executeRequest(request);
    }

    @Override
    public ArrayList<Video> findVideosByQuery(String query) throws  IOException {
        Uri url = endpointUrl.buildUpon()
                .appendPath("videos")
                .appendPath("search")
                .appendQueryParameter("q", query)
                .build();

        Request request = new Request.Builder()
                .url(url.toString())
                .addHeader("Accept", "application/json")
                .get().build();

        Response response = executeRequest(request);
        if (response.isSuccessful()) {
            JsonVideoList jsonVideoList = serializer.read(JsonVideoList.class, response.body().byteStream());

            for (Video video : jsonVideoList.videos) {
                video.setIsTemporary(true);
                video.setRepository(App.videoRepository);
            }
            return jsonVideoList.videos;
        } else {
            return null;
        }

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
