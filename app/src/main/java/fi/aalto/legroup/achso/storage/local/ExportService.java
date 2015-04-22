package fi.aalto.legroup.achso.storage.local;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;

import com.google.common.base.Optional;
import com.google.common.io.Closer;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.app.AppCache;
import fi.aalto.legroup.achso.browsing.BrowserActivity;
import fi.aalto.legroup.achso.entities.VideoInfo;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;

import static android.app.Notification.DEFAULT_LIGHTS;
import static android.app.Notification.DEFAULT_VIBRATE;
import static android.app.Notification.PRIORITY_HIGH;
import static android.app.PendingIntent.FLAG_ONE_SHOT;

/**
 * A service for exporting videos as local .achso files.
 *
 * TODO: Implementation details of .achso files could be moved elsewhere.
 */
public final class ExportService extends IntentService {

    public static final String TAG = "ExportService";

    public static final String ARG_OUTPUT_DIRECTORY = "ARG_OUTPUT_DIRECTORY";
    public static final String ARG_VIDEO_IDS = "ARG_VIDEO_IDS";

    private static final int PROGRESS_NOTIFICATION_ID = 1;
    private static final int FINISHED_NOTIFICATION_ID = 2;
    private static final int ERROR_NOTIFICATION_ID = 3;

    private OkHttpClient httpClient;
    private VideoInfoRepository videoInfoRepository;
    private NotificationManager notificationManager;

    /**
     * Convenience method for exporting a video into Android's cache directory. When the video has
     * been exported, an ExportResultEvent is broadcast.
     *
     * @param activity Activity to use.
     * @param video    ID of the video to export.
     */
    public static void export(Activity activity, UUID video) {
        export(activity, Collections.singletonList(video));
    }

    /**
     * Convenience method for exporting videos. Exports the videos into Android's cache directory.
     * When all videos have been exported, an ExportResultEvent is broadcast.
     *
     * @param activity Activity to use.
     * @param videos   List of video IDs to export.
     */
    public static void export(Activity activity, List<UUID> videos) {
        export(activity, AppCache.getCache(activity), videos);
    }

    /**
     * Convenience method for exporting videos. When all videos have been exported, an
     * ExportResultEvent is broadcast.
     *
     * @param activity        Activity to use.
     * @param outputDirectory Directory where the exported videos should be output.
     * @param videos          List of video IDs to export.
     */
    public static void export(Activity activity, File outputDirectory, List<UUID> videos) {
        Intent intent = new Intent(activity, ExportService.class);

        // If the list is serializable, it can be used like that. If not, a new ArrayList is
        // created with the contents.
        Serializable serializableVideos;

        if (videos instanceof Serializable) {
            serializableVideos = (Serializable) videos;
        } else {
            serializableVideos = new ArrayList<>(videos);
        }

        intent.putExtra(ARG_OUTPUT_DIRECTORY, outputDirectory);
        intent.putExtra(ARG_VIDEO_IDS, serializableVideos);

        activity.startService(intent);

        // Show a snackbar telling the user that we'll get back to them.
        SnackbarManager.show(Snackbar.with(activity).text(R.string.share_preparing_toast));
    }

    public ExportService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO: Inject instead
        httpClient = App.httpClient;
        videoInfoRepository = App.videoInfoRepository;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        File outputDirectory = (File) intent.getSerializableExtra(ARG_OUTPUT_DIRECTORY);

        // This cast is expensive to check: it's your fault if you stick something else in there.
        // The static convenience methods should protect against it.
        @SuppressWarnings("unchecked")
        List<UUID> videoIds = (List<UUID>) intent.getSerializableExtra(ARG_VIDEO_IDS);
        int count = videoIds.size();

        showProgressNotification(count);

        Map<UUID, Optional<File>> results = exportVideos(outputDirectory, videoIds);
        List<File> sharedFiles = new ArrayList<>();
        List<UUID> failedVideos = new ArrayList<>();

        // Check if videos could be exported and divide them into two lists
        for (Map.Entry<UUID, Optional<File>> result : results.entrySet()) {
            UUID id = result.getKey();
            Optional<File> file = result.getValue();

            if (file.isPresent()) {
                sharedFiles.add(file.get());
            } else {
                failedVideos.add(id);
            }
        }

