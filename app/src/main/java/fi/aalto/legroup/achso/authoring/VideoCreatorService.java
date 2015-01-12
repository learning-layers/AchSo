package fi.aalto.legroup.achso.authoring;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;

import com.squareup.otto.Bus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.User;
import fi.aalto.legroup.achso.entities.Video;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Creates videos in the background.
 *
 * TODO: Copying the stream from the content URI is lazy; persist it instead.
 * TODO: Delegate storage-specific things.
 *
 * @author Leo Nikkilä
 */
public final class VideoCreatorService extends IntentService {

    public static final String ARG_VIDEO_URI = "ARG_VIDEO_URI";
    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";
    public static final String ARG_VIDEO_TITLE = "ARG_VIDEO_TITLE";
    public static final String ARG_VIDEO_GENRE = "ARG_VIDEO_GENRE";
    public static final String ARG_VIDEO_TAG = "ARG_VIDEO_TAG";
    public static final String ARG_VIDEO_DATE = "ARG_VIDEO_DATE";
    public static final String ARG_VIDEO_AUTHOR_NAME = "ARG_VIDEO_AUTHOR_NAME";
    public static final String ARG_VIDEO_AUTHOR_URI = "ARG_VIDEO_AUTHOR_URI";
    public static final String ARG_VIDEO_LOCATION = "ARG_VIDEO_LOCATION";
    public static final String ARG_VIDEO_ANNOTATIONS = "ARG_VIDEO_ANNOTATIONS";

    private Bus bus;

    public static VideoBuilder with(Context context, Uri videoUri, String videoGenre) {
        return new VideoBuilder(context, videoUri, videoGenre);
    }

    public VideoCreatorService() {
        super("VideoCreatorService");

        // TODO: Inject instead
        this.bus = App.bus;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        bus.post(new VideoCreationStateEvent(VideoCreationStateEvent.Type.STARTED));

        Uri videoContentUri = intent.getParcelableExtra(ARG_VIDEO_URI);
        UUID id = (UUID) intent.getSerializableExtra(ARG_VIDEO_ID);
        String title = intent.getStringExtra(ARG_VIDEO_TITLE);
        String genre = intent.getStringExtra(ARG_VIDEO_GENRE);
        String tag = intent.getStringExtra(ARG_VIDEO_TAG);
        Date date = (Date) intent.getSerializableExtra(ARG_VIDEO_DATE);
        String authorName = intent.getStringExtra(ARG_VIDEO_AUTHOR_NAME);
        Uri authorUri = intent.getParcelableExtra(ARG_VIDEO_AUTHOR_URI);
        Location location = intent.getParcelableExtra(ARG_VIDEO_LOCATION);
        List<Annotation> annotations = intent.getParcelableArrayListExtra(ARG_VIDEO_ANNOTATIONS);

        User author;

        if (location == null) {
            location = App.locationManager.getLastLocation();
        }

        if (authorName == null) {
            author = App.loginManager.getUser();
        } else {
            author = new User(authorName, authorUri);
        }

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (date == null) {
            date = new Date();
        }

        if (title == null) {
            title = buildTitle(date, location);
        }

        File videoFile = getStorageFile(id, "mp4");
        File thumbFile = getStorageFile(id, "jpg");

        Uri videoUri = Uri.fromFile(videoFile);
        Uri thumbUri = Uri.fromFile(thumbFile);

        // TODO: Try to record the video straight to the storage directory
        try {
            InputStream from = getContentResolver().openInputStream(videoContentUri);
            OutputStream to = new FileOutputStream(videoFile);

            copy(from, to);
        } catch (IOException e) {
            // TODO: Error message
            bus.post(new VideoCreationStateEvent(VideoCreationStateEvent.Type.ERROR));
            e.printStackTrace();
            return;
        }

        try {
            createThumbnail(videoFile, thumbFile);
        } catch (IOException e) {
            // TODO: Error message
            e.printStackTrace();
        }

        Video video = new Video(App.videoRepository, videoUri, thumbUri, id, title, genre, tag,
                date, author, location, annotations);

        video.save();

        bus.post(new VideoCreationStateEvent(VideoCreationStateEvent.Type.FINISHED));
    }

    private File getStorageFile(UUID id, String extension) {
        return new File(App.localStorageDirectory, id + "." + extension);
    }

