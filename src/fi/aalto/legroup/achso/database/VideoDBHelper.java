/**
 * Copyright 2013 Aalto university, see AUTHORS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.util.FloatPosition;

public class VideoDBHelper extends SQLiteOpenHelper {
    public static final String KEY_ID = "id";
    public static final String KEY_URI = "uri";
    public static final String KEY_TITLE = "title";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_GENRE = "genre";
    public static final String KEY_THUMB_MINI = "thumbnail_mini";
    public static final String KEY_THUMB_MICRO = "thumbnail_micro";
    public static final String KEY_UPLOADED = "uploaded";
    public static final String KEY_CREATOR = "creator";
    public static final String KEY_STARTTIME = "starttime";
    public static final String KEY_DURATION = "duration";
    public static final String KEY_POSITION_X = "xposition";
    public static final String KEY_POSITION_Y = "yposition";
    public static final String KEY_VIDEO_ID = "videoid";
    public static final String KEY_TEXT = "text";
    public static final String KEY_SCALE = "scale";
    public static final String KEY_ACCURACY = "accuracy";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_PROVIDER = "provider";
    public static final String KEY_QRCODE = "qr_code";
    public static final String KEY_HASHKEY = "key";
    private static final int DBVER = 14; // Increase this if you make changes to the database
    // structure
    private static final String DBNAME = "videoDB";
    private static final String TBL_VIDEO = "video";
    private static final String TBL_GENRE = "genre";
    private static final String TBL_ANNOTATION = "annotation";
    private static final SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Datetime format that sqlite3 uses
    private static List<SemanticVideo> mLocalVideoCache;

    private Context mContext;

    public VideoDBHelper(Context c) {
        super(c, DBNAME, null, DBVER);
        mContext = c;
    }

    public static List<SemanticVideo> queryVideoCacheByTitle(String query) {
        List<SemanticVideo> ret = new ArrayList<SemanticVideo>();
        for (SemanticVideo v : mLocalVideoCache) {
            if (v.getTitle().toLowerCase().contains(query.toLowerCase())) ret.add(v);
        }
        return ret;
    }

    public static List<SemanticVideo> getVideosByQrCode(String query) {
        List<SemanticVideo> ret = new ArrayList<SemanticVideo>();
        for (SemanticVideo v : mLocalVideoCache) {
            if (v.getQrCode() != null && v.getQrCode().equals(query)) ret.add(v);
        }
        return ret;
    }

    public static List<SemanticVideo> getVideosByGenre(String g) {
        List<SemanticVideo> ret = new ArrayList<SemanticVideo>();
        Log.i("VideoDBHelper", "Getting videos by genre: "+ g);
        for (SemanticVideo v : mLocalVideoCache) {
            if (v.getEnglishGenreText().equals(g)) {
                ret.add(v);
            }
        }
        return ret;
    }

    public static List<SemanticVideo> getVideoCache() {
        return mLocalVideoCache;
    }

    public static void sortVideoCache(final String sortBy, final boolean desc) {
        Collections.sort(mLocalVideoCache, new Comparator<SemanticVideo>() {
            @Override
            public int compare(SemanticVideo lhs, SemanticVideo rhs) {
                if (sortBy.equals(KEY_GENRE)) {
                    if (desc) return rhs.getGenre().compareTo(lhs.getGenre());
                    else return lhs.getGenre().compareTo(rhs.getGenre());
                } else if (sortBy.equals(KEY_TITLE)) {
                    if (desc) return rhs.getTitle().compareToIgnoreCase(lhs.getTitle());
                    else return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                } else {
                    if (desc) return rhs.getCreationTime().compareTo(lhs.getCreationTime());
                    else return lhs.getCreationTime().compareTo(rhs.getCreationTime());
                }
            }
        });
    }

    public static SemanticVideo getById(long id) {
        if (mLocalVideoCache == null) return null;
        for (SemanticVideo v : mLocalVideoCache) {
            if (v.getId() == id) return v;
        }
        return null;
    }

    public static SemanticVideo getByUri(Uri uri) {
        if (mLocalVideoCache == null) return null;
        for (SemanticVideo v : mLocalVideoCache) {
            if (v.getUri().equals(uri)) return v;
        }
        return null;
    }


    public static SemanticVideo getByPosition(int pos) {
        return mLocalVideoCache.get(pos);
    }

    public static List<SemanticVideo> fakeListOfVideos(int amount) {
        List<SemanticVideo> ret = new ArrayList<SemanticVideo>();
        SemanticVideo sv = mLocalVideoCache.get(0);
        if (sv != null) {
            for (int i = 0; i < amount; ++i) {
                ret.add(sv);
            }
        }
        return ret;
    }

    private ContentValues getContentValues(SemanticVideo sv, Pair<Bitmap, Bitmap> thumbnails) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_URI, sv.getUri().toString());
        cv.put(KEY_TITLE, sv.getTitle());
        cv.put(KEY_CREATED_AT, mDateFormatter.format(sv.getCreationTime()));
        cv.put(KEY_GENRE, sv.getGenreAsInt());
        if (thumbnails.first != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Bitmap tmini = thumbnails.first;
            tmini.compress(Bitmap.CompressFormat.PNG, 0, baos); // Quality setting is ignored for PNG
            cv.put(KEY_THUMB_MINI, baos.toByteArray());
        } else {
            Log.e("VideoDBHelper", "Thumbnail.first is null!");
        }
        if (thumbnails.second != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Bitmap tmicro = thumbnails.second;
            tmicro.compress(Bitmap.CompressFormat.PNG, 0, baos); // Quality setting is ignored for PNG
            cv.put(KEY_THUMB_MICRO, baos.toByteArray());
        } else {
            Log.e("VideoDBHelper", "Thumbnail.second is null!");
        }
        cv.put(KEY_UPLOADED, sv.isUploaded());

        Location loc = sv.getLocation();
        cv.put(KEY_ACCURACY, loc != null ? loc.getAccuracy() : null);
        cv.put(KEY_LATITUDE, loc != null ? loc.getLatitude() : null);
        cv.put(KEY_LONGITUDE, loc != null ? loc.getLongitude() : null);
        cv.put(KEY_PROVIDER, loc != null ? loc.getProvider() : null);

        cv.put(KEY_QRCODE, sv.getQrCode());
        cv.put(KEY_CREATOR, sv.getCreator());
        return cv;
    }

    private ContentValues getContentValues(Annotation a) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_STARTTIME, a.getStartTime());
        cv.put(KEY_DURATION, a.getDuration());
        cv.put(KEY_POSITION_X, a.getPosition().getX());
        cv.put(KEY_POSITION_Y, a.getPosition().getY());
        cv.put(KEY_VIDEO_ID, a.getVideoId());
        cv.put(KEY_TEXT, a.getText());
        cv.put(KEY_SCALE, a.getScaleFactor());
        cv.put(KEY_CREATOR, a.getCreator());
        return cv;
    }

    public void insert(SerializableToDB o) {
        if (o instanceof SemanticVideo) {
            SemanticVideo sv = (SemanticVideo) o;
            insert(sv);
        } else if (o instanceof Annotation) {
            Annotation a = (Annotation) o;
            insert(a);
        }
    }

    public void update(SerializableToDB o) {
        if (o instanceof SemanticVideo) {
            SemanticVideo sv = (SemanticVideo) o;
            update(sv);
        } else if (o instanceof Annotation) {
            Annotation a = (Annotation) o;
            update(a);
        }
    }

    public void delete(SerializableToDB o) {
        if (o instanceof SemanticVideo) {
            SemanticVideo sv = (SemanticVideo) o;
            delete(sv);
        } else if (o instanceof Annotation) {
            Annotation a = (Annotation) o;
            delete(a);
        }
    }

    private void insert(SemanticVideo sv) {
        SQLiteDatabase db = this.getWritableDatabase();
        Pair<Bitmap, Bitmap> thumbs = sv.getThumbnails();
        ContentValues cv = getContentValues(sv, thumbs);
        long id = db.insertOrThrow(TBL_VIDEO, null, cv);
        sv.setId(id);
        mLocalVideoCache.add(0, sv);
        db.close();
    }

    private void insert(Annotation a) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = db.insertOrThrow(TBL_ANNOTATION, null, getContentValues(a));
        ((AnnotationBase) a).setId(id);
        db.close();
    }

    private void update(SemanticVideo sv) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] whereargs = {Long.toString(sv.getId())};
        // Update will now compress thumbnails repeatedly, but as png is lossless it's just a bit of extra work
        db.update(TBL_VIDEO, getContentValues(sv, sv.getThumbnails()), KEY_ID + "=?", whereargs);
        db.close();
    }

    private void update(Annotation a) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] whereargs = {Long.toString(a.getId())};
        db.update(TBL_ANNOTATION, getContentValues(a), KEY_ID + "=?", whereargs);
        db.close();
    }

    private void delete(Annotation a) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] whereargs = {Long.toString(a.getId())};
        db.delete(TBL_ANNOTATION, KEY_ID + "=?", whereargs);
        db.close();
    }

    private void delete(SemanticVideo sv) {
        List<Annotation> videoAnnotations = getAnnotations(sv.getId());
        for (Annotation a : videoAnnotations) {
            delete(a); // Again, these could be merged to one database transaction
        }
        File f = new File(sv.getUri().getPath());
        f.delete();
        mLocalVideoCache.remove(sv);
        SQLiteDatabase db = this.getWritableDatabase();
        String[] whereargs = {Long.toString(sv.getId())};
        db.delete(TBL_VIDEO, KEY_ID + "=?", whereargs);
        db.close();
    }

    private Annotation getAnnotationFromCursor(Cursor c) {
        int i = 0;
        long id = c.getLong(i++);
        long starttime = c.getLong(i++);
        long duration = c.getLong(i++);
        float x = c.getFloat(i++);
        float y = c.getFloat(i++);
        long vid = c.getLong(i++);
        String text = c.getString(i++);
        if (text == null) text = "";
        float scale = c.getFloat(i++);
        String creator = c.getString(i++);
        Annotation a = new Annotation(mContext, vid, starttime, text, new FloatPosition(x, y),
                scale, creator);
        ((AnnotationBase) a).setId(id);
        return a;
    }

    public List<Annotation> getAnnotations(long videoid) {
        List<Annotation> ret = new ArrayList<Annotation>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] whereargs = {Long.toString(videoid)};
        Cursor c = db.query(TBL_ANNOTATION, null, KEY_VIDEO_ID + "=?", whereargs, null, null,
                null);
        if (c.getCount() > 0) {
            while (c.moveToNext()) {
                ret.add(getAnnotationFromCursor(c));
            }
        }
        c.close();
        db.close();
        return ret;
    }

    public Annotation getAnnotationById(long videoId, long annotationId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] whereargs = {Long.toString(videoId), Long.toString(annotationId)};
        Cursor c = db.query(TBL_ANNOTATION, null, KEY_VIDEO_ID + "=? AND " + KEY_ID + "=?",
                whereargs, null, null, null);
        if (c.getCount() < 1) return null;
        c.moveToNext();
        Annotation ret = getAnnotationFromCursor(c);
        c.close();
        db.close();
        return ret;
    }

    public List<SemanticVideo> updateVideoCache() {
        Log.i("VideoDBHelper", "Updating videoCache, no args given");
        return updateVideoCache(null, true);
    }

    public List<SemanticVideo> updateVideoCache(String sortBy, boolean desc) {
        Log.i("VideoDBHelper", "Updating videoCache with args");
        if (sortBy == null) sortBy = KEY_CREATED_AT;
        if (mLocalVideoCache != null) { // Recycle existing bitmaps before loading them from the database.
            for (SemanticVideo sv : mLocalVideoCache) {
                Bitmap micro = sv.getThumbnail(MediaStore.Images.Thumbnails.MICRO_KIND);
                Bitmap mini = sv.getThumbnail(MediaStore.Images.Thumbnails.MINI_KIND);
                if (micro != null) micro.recycle();
                if (mini != null) mini.recycle();
            }
        }
        mLocalVideoCache = fetchVideosFromDB(sortBy, desc);
        return mLocalVideoCache;
    }

    private List<SemanticVideo> fetchVideosFromDB(String sortBy, boolean desc) {
        List<SemanticVideo> ret = new ArrayList<SemanticVideo>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TBL_VIDEO, null, null, null, null, null, sortBy + " COLLATE NOCASE " + (desc ? "DESC" : ""));
        if (c.getCount() > 0) {
            while (c.moveToNext()) {
                int i = 0;
                long id = c.getLong(i++);
                String title = c.getString(i++);
                Date createdat = new Date();
                try {
                    createdat = mDateFormatter.parse(c.getString(i++));
                } catch (ParseException e) {
                    //Log.d("DateFormatter", e.toString());
                }
                int genreInt = c.getInt(i++);
                Uri uri = Uri.parse(c.getString(i++));
                byte[] minib = c.getBlob(i++);
                byte[] microb = c.getBlob(i++);
                Bitmap mini = BitmapFactory.decodeByteArray(minib, 0, minib.length);
                Bitmap micro = BitmapFactory.decodeByteArray(microb, 0, microb.length);

                int uploadStatus = c.getInt(i++) == 1 ? SemanticVideo.UPLOADED : SemanticVideo.NO_UPLOAD;
                String creator = null;
                if (!c.isNull(i)) {
                    creator = c.getString(i++);
                } else i++;
                Location loc = null;
                if (!c.isNull(i)) {
                    float accuracy = c.getFloat(i++);
                    double longitude = c.getDouble(i++);
                    double latitude = c.getDouble(i++);
                    String provider = c.getString(i++);

                    loc = new Location(provider);
                    loc.setAccuracy(accuracy);
                    loc.setLongitude(longitude);
                    loc.setLatitude(latitude);
                } else i += 4;

                String qrCode = null;
                if (!c.isNull(i)) {
                    qrCode = c.getString(i++);
                }
                String key = null;
                if (!c.isNull(i)) {
                    key = c.getString(i++);
                }


                ret.add(new SemanticVideo(id, title, createdat, uri, genreInt, mini, micro,
                        qrCode, loc, uploadStatus, creator, key));
            }
        }
        c.close();
        db.close();
        return ret;
    }

    public int getNumberOfVideosToday() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TBL_VIDEO, null, KEY_CREATED_AT + ">= date('now','start of day')", null, null, null, null);
        int ret = c.getCount();
        c.close();
        db.close();
        return ret;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TBL_GENRE + "(" +
                        KEY_ID + " INTEGER PRIMARY KEY, " +
                        KEY_TITLE + " TEXT NOT NULL" +
                        ")"
        );
        db.execSQL("CREATE TABLE " + TBL_VIDEO + "(" +
                        KEY_ID + " INTEGER PRIMARY KEY," +
                        KEY_TITLE + " TEXT, " +
                        KEY_CREATED_AT + " DATETIME NOT NULL, " +
                        KEY_GENRE + " INTEGER NOT NULL, " +
                        KEY_URI + " TEXT UNIQUE NOT NULL, " +
                        KEY_THUMB_MINI + " BLOB NOT NULL, " +
                        KEY_THUMB_MICRO + " BLOB NOT NULL, " +
                        KEY_UPLOADED + " BOOLEAN NOT NULL, " +
                        KEY_CREATOR + " TEXT, " +
                        KEY_ACCURACY + " FLOAT, " +
                        KEY_LATITUDE + " DOUBLE, " +
                        KEY_LONGITUDE + " DOUBLE, " +
                        KEY_PROVIDER + " TEXT, " +
                        KEY_QRCODE + " TEXT, " +
                        KEY_HASHKEY + " TEXT, " +
                        "FOREIGN KEY(" + KEY_GENRE + ") REFERENCES " + TBL_GENRE + "(" + KEY_ID + ")" +
                        ")"
        );
        db.execSQL("CREATE TABLE " + TBL_ANNOTATION + "(" +
                        KEY_ID + " INTEGER PRIMARY KEY, " +
                        KEY_STARTTIME + " INTEGER NOT NULL, " +
                        KEY_DURATION + " INTEGER, " +
                        KEY_POSITION_X + " FLOAT NOT NULL, " +
                        KEY_POSITION_Y + " FLOAT NOT NULL, " +
                        KEY_VIDEO_ID + " INTEGER NOT NULL, " +
                        KEY_TEXT + " TEXT, " +
                        KEY_SCALE + " FLOAT NOT NULL DEFAULT '1.0'," +
                        KEY_CREATOR + " TEXT, " +
                        "FOREIGN KEY(" + KEY_VIDEO_ID + ") REFERENCES " + TBL_VIDEO + "(" + KEY_ID + ")" +
                        ")"
        );
        SemanticVideo.Genre[] genres = SemanticVideo.Genre.values();
        for (int i = 0; i < genres.length; ++i) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_ID, i);
            cv.put(KEY_TITLE, genres[i].ordinal());
            db.insert(TBL_GENRE, null, cv);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("VideoDBHelper", "Downgrading database, hope you know what you are doing.");
    }

        @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("VideoDBHelper", "visiting onUpgrade -method, oldVersion: " + oldVersion + " new " +
                "version: " + newVersion);
        for (int i = oldVersion; i < newVersion; i++)
        {
            switch(i)
            {
                case 10:
                    // ok it cannot be done in SqLite
                    //db.execSQL("ALTER TABLE video DROP CONSTRAINT UNIQUE");
                    break;
                case 11:
                    Log.i("VideoDBHelper *** upgrade", "Upgrading annotation table to have scale " +
                            "-column");
                    db.execSQL("ALTER TABLE annotation ADD COLUMN "+ KEY_SCALE + " FLOAT NOT NULL" +
                            " DEFAULT 1.0");
                    break;
                case 12:
                    Log.i("VideoDBHelper *** upgrade", "Upgrading video table to have hashkey  " +
                            "-column");
                    db.execSQL("ALTER TABLE video ADD COLUMN "+ KEY_HASHKEY + " TEXT");
                    break;
                case 13:
                    Log.i("VideoDBHelper *** upgrade", "Upgrading annotation table to have " +
                            "creator -column");
                    db.execSQL("ALTER TABLE annotation ADD COLUMN "+ KEY_CREATOR + " TEXT");
                    break;

            }
        }

        //db.execSQL("DROP TABLE IF EXISTS " + TBL_VIDEO);
        //db.execSQL("DROP TABLE IF EXISTS " + TBL_GENRE);
        //db.execSQL("DROP TABLE IF EXISTS " + TBL_ANNOTATION);
        //onCreate(db);
    }
    /*
    static {
        dump();
    }

    // Can be used to dump the database to sdcard. Only for debugging/backupping purposes, do not use otherwise!
    private static void dump() {
        Log.i("VideoDBHelper", String.format("Dumping database from %s to %s/dp_dump.db", getContext().getDatabasePath(DBNAME).getPath(), Environment.getExternalStorageDirectory().getPath()));
        File f= getContext().getDatabasePath(DBNAME);
        FileInputStream fis=null;
        FileOutputStream fos=null;
        try
        {
            fis=new FileInputStream(f);
            fos=new FileOutputStream(getContext().getExternalFilesDir(null) + "/db_dump.db");
            byte[] buf=new byte[1024];
            while(true) {
                int i=fis.read(buf);
                if(i!=-1) fos.write(buf, 0, i);
                else break;
            }
            fos.flush();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                fos.close();
                fis.close();
            }
            catch(IOException ioe) {}
        }
    }
    */
}
