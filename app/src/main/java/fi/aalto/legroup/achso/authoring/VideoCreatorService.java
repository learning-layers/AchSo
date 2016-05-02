package fi.aalto.legroup.achso.authoring;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.google.android.gms.analytics.HitBuilders;
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
import fi.aalto.legroup.achso.app.AppAnalytics;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.User;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.playback.utilities.VideoOrientationReader;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;

/**
 * Creates videos in the background.
 *
 * TODO: Copying the stream from the content URI is lazy; persist it instead.
 * TODO: Delegate storage-specific things.
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

    /**
     * Returns a builder for a video. Video URI must be supplied via the builder's setters
     * before calling VideoBuilder#create().
     */
    public static VideoBuilder build() {
        return new VideoBuilder();
    }

    /**
     * Returns a builder for a video.
     */
    public static VideoBuilder build(Uri videoUri) {
        return new VideoBuilder(videoUri);
    }

    /**
     * TODO: This could be neater.
     */
    public static File getStorageVideoFile(VideoBuilder builder) {
        return getStorageFile(builder.id, "mp4");
    }

    private static File getStorageFile(UUID id, String extension) {
        return new File(App.localStorageDirectory, id + "." + extension);
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

        if (date == null) {
            date = new Date();
        }

        if (title == null) {
            title = buildTitle(date, location);
        }

        File manifestFile = getStorageFile(id, "json");
        File videoFile = getStorageFile(id, "mp4");
        File thumbFile = getStorageFile(id, "jpg");

        try {
            ensureVideoContent(videoContentUri, videoFile);
        } catch (IOException e) {
            // TODO: Error message
            bus.post(new VideoCreationStateEvent(VideoCreationStateEvent.Type.ERROR));
            e.printStackTrace();
            return;
        }

        int rotation = VideoOrientationReader.readOrientation(this, videoFile);

        try {
            createThumbnail(videoFile, thumbFile);
        } catch (IOException e) {
            // TODO: Error message
            e.printStackTrace();
        }

        // Send an analytics hit
        sendAnalytics(getDuration(videoFile));

        Uri manifestUri = Uri.fromFile(manifestFile);
        Uri videoUri = Uri.fromFile(videoFile);
        Uri thumbUri = Uri.fromFile(thumbFile);

        // Save always with the most recent version.
        int formatVersion = Video.VIDEO_FORMAT_VERSION;

        Video video = new Video(App.videoRepository, manifestUri, videoUri, thumbUri, id, title,
                tag, rotation, date, author, location, formatVersion, annotations);

        video.save();

        bus.post(new VideoCreationStateEvent(VideoCreationStateEvent.Type.FINISHED));
    }

    /**
     * Tries to ensure that video content of the given URI is where it should be. Only the existence
     * of videoFile is checked, it is not actually read. If videoFile does not exist, it will be
     * created using the content of videoContentUri.
     *
     * @param videoContentUri URI (file:// or content://) with raw video content.
     * @param videoFile       File that should have the content.
     * @throws IOException If the content cannot be copied.
     */
    private void ensureVideoContent(Uri videoContentUri, File videoFile) throws IOException {
        if (videoFile.isFile() && videoFile.canRead()) {
            return;
        }

        InputStream from = getContentResolver().openInputStream(videoContentUri);
        OutputStream to = new FileOutputStream(videoFile);

        copy(from, to);
    }

    /**
     * Copies one stream to another.
     *
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
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Returns a video title based on the video date and location, if any.
     *
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
     *
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

    /**
     * Sends an analytics hit for creating a video.
     *
     * @param duration Duration of the created video (in milliseconds) or -1 to not report it.
     */
    private void sendAnalytics(long duration) {
        HitBuilders.EventBuilder event = new HitBuilders.EventBuilder()
                .setCategory(AppAnalytics.CATEGORY_VIDEOS)
                .setAction(AppAnalytics.ACTION_CREATE);

        // Round the duration
        if (duration != -1) {
            int durationSeconds = Math.round(duration / 1000);
            event.setValue(durationSeconds);
        }

        AppAnalytics.send(event.build());
    }

    /**
     * Parses the duration of the given video file.
     *
     * @param videoFile File to parse.
     *
     * @return Duration of the video in milliseconds, or -1 if it could not be parsed.
     */
    private long getDuration(File videoFile) {
        MediaMetadataRetriever retriever = null;
        String duration = null;

        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoFile.getAbsolutePath());

            duration = retriever.extractMetadata(METADATA_KEY_DURATION);
        } finally {
            if (retriever != null) {
                retriever.release();
            }
        }

        if (duration == null) {
            return -1;
        }

        return Long.parseLong(duration);
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
    public static final class VideoBuilder implements Parcelable {

        private UUID id = UUID.randomUUID();
        private Uri videoUri;

        private String title;
        private String tag;
        private String genre;
        private Date date;
        private String authorName;
        private Uri authorUri;
        private Location location;
        private ArrayList<Annotation> annotations;

        /**
         * If null, missing parameters must be supplied via their setters before calling #create().
         */
        private VideoBuilder(@Nullable Uri videoUri) {
            this.videoUri = videoUri;
        }

        private VideoBuilder() {}

        protected VideoBuilder(Parcel parcel) {
            id = (UUID) parcel.readValue(UUID.class.getClassLoader());
            videoUri = (Uri) parcel.readValue(Uri.class.getClassLoader());
            authorUri = (Uri) parcel.readValue(Uri.class.getClassLoader());
            location = (Location) parcel.readValue(Location.class.getClassLoader());

            title = parcel.readString();
            tag = parcel.readString();
            authorName = parcel.readString();
            genre = parcel.readString();

            long dateLong = parcel.readLong();

            if (dateLong == -1) {
                date = null;
            } else {
                date = new Date(dateLong);
            }

            if (parcel.readByte() == 0x01) {
                annotations = new ArrayList<>();
                parcel.readList(annotations, Annotation.class.getClassLoader());
            } else {
                annotations = null;
            }
        }

        public VideoBuilder setId(UUID id) {
            this.id = id;
            return this;
        }

        public VideoBuilder setVideoUri(Uri videoUri) {
            this.videoUri = videoUri;
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

        public void create(Context context) {
            if (this.videoUri == null) {
                throw new IllegalArgumentException("Video URI must be supplied.");
            }

            Intent intent = new Intent(context, VideoCreatorService.class);

            intent.putExtra(ARG_VIDEO_ID, this.id);
            intent.putExtra(ARG_VIDEO_URI, this.videoUri);

            intent.putExtra(ARG_VIDEO_TITLE, this.title);
            intent.putExtra(ARG_VIDEO_TAG, this.tag);
            intent.putExtra(ARG_VIDEO_DATE, this.date);
            intent.putExtra(ARG_VIDEO_AUTHOR_NAME, this.authorName);
            intent.putExtra(ARG_VIDEO_AUTHOR_URI, this.authorUri);
            intent.putExtra(ARG_VIDEO_LOCATION, this.location);

            intent.putParcelableArrayListExtra(ARG_VIDEO_ANNOTATIONS, this.annotations);

            context.startService(intent);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeValue(id);
            parcel.writeValue(videoUri);
            parcel.writeValue(authorUri);
            parcel.writeValue(location);

            parcel.writeString(title);
            parcel.writeString(tag);
            parcel.writeString(genre);
            parcel.writeString(authorName);

            if (date == null) {
                parcel.writeLong(-1);
            } else {
                parcel.writeLong(date.getTime());
            }

            if (annotations == null) {
                parcel.writeByte((byte) 0x00);
            } else {
                parcel.writeByte((byte) 0x01);
                parcel.writeList(annotations);
            }
        }

        public static final Creator<VideoBuilder> CREATOR = new Creator<VideoBuilder>() {

            @Override
            public VideoBuilder createFromParcel(Parcel parcel) {
                return new VideoBuilder(parcel);
            }

            @Override
            public VideoBuilder[] newArray(int size) {
                return new VideoBuilder[size];
            }

        };

    }

}
