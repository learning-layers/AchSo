package fi.aalto.legroup.achso.storage.remote.strategies;
                // Stop at the first uploader which succeeds.

import android.net.Uri;

import com.google.common.io.Files;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.otto.Bus;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URLConnection;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.remote.upload.ManifestUploader;
import fi.aalto.legroup.achso.storage.remote.upload.ThumbnailUploader;
import fi.aalto.legroup.achso.storage.remote.upload.VideoUploader;

/**
 * Supports uploading manifest and thumbnail and video data to an ownCloud instance.
 */
public class OwnCloudStrategy extends Strategy implements ManifestUploader, ThumbnailUploader,
        VideoUploader {

    protected JsonSerializer serializer;
    protected Uri endpointUrl;
    protected Uri webdavUrl;
    protected Uri sharesUrl;

    @Root(strict = false)
    private static class ShareResponseXML {

        @Path("data")
        @Element
        String token;
    };

    public OwnCloudStrategy(Bus bus, JsonSerializer serializer, Uri endpointUrl) {
        super(bus);

        this.serializer = serializer;
        this.endpointUrl = endpointUrl;
        this.webdavUrl = appendPaths(endpointUrl, "remote.php/webdav");
        this.sharesUrl = appendPaths(endpointUrl, "ocs/v1.php/apps/files_sharing/api/v1");
    }

    private Request.Builder buildWebDavRequest(String path) {

        Request.Builder builder = new Request.Builder()
            .url(appendPaths(this.webdavUrl, path).toString())
            .header("Authorization", Credentials.basic("user", "bitnami"));

        return builder;
    }

    private Request.Builder buildSharesRequest() {

        Request.Builder builder = new Request.Builder()
            .url(appendPaths(this.sharesUrl, "shares").toString())
            .header("Authorization", Credentials.basic("user", "bitnami"));

        return builder;
    }

    private Response executeRequest(Request request) throws IOException {

        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }

        return response;
    }

    private void uploadFile(String path, File file) throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(file.getPath());

        Request request = buildWebDavRequest(path)
                .put(RequestBody.create(MediaType.parse(mimeType), file))
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

    private Uri shareFile(String path) throws IOException {

        RequestBody formBody = new FormEncodingBuilder()
            .add("path", path)
            .add("shareType", "3") // public
            .add("publicUpload", "true") // yes, it's really public
            .build();

        Request request = buildSharesRequest()
                .header("Authorization", Credentials.basic("user", "bitnami"))
                .post(formBody)
                .build();

        Response response = executeRequest(request);
        ShareResponseXML xml = parseXml(ShareResponseXML.class, response);

        Uri url = appendPaths(endpointUrl, "index.php/s/" + xml.token + "/download");
        return url;
    }

    @Override
    public Uri uploadManifest(Video video) throws IOException {

        String serializedVideo = serializer.write(video);

        String path = "achso/manifest/ " + video.getId() + ".json";
        Request request = buildWebDavRequest(path)
                .put(RequestBody.create(MediaType.parse("application/json"), serializedVideo))
                .build();

        executeRequest(request);

        // TODO: Work as a repository too
        // bus.post(new VideoRepositoryUpdatedEvent(this));

        Uri uri = shareFile(path);

        return uri;
    }

    @Override
    public Uri uploadThumb(Video video) throws IOException {

        File file = new File(video.getThumbUri().getPath());
        String path = "achso/thumbnail/" + video.getId() + "." + Files.getFileExtension(file.getName());

        uploadFile(path, file);
        Uri shareUrl = shareFile(path);

        return shareUrl;
    }

    @Override
    public VideoUploadResult uploadVideo(Video video) throws IOException {

        File file = new File(video.getVideoUri().getPath());
        String path = "achso/video/" + video.getId() + "." + Files.getFileExtension(file.getName());
        
        uploadFile(path, file);
        Uri shareUrl = shareFile(path);

        return new VideoUploadResult(shareUrl);
    }

    @Override
    public void uploadCancelledCleanThumb(Video video, Uri thumbUrl) {

        // TODO: Remove file
    }

    @Override
    public void uploadCancelledCleanVideo(Video video, Uri videoUrl, Uri thumbUrl) {

        // TODO: Remove file
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
