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
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;
import fi.aalto.legroup.achso.storage.remote.VideoHost;
import fi.aalto.legroup.achso.storage.remote.upload.ThumbnailUploader;
import fi.aalto.legroup.achso.storage.remote.upload.VideoUploader;

/**
 * Supports uploading manifest and thumbnail and video data to an ownCloud instance.
 */
public class OwnCloudStrategy extends Strategy implements ThumbnailUploader,
        VideoUploader, VideoHost {

    protected JsonSerializer serializer;
    protected Uri endpointUrl;
    protected Uri webdavUrl;
    protected Uri sharesUrl;

    protected static final SimpleDateFormat davDateFormat
            = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

    @Root(strict = false)
    private static class ShareResponseXML {

        @Path("data")
        @Element
        public String token;

        @Path("data")
        @Element
        public long id;
    };

    @Root(strict=false)
    @Namespace(reference="DAV:", prefix="d")
    static class DAVPropfindXML {

        @ElementList(inline=true)
        @Namespace(reference="DAV:", prefix="d")
        private List<DAVPropfindResponseXML> responses;

        List<DAVPropfindResponseXML> getResponses() {
            return responses;
        }
    };

    @Root(name="response", strict=false)
    @Namespace(reference="DAV:", prefix="d")
    static class DAVPropfindResponseXML {

        @Element
        @Namespace(reference="DAV:", prefix="d")
        private String href;

        @Element
        @Path("d:propstat/d:prop")
        @Namespace(reference="DAV:", prefix="d")
        private String getlastmodified;

        String getHref() { return href; }

        String getLastModified() { return getlastmodified; }
    }

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

    private Request.Builder buildSharesRequest(long id) {

        Request.Builder builder = new Request.Builder()
                .url(appendPaths(this.sharesUrl, "shares/" + id).toString())
                .header("Authorization", Credentials.basic("user", "bitnami"));

        return builder;
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

    private void deleteShare(long id) throws IOException {

        Request request = buildSharesRequest(id)
                .delete()
                .build();

        executeRequest(request);
    }

    private void deleteFileAndShare(String path, long id) throws IOException {

        IOException fileException = null;
        IOException shareException = null;

        try {
            deleteFile(path);
        } catch (IOException e) {
            fileException = e;
        }
        try {
            deleteShare(id);
        } catch (IOException e) {
            shareException = e;
        }

        if (fileException != null && shareException != null) {
            throw new IOException("Failed to delete file and share: " + fileException.getMessage() + ", " + shareException.getMessage(), fileException);
        } else if (fileException != null) {
            throw new IOException("Failed to delete file: " + fileException.getMessage(), fileException);
        } else if (shareException != null) {
            throw new IOException("Failed to delete share: " + shareException.getMessage(), shareException);
        }
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


    private ShareResult shareFile(String path) throws IOException {

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
        return new ShareResult(url, xml.id);
    }

    @Override
    public void deleteVideoManifest(UUID id) throws IOException {
        // TODO
    }

    @Override
    public ThumbnailUploadResult uploadThumb(Video video) throws IOException {

        File file = new File(video.getThumbUri().getPath());
        String path = "achso/thumbnail/" + video.getId() + "." + Files.getFileExtension(file.getName());

        uploadFile(path, file);
        ShareResult result = shareFile(path);

        return new OwnCloudThumbUploadResult(result.getUrl(), path, result.getId());
    }

    @Override
    public VideoUploadResult uploadVideo(Video video) throws IOException {

        File file = new File(video.getVideoUri().getPath());
        String path = "achso/video/" + video.getId() + "." + Files.getFileExtension(file.getName());

        uploadFile(path, file);
        ShareResult result = shareFile(path);

        return new OwnCloudVideoUploadResult(result.getUrl(), path, result.getId());
    }

    @Override
    public void uploadCancelledCleanThumb(Video video, ThumbnailUploadResult result) throws IOException {

        if (!(result instanceof OwnCloudThumbUploadResult)) {
            throw new IllegalArgumentException("result expected to be OwnCloudThumbUploadResult");
        }
        OwnCloudThumbUploadResult ownCloudResult = (OwnCloudThumbUploadResult) result;

        deleteFileAndShare(ownCloudResult.getPath(), ownCloudResult.getShareId());
    }

    @Override
    public void uploadCancelledCleanVideo(Video video, VideoUploadResult result) throws IOException {

        if (!(result instanceof OwnCloudVideoUploadResult)) {
            throw new IllegalArgumentException("result expected to be OwnCloudVideoUploadResult");
        }
        OwnCloudVideoUploadResult ownCloudResult = (OwnCloudVideoUploadResult) result;

        deleteFileAndShare(ownCloudResult.getPath(), ownCloudResult.getShareId());
    }

    public List<VideoInfoRepository.FindResult> getIndex() throws IOException {
        Request request = buildWebDavRequest("achso/manifest")
            .header("Depth", "1")
            .method("PROPFIND", null)
            .build();

        Response response = executeRequest(request);
        DAVPropfindXML xml = parseXml(DAVPropfindXML.class, response);

        List<DAVPropfindResponseXML> propResponses = xml.getResponses();
        ArrayList<VideoInfoRepository.FindResult> results = new ArrayList<>(propResponses.size());

        for (DAVPropfindResponseXML propResponse : propResponses) {

            Uri href = Uri.parse(propResponse.getHref());
            String name = href.getLastPathSegment();

            String[] parts = name.trim().split("\\.");

            if (parts.length != 2)
                continue;
            if (!parts[1].equals("json"))
                continue;

            UUID id;
            try {
                id = UUID.fromString(parts[0]);
            } catch (IllegalArgumentException e) {
                continue;
            }

            Date date;
            try {
                date = davDateFormat.parse(propResponse.getLastModified());
            } catch (ParseException e) {
                throw new IOException("Invalid response XML: " + e.getMessage(), e);
            }
            long timestamp = date.getTime();

            results.add(new VideoInfoRepository.FindResult(id, timestamp));
        }

        results.trimToSize();
        return results;
    }

    @Override
    public Video downloadVideoManifest(UUID id) throws IOException {
        Request request = buildWebDavRequest("achso/manifest/" + id + ".json")
                .get()
                .build();

        Response response = executeRequest(request);

        Video video = serializer.read(Video.class, response.body().byteStream());
        video.setManifestUri(Uri.parse("achso/manifest/" + id + ".json"));
        video.setVersionTag(response.header("ETag"));
        return video;
    }

    @Override
    public ManifestUploadResult uploadVideoManifest(Video video,
            String expectedVersionTag) throws IOException {

        String path = "achso/manifest/" + video.getId() + ".json";
        Request.Builder requestBuilder = buildWebDavRequest(path);

        if (expectedVersionTag != null) {
            // This is a little suspicious, but ownCloud gives ETags with "-gzip" suffix, but
            // doesn't match them in If-Match to the same resource. If the suffix is stripped
            // away then it matches just fine...
            expectedVersionTag = expectedVersionTag.replace("-gzip", "");
            requestBuilder = requestBuilder.addHeader("If-Match", expectedVersionTag);
        }

        String serializedVideo = serializer.write(video);

        Request request = requestBuilder
                .put(RequestBody.create(MediaType.parse("application/json"), serializedVideo))
            .build();

        Response response = executeRequestNoFail(request);

        if (response.code() == 412) {
            // Precondition failed (ETag didn't match, return null instead of failing)
            return null;
        }
        validateResponse(response);

        String versionTag = response.header("ETag");
        Uri uri = shareFile(path).getUrl();

        return new ManifestUploadResult(uri, versionTag);
    }

    private static Uri appendPaths(Uri base, String path) {
        Uri.Builder builder = base.buildUpon();

        String[] parts = path.split("/");
        for (String part : parts) {
            builder = builder.appendPath(part);
        }

        return builder.build();
    }

    protected static class OwnCloudVideoUploadResult extends VideoUploadResult {

        private String path;
        private long shareId;

        OwnCloudVideoUploadResult(Uri videoUrl, String path, long shareId) {
            super(videoUrl);
            this.path = path;
            this.shareId = shareId;
        }

        String getPath() {
            return path;
        }
        long getShareId() {
            return shareId;
        }
    };

    protected static class OwnCloudThumbUploadResult extends ThumbnailUploadResult {

        private String path;
        private long shareId;

        OwnCloudThumbUploadResult(Uri thumbUrl, String path, long shareId) {
            super(thumbUrl);
            this.path = path;
            this.shareId = shareId;
        }

        String getPath() {
            return path;
        }
        long getShareId() {
            return shareId;
        }
    };

    protected static class ShareResult {

        private Uri url;
        private long id;

        ShareResult(Uri url, long id) {
            this.url = url;
            this.id = id;
        }

        Uri getUrl() {
            return url;
        }
        long getId() {
            return id;
        }

    };
}
