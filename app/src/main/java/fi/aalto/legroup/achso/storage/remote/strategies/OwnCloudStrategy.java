package fi.aalto.legroup.achso.storage.remote.strategies;

import android.net.Uri;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.HttpDate;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Group;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoReference;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.remote.VideoHost;
import fi.aalto.legroup.achso.storage.remote.upload.ThumbnailUploader;
import fi.aalto.legroup.achso.storage.remote.upload.VideoUploader;

/**
 * Supports uploading manifest and thumbnail and video data to an ownCloud instance.
 */
public class OwnCloudStrategy implements ThumbnailUploader,
        VideoUploader, VideoHost {

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

    // VideoHost

    @Override
    public List<VideoReference> getIndex() throws IOException {
        Request request = buildWebDavRequest("achso/manifest")
            .header("Depth", "1")
            .method("PROPFIND", null)
            .build();

        Response response = executeRequest(request);
        DAVPropfindXML xml = parseXml(DAVPropfindXML.class, response);

        List<DAVPropfindResponseXML> propResponses = xml.getResponses();
        ArrayList<VideoReference> results = new ArrayList<>(propResponses.size());

        for (DAVPropfindResponseXML propResponse : propResponses) {

            Uri href = Uri.parse(propResponse.getHref());
            String name = href.getLastPathSegment();

            Matcher matcher = MANIFEST_NAME_PATTERN.matcher(name.trim().toLowerCase());
            if (!matcher.matches())
                continue;

            UUID id;
            try {
                id = UUID.fromString(matcher.group(1));
            } catch (IllegalArgumentException e) {
                continue;
            }

            Date date = HttpDate.parse(propResponse.getLastModified());
            if (date == null) {
                continue;
            }
            results.add(new VideoReference(id, date.getTime()));
        }

        results.trimToSize();
        return results;
    }

    @Override
    public List<Group> getGroups() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public Video downloadVideoManifest(UUID id) throws IOException {
        Request request = buildWebDavRequest(getManifestPath(id))
                .get()
                .build();

        Response response = executeRequest(request);

        Video video = serializer.read(Video.class, response.body().byteStream());

        // This is technically wrong but I don't think getting the share url is worth the trouble.
        video.setManifestUri(Uri.parse(getManifestPath(id)));
        video.setVersionTag(response.header("ETag"));
        video.setLastModified(response.headers().getDate("Last-Modified"));
        return video;
    }

    @Override
    public ManifestUploadResult uploadVideoManifest(Video video,
            String expectedVersionTag) throws IOException {

        String path = getManifestPath(video.getId());
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
        Uri uri = shareFile(path);

        return new ManifestUploadResult(uri, versionTag);
    }

    @Override
    public void deleteVideoManifest(UUID id) throws IOException {
        deleteFile(getManifestPath(id));
    }


}
