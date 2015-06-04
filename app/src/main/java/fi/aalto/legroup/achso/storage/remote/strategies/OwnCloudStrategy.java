package fi.aalto.legroup.achso.storage.remote.strategies;
                // Stop at the first uploader which succeeds.

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
import java.util.UUID;

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

    public OwnCloudStrategy(Bus bus, JsonSerializer serializer, Uri endpointUrl) {
        super(bus);

        this.serializer = serializer;
        this.endpointUrl = endpointUrl;
    }


    public Uri getManifestUrlForId(UUID key) {
        return endpointUrl.buildUpon().appendPath("manifest").appendPath(key.toString() + ".json").build();
    }

    public Uri getThumbUrlForId(UUID key) {
        return endpointUrl.buildUpon().appendPath("thumbnail").appendPath(key.toString()).build();
    }

    public Uri getVideoUrlForId(UUID key) {
        return endpointUrl.buildUpon().appendPath("video").appendPath(key.toString()).build();
    }

    private Uri uploadFile(Uri baseUrl, File file) throws IOException {
        String extension = Files.getFileExtension(file.getName());
        String mimeType = URLConnection.guessContentTypeFromName(file.getPath());

        // This is kinda ugly but I don't see that the builder could append an extension
        Uri url = Uri.parse(baseUrl.toString() + "." + extension);

        Request request = new Request.Builder()
                .url(url.toString())
                .header("Authorization", Credentials.basic("user", "bitnami"))
                .put(RequestBody.create(MediaType.parse(mimeType), file))
                .build();

        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }

        return url;
    }

    @Override
    public Uri uploadManifest(Video video) throws IOException {
        String serializedVideo = serializer.write(video);

        Uri url = getManifestUrlForId(video.getId());
        Request request = new Request.Builder()
                .url(url.toString())
                .header("Authorization", Credentials.basic("user", "bitnami"))
                .put(RequestBody.create(MediaType.parse("application/json"), serializedVideo))
                .build();

        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }

        // TODO: Work as a repository too
        // bus.post(new VideoRepositoryUpdatedEvent(this));

        return url;
    }

    @Override
    public void uploadCancelledCleanThumb(Video video, Uri thumbUrl) {

        // TODO: Remove file
    }

    public Uri uploadThumb(Video video) throws IOException {

        Uri url = getThumbUrlForId(video.getId());
        File file = new File(video.getThumbUri().getPath());

        return uploadFile(url, file);
    }

    @Override
    public void uploadCancelledCleanVideo(Video video, Uri videoUrl, Uri thumbUrl) {

        // TODO: Remove file
    }

    @Override
    public VideoUploadResult uploadVideo(Video video) throws IOException {

        Uri url = getVideoUrlForId(video.getId());
        File file = new File(video.getVideoUri().getPath());

        return new VideoUploadResult(uploadFile(url, file));
    }
}
