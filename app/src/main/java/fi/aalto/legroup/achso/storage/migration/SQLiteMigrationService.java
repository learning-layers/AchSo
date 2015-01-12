package fi.aalto.legroup.achso.storage.migration;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;

import com.bugsnag.android.Bugsnag;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.authoring.VideoCreatorService;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.User;

/**
 * Migrates videos from the old SQLite database into the new flat file format. For historical
 * details on the structure of the old database, see http://git.io/uldLWg.
 *
 * TODO: This is to be removed after a sufficient grace period.
 *
 * @author Leo Nikkilä
 */
public class SQLiteMigrationService extends IntentService {

    private static final String TAG = "SQLiteMigrationService";

    private static final int PROGRESS_NOTIFICATION_ID = 1;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String DATABASE_NAME = "videoDB";

    private static final String TBL_VIDEO = "video";
    private static final String TBL_ANNOTATION = "annotation";

    private static final String KEY_VIDEO_ID = "videoid";

    private static final int VIDEO_ID_COLUMN = 0;
    private static final int VIDEO_TITLE_COLUMN = 1;
    private static final int VIDEO_CREATION_DATE_COLUMN = 2;
    private static final int VIDEO_GENRE_ID_COLUMN = 3;
    private static final int VIDEO_URI_COLUMN = 4;
    private static final int VIDEO_AUTHOR_NAME_COLUMN = 8;
    private static final int VIDEO_LOCATION_ACCURACY_COLUMN = 9;
    private static final int VIDEO_LOCATION_LONGITUDE_COLUMN = 10;
    private static final int VIDEO_LOCATION_LATITUDE_COLUMN = 11;
    private static final int VIDEO_TAG_COLUMN = 13;

    private static final int ANNOTATION_TIME_COLUMN = 1;
    private static final int ANNOTATION_POSITION_X_COLUMN = 3;
    private static final int ANNOTATION_POSITION_Y_COLUMN = 4;
    private static final int ANNOTATION_TEXT_COLUMN = 6;
    private static final int ANNOTATION_AUTHOR_NAME_COLUMN = 8;

    private static final String[] GENRES = {
            "Good work",
            "Problem",
            "Trick of trade",
            "Site overview"
    };

    public SQLiteMigrationService() {
        super("SQLiteMigrationService");
    }

    public static void run(Context context) {
        Intent intent = new Intent(context, SQLiteMigrationService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        File databaseFile = getDatabasePath(DATABASE_NAME);

        if (!databaseFile.exists()) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.migration_notification))
                .setOngoing(true)
                .setProgress(0, 100, true)
                .setSmallIcon(R.drawable.ic_sync_white_24dp)
                .build();

        notificationManager.notify(TAG, PROGRESS_NOTIFICATION_ID, notification);

        SQLiteDatabase database = null;

        try {
            database = SQLiteDatabase.openDatabase(databaseFile.getPath(), null,
                    SQLiteDatabase.OPEN_READONLY);

            migrateVideos(database);
        } catch (SQLiteException e) {
            Bugsnag.notify(e);
        } finally {
            if (database != null) {
                database.close();
            }
        }