        hideProgressNotification();

        if (sharedFiles.size() > 0) {
            showFinishedNotification(sharedFiles);
        }

        if (failedVideos.size() > 0) {
            showFailedNotification(failedVideos);
        }
    }

    /**
     * Exports the given videos as .achso files.
     *
     * @param directory Where the exported videos should be stored.
     * @param videos    List of the videos to export.
     *
     * @return Map with IDs of the videos as keys and the exported files as values. The file may be
     *         absent if it cannot be exported.
     */
    private Map<UUID, Optional<File>> exportVideos(File directory, List<UUID> videos) {
        Map<UUID, Optional<File>> results = new HashMap<>();

        for (UUID id : videos) {
            try {
                File outputFile = exportVideo(directory, id);
                results.put(id, Optional.of(outputFile));
            } catch (IOException e) {
                results.put(id, Optional.<File>absent());
                e.printStackTrace();
            }
        }

        return results;
    }

    /**
     * Exports a video as an .achso file.
     *
     * @param directory Where the video should be exported.
     * @param videoId   Video to export.
     *
     * @return The file where the video was exported.
     *
     * @throws IOException If the video could not be exported.
     */
    private File exportVideo(File directory, UUID videoId) throws IOException {
        // TODO: We should not rely on the repository
        VideoInfo video = videoInfoRepository.get(videoId);

        String fileName = sanitizeFilename(video.getTitle()) + ".achso";
        File outputFile = new File(directory, fileName);

        Closer closer = Closer.create();

        try {
            OutputStream output = new FileOutputStream(outputFile);
            ZipOutputStream zipOutput = new ZipOutputStream(output);
            Sink zipSink = closer.register(Okio.sink(zipOutput));

            BufferedSource manifestSource = closer.register(getSource(video.getManifestUri()));
            BufferedSource thumbSource = closer.register(getSource(video.getThumbUri()));
            BufferedSource videoSource = closer.register(getSource(video.getVideoUri()));

            ZipEntry jsonEntry = new ZipEntry(videoId + ".json");
            ZipEntry jpgEntry = new ZipEntry(videoId + ".jpg");
            ZipEntry mp4Entry = new ZipEntry(videoId + ".mp4");

            // No compression
            zipOutput.setMethod(ZipOutputStream.DEFLATED);
            zipOutput.setLevel(Deflater.NO_COMPRESSION);

            zipOutput.putNextEntry(jsonEntry);
            manifestSource.readAll(zipSink);
            zipOutput.closeEntry();

            zipOutput.putNextEntry(jpgEntry);
            thumbSource.readAll(zipSink);
            zipOutput.closeEntry();

            zipOutput.putNextEntry(mp4Entry);
            videoSource.readAll(zipSink);
            zipOutput.closeEntry();

            zipSink.flush();
            zipOutput.finish();
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }

        return outputFile;
    }

    /**
     * Notifies the user that the service is running. The notification needs to be cancelled when
     * it's no longer needed using #hideProgressNotification().
     *
     * @param count Number of videos that will be exported.
     */
    private void showProgressNotification(int count) {
        String title = getResources().getQuantityString(R.plurals.share_preparing, count, count);

        Notification notification = new Notification.Builder(this)
                .setDefaults(DEFAULT_VIBRATE)
                .setPriority(PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_sync_white_24dp)
                .setContentTitle(title)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(0, 100, true)
                .build();

        notificationManager.notify(TAG, PROGRESS_NOTIFICATION_ID, notification);
    }

    /**
     * Hides the progress notification.
     */
    private void hideProgressNotification() {
        notificationManager.cancel(TAG, PROGRESS_NOTIFICATION_ID);
    }

    /**
     * Shows a notification that prompts the user to share the given exported files.
     *
     * @param files List of exported .achso files to share.
     */
    private void showFinishedNotification(List<File> files) {
        ArrayList<Uri> fileUris = new ArrayList<>();

        for (File file : files) {
            fileUris.add(Uri.fromFile(file));
        }

        int count = fileUris.size();
        String title = getResources().getQuantityString(R.plurals.share_ready, count, count);
        String subtitle = getString(R.string.share_ready_subtitle);

        Intent shareIntent = createShareIntent(fileUris);
        Intent chooserIntent = Intent.createChooser(shareIntent, getString(R.string.video_share));
        PendingIntent intent = PendingIntent.getActivity(this, 0, chooserIntent, FLAG_ONE_SHOT);

        Notification notification = new Notification.Builder(this)
                .setDefaults(DEFAULT_LIGHTS | DEFAULT_VIBRATE)
                .setPriority(PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(intent)
                .setSmallIcon(R.drawable.ic_done_white_24dp)
                .setContentTitle(title)
                .setContentText(subtitle)
                .build();

        notificationManager.notify(FINISHED_NOTIFICATION_ID, notification);
    }

    /**
     * Shows a notification that tells the user that some videos could not be exported.
     *
     * @param failedIds List of video IDs that could not be exported.
     */
    private void showFailedNotification(List<UUID> failedIds) {
        int count = failedIds.size();
        String title = getResources().getQuantityString(R.plurals.share_failed, count, count);
        String subtitle = getString(R.string.share_ready_subtitle);

        // TODO: Do something more sophisticated here.
        // Currently the intent redirects the user to the browser activity. Should we show a list
        // of the videos that could not be exported? Maybe using search?
        Intent browserIntent = new Intent(this, BrowserActivity.class);

        Notification notification = new Notification.Builder(this)
                .setDefaults(DEFAULT_LIGHTS | DEFAULT_VIBRATE)
                .setPriority(PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, browserIntent, FLAG_ONE_SHOT))
                .setSmallIcon(R.drawable.ic_error_white_24dp)
                .setContentTitle(title)
                .setContentText(subtitle)
                .build();

        notificationManager.notify(ERROR_NOTIFICATION_ID, notification);
    }

    /**
     * Returns an intent for sharing the given URIs.
     */
    private Intent createShareIntent(ArrayList<Uri> uris) {
        Intent intent;

        if (uris.size() == 1) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }

        intent.setType("application/achso");

        return intent;
    }

    /**
     * Returns a buffered source that reads from the given URI.
     *
     * Accepts the following schemes:
     *   - file
     *   - content
     *   - http
     *   - https
     *
     * @param uri The URI to stream.
     *
     * @throws IOException              If a stream cannot be opened.
     * @throws IllegalArgumentException If the scheme is not supported.
     */
    private BufferedSource getSource(Uri uri) throws IOException {
        InputStream stream;

        if (isLocal(uri)) {
            stream = getContentResolver().openInputStream(uri);
        } else {
            Request request = new Request.Builder().url(uri.toString()).build();
            Response response = httpClient.newCall(request).execute();

            stream = response.body().byteStream();
        }

        return Okio.buffer(Okio.source(stream));
    }

    /**
     * Returns a sanitised version of the given filename that should be safe to use on other
     * systems.
     */
    private String sanitizeFilename(String filename) {
        String replacementCharacter = "_";

        // Windows forbids using any of < > : " / | \ ? *
        String windowsBlacklist = "[<>:\"/\\|\\?\\*]";

        // Dots at the beginning of a file name are problematic on Unix since they hide the file
        String unixBlacklist = "^\\.";

        filename = filename.replaceAll(windowsBlacklist, replacementCharacter);
        filename = filename.replaceAll(unixBlacklist, replacementCharacter);

        return filename;
    }

    /**
     * Returns whether the given URI is local or not.
     *
     * Accepts the following schemes:
     *   - file
     *   - content
     *   - http
     *   - https
     *
     * @throws IllegalArgumentException If the scheme is unknown.
     */
    private boolean isLocal(Uri uri) throws IllegalArgumentException {
        String scheme = uri.getScheme();

        // Assume that URIs without a scheme are local.
        if (scheme == null) {
            return true;
        }

        switch (scheme.trim().toLowerCase()) {
            case "file":
            case "content":
                return true;

            case "http":
            case "https":
                return false;

            default:
                throw new IllegalArgumentException("Unknown scheme " + scheme);
        }
    }

}
