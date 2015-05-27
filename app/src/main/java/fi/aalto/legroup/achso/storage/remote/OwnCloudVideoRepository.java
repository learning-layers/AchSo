package fi.aalto.legroup.achso.storage.remote;

import android.net.Uri;

import com.squareup.okhttp.Credentials;
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

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoInfo;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.achso.storage.AbstractVideoRepository;
import fi.aalto.legroup.achso.storage.VideoRepositoryUpdatedEvent;

public class OwnCloudVideoRepository extends AbstractVideoRepository {
    
    protected JsonSerializer serializer;
    protected Uri endpointUrl;

    @Root
    class DAVPropfindXML {

        @ElementList(inline=true)
        private List<DAVPropfindResponseXML> responses;

        List<DAVPropfindResponseXML> getResponses() {
            return responses;
        }
    };

    @Root
    @Namespace(reference="DAV:", prefix="d")
    class DAVPropfindResponseXML {

        @Element
        @Namespace(reference="DAV:")
        private String href;

        @Element
        @Path("d:propstat/d:prop")
        @Namespace(reference="DAV:")
        private String getlastmodified;

        String getHref() { return href; }

        String getLastModified() { return getlastmodified; }
    }

    public OwnCloudVideoRepository(Bus bus, JsonSerializer serializer, Uri endpointUrl) {
        super(bus);

        this.serializer = serializer;
        this.endpointUrl = endpointUrl;
    }

    public Uri getVideoUrlForId(UUID key) {
        return endpointUrl.buildUpon().appendPath(key.toString() + ".json").build();
    }

    @Override
    public FindResults getAll() throws IOException {
        
        Request request = new Request.Builder()
            .url(endpointUrl.toString())
            .header("Accept", "application/json")
            .header("Authorization", Credentials.basic("user", "bitnami"))
            .header("Depth", "1")
            .method("PROPFIND", null)
            .build();

        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }

        DAVPropfindXML propRoot = null;

        try {
            // Try to serialize the XML
            Serializer serializer = new Persister();
            Reader source = response.body().charStream();
            propRoot = serializer.read(DAVPropfindXML.class, source);
            
        } catch (Exception e) {
            throw new IOException("Invalid response XML: " + e.getMessage(), e);
        }

        List<DAVPropfindResponseXML> propResponses = propRoot.getResponses();
        ArrayList<FindResult> results = new ArrayList<>(propResponses.size());

        for (DAVPropfindResponseXML propResponse : propResponses) {

            Uri href = Uri.parse(propResponse.getHref());
            String name = href.getLastPathSegment();

            String[] parts = name.split("\\.");

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

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
            Date date = null;

            try {
                date = dateFormat.parse(propResponse.getLastModified());
            } catch (ParseException e) {
                throw new IOException("Invalid response XML: " + e.getMessage(), e);
            }
            long timestamp = date.getTime();

            results.add(new FindResult(id, timestamp));
        }

        results.trimToSize();
        return new FindResults(results);
    }

    @Override
    public long getLastModifiedTime(UUID id) throws IOException {
        
        Request request = new Request.Builder()
            .url(endpointUrl.toString())
            .header("Accept", "application/json")
            .header("Authorization", Credentials.basic("user", "bitnami"))
            .header("Depth", "0")
            .method("PROPFIND", null)
            .build();

        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }

        DAVPropfindXML propRoot = null;

        try {
            // Try to serialize the XML
            Serializer serializer = new Persister();
            Reader source = response.body().charStream();
            propRoot = serializer.read(DAVPropfindXML.class, source);

        } catch (Exception e) {
            throw new IOException("Invalid response XML: " + e.getMessage(), e);
        }

        if (propRoot.getResponses().size() != 1)
            throw new IOException("Unexpected response count");

        DAVPropfindResponseXML propResponse = propRoot.getResponses().get(0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz");
        Date date = null;

        try {
            date = dateFormat.parse(propResponse.getLastModified());
        } catch (ParseException e) {
            throw new IOException("Invalid response XML: " + e.getMessage(), e);
        }

        return date.getTime();
    }

    protected <T extends JsonSerializable> T getAny(Class<T> classtype, Uri url) throws IOException {
        Request request = new Request.Builder()
            .url(url.toString())
            .header("Accept", "application/json")
            .header("Authorization", Credentials.basic("user", "bitnami"))
            .get()
            .build();

        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }

        return serializer.read(classtype, response.body().byteStream());
    }

    @Override
    public VideoInfo getVideoInfo(UUID id) throws IOException {

        Uri url = getVideoUrlForId(id);
        VideoInfo videoInfo = getAny(VideoInfo.class, url);
        videoInfo.setManifestUri(url);

        return videoInfo;
    }

    @Override
    public Video getVideo(UUID id) throws IOException {

        Uri url = getVideoUrlForId(id);
        Video video = getAny(Video.class, url);
        video.setManifestUri(url);
        video.setRepository(this);

        return video;
    }

    @Override
    public void save(Video video) throws IOException {

        String serializedVideo = serializer.write(video);

        Request request = new Request.Builder()
            .url(getVideoUrlForId(video.getId()).toString())
            .header("Authorization", Credentials.basic("user", "bitnami"))
            .put(RequestBody.create(MediaType.parse("application/json"), serializedVideo))
            .build();

        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }

        bus.post(new VideoRepositoryUpdatedEvent(this));
    }

    @Override
    public void delete(UUID id) throws IOException {

        Request request = new Request.Builder()
            .url(getVideoUrlForId(id).toString())
            .header("Authorization", Credentials.basic("user", "bitnami"))
            .delete()
            .build();

        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String errorMessage = response.code() + " " + response.message();
            throw new IOException(errorMessage);
        }

        bus.post(new VideoRepositoryUpdatedEvent(this));
    }
}
