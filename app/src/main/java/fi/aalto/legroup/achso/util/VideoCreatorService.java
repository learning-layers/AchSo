package fi.aalto.legroup.achso.util;

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
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

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
public class VideoCreatorService extends IntentService {

    public static final String ARG_VIDEO_URI = "ARG_VIDEO_URI";
    public static final String ARG_VIDEO_GENRE = "ARG_VIDEO_GENRE";

    private Bus bus;

    public static void create(Context context, Uri videoUri, String videoGenre) {
        Intent intent = new Intent(context, VideoCreatorService.class);

        intent.putExtra(ARG_VIDEO_URI, videoUri);
        intent.putExtra(ARG_VIDEO_GENRE, videoGenre);

        context.startService(intent);
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
        String genre = intent.getStringExtra(ARG_VIDEO_GENRE);

        // TODO: These should be provided via the intent
        Location location = App.locationManager.getLastLocation();
        User author = App.loginManager.getUser();

        UUID id = UUID.randomUUID();
        Date date = new Date();
        String title = buildTitle(date, location);

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

        Video video = new Video(App.videoRepository, videoUri, thumbUri, id, title, genre, null,
                date, author, location, null);

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

}
