package fi.aalto.legroup.achso.storage.remote.strategies;

import android.net.Uri;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URLConnection;
import java.util.UUID;
import java.util.regex.Pattern;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.remote.upload.ThumbnailUploader;
import fi.aalto.legroup.achso.storage.remote.upload.VideoUploader;

/**
 * Supports uploading manifest and thumbnail and video data to an ownCloud instance.
 */
public class OwnCloudStrategy implements ThumbnailUploader, VideoUploader {

    private static final Pattern MANIFEST_NAME_PATTERN = Pattern.compile(
            "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\.json");

    protected JsonSerializer serializer;
    protected Uri endpointUrl;
    protected Uri webdavUrl;
    protected Uri sharesUrl;
    protected String authorization;

    @Root(strict = false)
    private static class ShareResponseXML {

        @Path("data")
        @Element
        public String token;

        @Path("data")
        @Element
        public long id;
    };

    public OwnCloudStrategy(JsonSerializer serializer, Uri endpointUrl, String username, String password) {
        this.serializer = serializer;
        this.endpointUrl = endpointUrl;
        this.webdavUrl = appendPaths(endpointUrl, "remote.php/webdav");
        this.sharesUrl = appendPaths(endpointUrl, "ocs/v1.php/apps/files_sharing/api/v1");
        this.authorization = Credentials.basic(username, password);
    }

    private Request.Builder authorize(Request.Builder builder) {
        return builder.header("Authorization", authorization);
    }

    private Request.Builder buildWebDavRequest(String path) {
        Request.Builder builder = new Request.Builder()
            .url(appendPaths(this.webdavUrl, path).toString());
        return authorize(builder);
    }

    private Request.Builder buildSharesRequest() {
        Request.Builder builder = new Request.Builder()
            .url(appendPaths(this.sharesUrl, "shares").toString());
        return authorize(builder);
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

    private void uploadFile(String path, File file) throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(file.getPath());

        Request request = buildWebDavRequest(path)
                .put(RequestBody.create(MediaType.parse(mimeType), file))
                .build();

        executeRequest(request);
    }

    private void deleteFile(String path) throws IOException {
        Request request = buildWebDavRequest(path)
                .delete()
                .build();

        executeRequest(request);
    }

    private <T> T parseXml(Class<T> clazz, Response response) throws IOException {
        try {
            Serializer serializer = new Persister();
            Reader source = response.body().charStream();
            return serializer.read(clazz, source);
        } catch (Exception e) {
            throw new IOException("Invalid response XML: " + e.getMessage(), e);
        }
    }

    /**
     * Share a file to public using the ownCloud shares api.
     * @param path Path inside ownCloud, not local.
     * @return A public url to the file
     */
    private Uri shareFile(String path) throws IOException {

        RequestBody formBody = new FormEncodingBuilder()
            .add("path", path)
            .add("shareType", "3") // public
            .add("publicUpload", "true") // yes, it's really public
            .build();

        Request request = buildSharesRequest()
            .post(formBody)
            .build();

        Response response = executeRequest(request);
        ShareResponseXML xml = parseXml(ShareResponseXML.class, response);

        return appendPaths(endpointUrl, "index.php/s/" + xml.token + "/download");
    }

    private static Uri appendPaths(Uri base, String path) {
        Uri.Builder builder = base.buildUpon();

        String[] parts = path.split("/");
        for (String part : parts) {
            builder = builder.appendPath(part);
        }

        return builder.build();
    }

    private Uri uploadAndShare(String path, File file) throws IOException {
        uploadFile(path, file);
        return shareFile(path);
    }

    private String getManifestPath(UUID id) {
        return "achso/manifest/" + id + ".json";
    }

    private String getVideoPath(UUID id) {
        return "achso/video/" + id + ".mp4";
    }

    private String getThumbPath(UUID id) {
        return "achso/thumbnail/" + id + ".jpg";
    }


    // VideoUploader

    @Override
    public VideoUploadResult uploadVideo(Video video) throws IOException {
        File file = new File(video.getVideoUri().getPath());
        String path = getVideoPath(video.getId());

        return new VideoUploadResult(uploadAndShare(path, file));
    }

    @Override
    public void deleteVideo(Video video) throws IOException {
        deleteFile(getVideoPath(video.getId()));

    }

    // ThumbnailUploader

    @Override
    public Uri uploadThumb(Video video) throws IOException {
        File file = new File(video.getThumbUri().getPath());
        String path = getThumbPath(video.getId());

        return uploadAndShare(path, file);
    }

    @Override
    public void deleteThumb(Video video) throws IOException {
        deleteFile(getThumbPath(video.getId()));
    }
}