    /**
     * Copies one stream to another.
     * @param from Input stream to read from.
     * @param to   Output stream to write to.
     * @throws IOException If the input stream cannot be read from or the output stream written to.
     */
    private void copy(InputStream from, OutputStream to) throws IOException {
        try {
            Source source = Okio.source(from);
            BufferedSink destination = Okio.buffer(Okio.sink(to));

            destination.writeAll(source);
            destination.flush();
        } finally {
            if (from != null) from.close();
            if (to != null) to.close();
        }
    }

    private void createThumbnail(File videoFile, File thumbFile) throws IOException {
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath(),
                MediaStore.Video.Thumbnails.MINI_KIND);

        if (thumbnail == null) {
            return;
        }

        OutputStream stream = null;

        try {
            stream = new FileOutputStream(thumbFile);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 74, stream);
        } finally {
            if (stream != null) stream.close();
        }
    }

    /**
     * Returns a video title based on the video date and location, if any.
     * @param date     Date of capturing.
     * @param location Location of capturing or null if there is none.
     */
    private String buildTitle(Date date, @Nullable Location location) {
        String addressString = getAddress(location);

        String dateString = DateFormat.getDateInstance().format(date);
        String timeString = DateFormat.getTimeInstance().format(date);

        if (addressString == null) {
            return dateString + " " + timeString;
        }

        return String.format("%s, %s %s", addressString, dateString, timeString);
    }

    /**
     * Returns an address string based on a location or null if the address cannot be resolved.
     * @param location Location to build an address for.
     */
    @Nullable
    private String getAddress(@Nullable Location location) {
        if (!Geocoder.isPresent() || location == null) {
            return null;
        }

        Geocoder geocoder = new Geocoder(this);

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        List<Address> addressList;

        try {
            addressList = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (addressList == null || addressList.isEmpty()) {
            return null;
        }

        return addressList.get(0).getAddressLine(0);
    }

    public static final class VideoCreationStateEvent {

        private Type type;

        public VideoCreationStateEvent(Type type) {
            this.type = type;
        }

        public Type getType() {
            return this.type;
        }

        public enum Type {
            STARTED,
            FINISHED,
            ERROR
        }

    }

    /**
     * Provides a flexible DSL for initialising the service.
     */
    public static final class VideoBuilder {

        private Context context;

        private Uri videoUri;
        private UUID id;
        private String title;
        private String genre;
        private String tag;
        private Date date;
        private String authorName;
        private Uri authorUri;
        private Location location;
        private ArrayList<Annotation> annotations;

        private VideoBuilder(Context context, Uri videoUri, String genre) {
            this.context = context;
            this.videoUri = videoUri;
            this.genre = genre;
        }

        public VideoBuilder setId(UUID id) {
            this.id = id;
            return this;
        }

        public VideoBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public VideoBuilder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public VideoBuilder setDate(Date date) {
            this.date = date;
            return this;
        }

        public VideoBuilder setAuthor(User author) {
            this.authorName = author.getName();
            this.authorUri = author.getUri();
            return this;
        }

        public VideoBuilder setLocation(Location location) {
            this.location = location;
            return this;
        }

        public VideoBuilder setAnnotations(List<Annotation> annotations) {
            this.annotations = new ArrayList<>(annotations);
            return this;
        }

        public void create() {
            Intent intent = new Intent(this.context, VideoCreatorService.class);

            intent.putExtra(ARG_VIDEO_URI, this.videoUri);
            intent.putExtra(ARG_VIDEO_ID, this.id);
            intent.putExtra(ARG_VIDEO_TITLE, this.title);
            intent.putExtra(ARG_VIDEO_GENRE, this.genre);
            intent.putExtra(ARG_VIDEO_TAG, this.tag);
            intent.putExtra(ARG_VIDEO_DATE, this.date);
            intent.putExtra(ARG_VIDEO_AUTHOR_NAME, this.authorName);
            intent.putExtra(ARG_VIDEO_AUTHOR_URI, this.authorUri);
            intent.putExtra(ARG_VIDEO_LOCATION, this.location);

            intent.putParcelableArrayListExtra(ARG_VIDEO_ANNOTATIONS, annotations);

            context.startService(intent);
        }

    }

}