        notificationManager.cancel(TAG, PROGRESS_NOTIFICATION_ID);
    }

    private void migrateVideos(SQLiteDatabase database) {
        Cursor videoCursor = database.query(TBL_VIDEO, null, null, null, null, null, null);

        while (videoCursor.moveToNext()) {
            UUID id = UUID.randomUUID();

            long originalId = getVideoId(videoCursor);

            Uri videoUri = getVideoUri(videoCursor);
            String genre = getVideoGenre(videoCursor);
            String title = getVideoTitle(videoCursor);
            String tag = getVideoTag(videoCursor);
            Date date = getVideoDate(videoCursor);
            User author = getVideoAuthor(videoCursor);
            Location location = getVideoLocation(videoCursor);

            List<Annotation> annotations = new ArrayList<>();

            Cursor annotationCursor = database.query(TBL_ANNOTATION, null, KEY_VIDEO_ID + " = ?",
                    new String[] { Long.toString(originalId) }, null, null, null);

            while (annotationCursor.moveToNext()) {
                long time = getAnnotationTime(annotationCursor);
                PointF position = getAnnotationPosition(annotationCursor);
                String text = getAnnotationText(annotationCursor);
                User annotationAuthor = getAnnotationAuthor(annotationCursor);

                Annotation annotation = new Annotation(time, position, text, annotationAuthor);
                annotations.add(annotation);
            }

            annotationCursor.close();

            VideoCreatorService.with(this, videoUri, genre)
                    .setId(id)
                    .setTitle(title)
                    .setTag(tag)
                    .setDate(date)
                    .setAuthor(author)
                    .setLocation(location)
                    .setAnnotations(annotations)
                    .create();
        }

        videoCursor.close();
    }

    private long getVideoId(Cursor cursor) {
        return cursor.getLong(VIDEO_ID_COLUMN);
    }

    private String getVideoTitle(Cursor cursor) {
        return cursor.getString(VIDEO_TITLE_COLUMN);
    }

    private Date getVideoDate(Cursor cursor) {
        try {
            String dateString = cursor.getString(VIDEO_CREATION_DATE_COLUMN);
            return DATE_FORMAT.parse(dateString);
        } catch (ParseException e) {
            return new Date();
        }
    }

    private String getVideoGenre(Cursor cursor) {
        int genreId = cursor.getInt(VIDEO_GENRE_ID_COLUMN);
        return GENRES[genreId];
    }

    private Uri getVideoUri(Cursor cursor) {
        String videoUriString = cursor.getString(VIDEO_URI_COLUMN);
        return Uri.parse(videoUriString);
    }

    private User getVideoAuthor(Cursor cursor) {
        if (cursor.isNull(VIDEO_AUTHOR_NAME_COLUMN)) {
            return App.loginManager.getUser();
        }

        String name = cursor.getString(VIDEO_AUTHOR_NAME_COLUMN);
        Uri uri = Uri.EMPTY;

        return new User(name, uri);
    }

    private Location getVideoLocation(Cursor cursor) {
        if (cursor.isNull(VIDEO_LOCATION_LATITUDE_COLUMN)) {
            return null;
        }

        double latitude = cursor.getDouble(VIDEO_LOCATION_LATITUDE_COLUMN);
        double longitude = cursor.getDouble(VIDEO_LOCATION_LONGITUDE_COLUMN);
        float accuracy = cursor.getFloat(VIDEO_LOCATION_ACCURACY_COLUMN);

        Location location = new Location("serialized");

        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);

        return location;
    }

    private String getVideoTag(Cursor cursor) {
        if (cursor.isNull(VIDEO_TAG_COLUMN)) {
            return null;
        }

        return cursor.getString(VIDEO_TAG_COLUMN);
    }

    private long getAnnotationTime(Cursor cursor) {
        return cursor.getLong(ANNOTATION_TIME_COLUMN);
    }

    private PointF getAnnotationPosition(Cursor cursor) {
        float posX = cursor.getFloat(ANNOTATION_POSITION_X_COLUMN);
        float posY = cursor.getFloat(ANNOTATION_POSITION_Y_COLUMN);

        return new PointF(posX, posY);
    }

    private String getAnnotationText(Cursor cursor) {
        return cursor.getString(ANNOTATION_TEXT_COLUMN);
    }

    private User getAnnotationAuthor(Cursor cursor) {
        if (cursor.isNull(ANNOTATION_AUTHOR_NAME_COLUMN)) {
            return App.loginManager.getUser();
        }

        String name = cursor.getString(ANNOTATION_AUTHOR_NAME_COLUMN);
        Uri uri = Uri.EMPTY;

        return new User(name, uri);
    }

}
