package fi.aalto.legroup.achso.storage.remote.strategies;

import android.net.Uri;

import com.google.common.io.Files;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.otto.Bus;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;
import fi.aalto.legroup.achso.storage.remote.VideoHost;
import fi.aalto.legroup.achso.storage.remote.upload.ThumbnailUploader;
import fi.aalto.legroup.achso.storage.remote.upload.VideoUploader;

public class ShareServerStrategy implements ThumbnailUploader, VideoUploader,
        VideoHost {

    protected Bus bus;
    protected JsonSerializer serializer;
    protected Uri endpointUrl;

    public ShareServerStrategy(Bus bus, JsonSerializer serializer, Uri endpointUrl) {
        this.bus = bus;
        this.serializer = serializer;
        this.endpointUrl = endpointUrl;
    }

    static class FileResponse implements JsonSerializable {
        public String name;
        public String path;
        public boolean directory;
        public String etag;
        public String modified;
        public List<FileResponse> children;
        public String mime;
    };

    Request.Builder buildRequest(String path) {
        Uri uri = appendPaths(endpointUrl, path);
        return new Request.Builder()
            .url(uri.toString())
            .header("Authorization", Credentials.basic("user", "pass"));
    }

    private Response executeRequestNoFail(Request request) throws IOException {
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

    private Uri uploadFile(String path, File file) throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(file.getPath());

        Request request = buildRequest(path)
                .put(RequestBody.create(MediaType.parse(mimeType), file))
                .build();

        Response response = executeRequest(request);
        FileResponse fileResponse = serializer.read(FileResponse.class,
                response.body().byteStream());

        return appendPaths(endpointUrl, fileResponse.path);
    }


    @Override
    public void uploadCancelledCleanThumb(Video video,
            ThumbnailUploadResult result) throws IOException {

    }

    @Override
    public ThumbnailUploadResult uploadThumb(Video video) throws IOException {
        File file = new File(video.getThumbUri().getPath());
        String path = "thumb/" + video.getId() + "." + Files.getFileExtension(file.getName());
        return new ThumbnailUploadResult(uploadFile(path, file));
    }

    @Override
    public List<VideoInfoRepository.FindResult> getIndex() throws IOException {

        Request request = buildRequest("manifest/")
            .get()
            .build();

        Response response = executeRequest(request);
        FileResponse fileResponse = serializer.read(FileResponse.class,
                response.body().byteStream());

        List<VideoInfoRepository.FindResult> results = new ArrayList<>(fileResponse.children.size());
        for (FileResponse child : fileResponse.children) {
            String path = child.path;
            UUID id = UUID.fromString(path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.')));
            long timestamp = Long.parseLong(child.modified);
            results.add(new VideoInfoRepository.FindResult(id, timestamp));
        }

        return results;
    }

    @Override
    public Video downloadVideoManifest(UUID id) throws IOException {
        Request request = buildRequest("manifest/" + id + ".json")
            .get()
            .build();

        Response response = executeRequest(request);
        Video video = serializer.read(Video.class, response.body().byteStream());
        video.setManifestUri(Uri.parse(request.uri().toString()));
        video.setVersionTag(response.header("ETag"));
        video.setLastModified(response.headers().getDate("Last-Modified"));
        return video;
    }

    @Override
    public ManifestUploadResult uploadVideoManifest(Video video, String expectedVersionTag) throws IOException {

        String serializedVideo = serializer.write(video);
        Request request = buildRequest("manifest/" + video.getId() + ".json")
            .put(RequestBody.create(MediaType.parse("application/json"), serializedVideo))
            .header("If-Match", expectedVersionTag != null ? expectedVersionTag : "*")
            .build();

        Response response = executeRequestNoFail(request);

        if (response.code() == 412) {
            // Precondition failed (ETag didn't match, return null instead of failing)
            return null;
        }

        validateResponse(response);

        FileResponse fileResponse = serializer.read(FileResponse.class,
                response.body().byteStream());

        Uri url = appendPaths(endpointUrl, fileResponse.path);
        String versionTag = response.header("ETag");

        return new ManifestUploadResult(url, versionTag);
    }

    @Override
    public void deleteVideoManifest(UUID id) throws IOException {
        Request request = buildRequest("manifest/" + id + ".json")
            .delete()
            .build();

        executeRequest(request);
    }

    @Override
    public void uploadCancelledCleanVideo(Video video, VideoUploadResult result)
            throws IOException {

    }

    @Override
    public VideoUploadResult uploadVideo(Video video) throws IOException {
        File file = new File(video.getVideoUri().getPath());
        String path = "video/" + video.getId() + "." + Files.getFileExtension(file.getName());
        return new VideoUploadResult(uploadFile(path, file));
    }

    private static Uri appendPaths(Uri base, String path) {
        Uri.Builder builder = base.buildUpon();

        String[] parts = path.split("/");
        for (String part : parts) {
            builder = builder.appendPath(part);
        }

        return builder.build();
    }

}
