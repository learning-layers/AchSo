/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
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

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.UUID;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.TextSettable;
import fi.aalto.legroup.achso.util.xml.XmlConverter;
import fi.aalto.legroup.achso.util.xml.XmlObject;
import fi.aalto.legroup.achso.util.xml.XmlSerializable;

public class SemanticVideo implements XmlSerializable, TextSettable, SerializableToDB {
	// Modify VideoDBHelper database in case of adding/removing/modifying
	// members of this class!

    public static final int NO_UPLOAD = 0;
    public static final int UPLOAD_PENDING = 0;
    public static final int UPLOADED = 1;
    public static final int UPLOADING = 2;
    public static final int UPLOAD_ERROR = 3;

    protected long mId;
	protected String mTitle;
	protected String mCreator;
	protected String mQrCode;
	protected Date mCreatedAt;
	protected Uri mUri;
	protected Genre mGenre;
	protected Bitmap mThumbMini;
	protected Bitmap mThumbMicro;
    protected int mUploadStatus;
    protected boolean mInLocalDB;
    protected boolean mInCloud;
    protected String mKey;
	protected Location mLocation;
    protected Long mDuration;

    @Override
    public void setText(String text) {
        this.setTitle(text);
    }

    public JSONObject json_dump() {
        JSONObject o = new JSONObject();
        try {
            o.accumulate("title", mTitle);
            o.accumulate("creator", mCreator);
            o.accumulate("qr_code", mQrCode);
            o.accumulate("created_at", mCreatedAt.getTime());
            o.accumulate("video_uri", mUri.toString());
            o.accumulate("genre", englishGenreStrings.get(mGenre));
            o.accumulate("key", mKey);
            if (mLocation != null) {
                o.accumulate("latitude", mLocation.getLatitude());
                o.accumulate("longitude", mLocation.getLongitude());
                o.accumulate("accuracy", mLocation.getAccuracy());
            } else {
                o.accumulate("latitude", 0);
                o.accumulate("longitude", 0);
                o.accumulate("accuracy", 0);
            }
            o.accumulate("duration", mDuration);
            o.accumulate("thumb_uri", "");
        } catch (JSONException e) {
            Log.i("SemanticVideo", "Error building json string.");
            e.printStackTrace();
        }
        return o;
    }


    public enum Genre {
        GoodWork, Problem, TrickOfTrade, SiteOverview
	}

	// LinkedHashMap keeps the insertion order
	public static final LinkedHashMap<Genre, String> genreStrings;
    public static final LinkedHashMap<Genre, String> englishGenreStrings;

	static {
		genreStrings = new LinkedHashMap<Genre, String>();
        genreStrings.put(Genre.GoodWork, App.getContext().getString(R.string.good_work_genre));
        genreStrings.put(Genre.Problem, App.getContext().getString(R.string.problem_genre));
        genreStrings.put(Genre.TrickOfTrade, App.getContext().getString(R.string.trick_of_trade_genre));
        genreStrings.put(Genre.SiteOverview, App.getContext().getString(R.string.site_overview_genre));

        englishGenreStrings = new LinkedHashMap<Genre, String>();
        englishGenreStrings.put(Genre.GoodWork, App.getContext().getString(R.string.good_work_genre_english));
        englishGenreStrings.put(Genre.Problem, App.getContext().getString(R.string.problem_genre_english));
        englishGenreStrings.put(Genre.TrickOfTrade, App.getContext().getString(R.string.trick_of_trade_genre_english));
        englishGenreStrings.put(Genre.SiteOverview, App.getContext().getString(R.string.site_overview_genre_english));
	}

    protected SemanticVideo(long id, String title, Date createdat, Long duration, Uri uri,
                            int genreInt, Bitmap mini, Bitmap micro, String qrcode,
                            Location location, int uploadStatus, String creator, String key) {
        this.mId = id;
        this.mTitle = title;
        this.mCreatedAt = createdat;
        this.mUri = uri;
        this.mQrCode = qrcode;
        this.mGenre = Genre.values()[genreInt];
        this.mUploadStatus = uploadStatus;
        this.mThumbMini = mini;
        this.mThumbMicro = micro;
        this.mLocation = location;
        this.mCreator = creator;
        this.mDuration = duration;
        this.mInLocalDB = true;
        this.mInCloud = (uploadStatus == UPLOADED);
        this.mKey = key;
    }

	// Constructor for VideoDBHelper to reconstruct SemanticVideo object from
	// the database
	protected SemanticVideo(long id, String title, Date createdat, Uri uri,
			int genreInt, Bitmap mini, Bitmap micro, String qrcode,
			Location location, int uploadStatus, String creator, String key) {
        this(id, title, createdat, null, uri, genreInt, mini, micro, qrcode, location,
                uploadStatus, creator, key);
	}

    // This is used when creating SemanticVideo from local recording
	public SemanticVideo(String title, Uri uri, Genre genre, String creator) {
		this.mUri = uri;
		this.mCreatedAt = new Date();
		this.mTitle = title != null ? title : "Untitled";
		this.mGenre = genre;
		this.mLocation = App.last_location;
		this.mQrCode = null;
        this.mUploadStatus = NO_UPLOAD;
		this.mCreator = creator;
        this.mDuration = null;
        this.mInLocalDB = true;
        this.mInCloud = false;
        this.mKey = null;
	}

    public void extractRemoteDatas() {
        /*
        FFmpegMediaMetadataRetriever ffretriever=new FFmpegMediaMetadataRetriever();
        try {
            ffretriever.setDataSource(mUri.toString());
            mDuration = Long.parseLong(ffretriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION));
            mThumbMini = ffretriever.getFrameAtTime();
            mThumbMicro=mThumbMini;
        } catch(IllegalArgumentException e2) {
            mDuration=0l;
        }
        ffretriever.release();
        */
        mDuration=0l;
    }

    public long getDuration(Context ctx) {
        if(mDuration==null) {
            MediaMetadataRetriever retriever=new MediaMetadataRetriever();
            try {
                retriever.setDataSource(ctx, mUri);
                mDuration=Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            } catch(IllegalArgumentException e) {
                mDuration=0l;
            }
            retriever.release();
        }
        return mDuration;
    }

    public boolean inCloud() {
        return mInCloud;
    }

    public void setInCloud(boolean b) {
        this.mInCloud = b;
    }

    public boolean inLocalDB() {
        return mInLocalDB;
    }

    public void setInLocalDB(boolean b) {
        this.mInLocalDB = b;
    }

    //passing the LAS credentials with creator information
	public String getCreator() {
		return mCreator;
	}
	public void setCreator(String creator) {
		mCreator = creator;
	}

	public Location getLocation() {
		return mLocation;
	}

	public String getQrCode() {
		return mQrCode;
	}

	public void setGenre(Genre genre) {
		this.mGenre = genre;
	}

	public Genre getGenre() {
		return this.mGenre;
	}

	public void setTitle(String title) {
		this.mTitle = title;
	}

	public String getTitle() {
		return this.mTitle;
	}

	public int getGenreAsInt() {
		return this.mGenre.ordinal();
	}

	public String getGenreText() {
		return genreStrings.get(mGenre);
	}

    public String getEnglishGenreText() {
        return englishGenreStrings.get(mGenre);
    }

	public String toString() {
		return this.mTitle + "\n" + genreStrings.get(mGenre) + "\n"
				+ mCreatedAt.toString();
	}

	public Uri getUri() {
		return this.mUri;
	}

	public Date getCreationTime() {
		return this.mCreatedAt;
	}

	public long getId() {
		return this.mId;
	}

    public String getKey() {
        return this.mKey;
    }

    public String createKey() {
        if (this.mKey == null || this.mKey.isEmpty()) {
            mKey = UUID.randomUUID().toString();
        }
        return mKey;
    }

	public Bitmap getThumbnail(int type) {
		switch (type) {
		case MediaStore.Images.Thumbnails.MINI_KIND:
			return mThumbMini;
		case MediaStore.Images.Thumbnails.MICRO_KIND:
			return mThumbMicro;
		default:
			return mThumbMini;
		}
	}

    public boolean prepareThumbnails() {

        Log.i("VideoDBHelper", "Creating thumbnails from sv in " + getUri().getPath());
        Bitmap mini = ThumbnailUtils.createVideoThumbnail(getUri().getPath(), MediaStore.Images.Thumbnails.MINI_KIND);

        Bitmap micro = ThumbnailUtils.createVideoThumbnail(getUri().getPath(), MediaStore.Images.Thumbnails.MICRO_KIND);
        if (mini == null) {
            Log.e("SemanticVideo", "Thumbnail mini is null!");
            return false;
        }
        if (micro == null) {
            Log.e("SemanticVideo", "Thumbnail micro is null!");
            return false;
        }
        setThumbnails(mini, micro);
        return true;
    }

    public Pair<Bitmap, Bitmap> getThumbnails() {
		return new Pair(mThumbMini, mThumbMicro);
	}

	protected void setId(long id) {
		this.mId = id;
	}

	protected void setThumbnails(Bitmap mini, Bitmap micro) {
		this.mThumbMini = mini;
		this.mThumbMicro = micro;
	}

    public void setUploadStatus(int status) {
        mUploadStatus = status;
    }

    public boolean isUploaded() {
		return (mUploadStatus == UPLOADED);
	}

	public boolean isUploading() {
        return (mUploadStatus == UPLOADING);
	}

	public boolean isUploadPending() {
        return (mUploadStatus == UPLOAD_PENDING);
	}

    public boolean hasUploadError() {
        return (mUploadStatus == UPLOAD_ERROR);
    }

    public boolean isNeverUploaded() {
        return (mUploadStatus == NO_UPLOAD);
    }


    public void setQrCode(String code) {
		mQrCode = code;
	}

    @Override
    public XmlObject getXmlObject(Context ctx) {
        XmlObject video=new XmlObject("video");
        video.addSubObject("title", getTitle());
        video.addSubObject("genre", getEnglishGenreText());
        if(getQrCode() != null && getQrCode().length() > 0) {
            video.addSubObject("qr_code", getQrCode());
        }
        video.addSubObject("creator", mCreator);
        video.addSubObject("video_uri", mUri.toString());
        video.addSubObject("created_at", mCreatedAt.toString());
        video.addSubObject("duration", Long.toString(getDuration(ctx))); // In milliseconds
        if(getLocation() != null) {
            video.addSubObject(new XmlObject("location")
                .addSubObject("provider", getLocation().getProvider())
                .addSubObject("latitude", Double.toString(getLocation().getLatitude()))
                .addSubObject("longitude", Double.toString(getLocation().getLongitude()))
                .addSubObject("accuracy", Float.toString(getLocation().getAccuracy())));
        }
        VideoDBHelper vdb=new VideoDBHelper(ctx);
        Iterator<Annotation> annotationIterator = vdb.getAnnotations(getId()).iterator();
        XmlObject annXml=new XmlObject("annotations");
        while(annotationIterator.hasNext()) {
            annXml.addSubObject(annotationIterator.next().getXmlObject(ctx));
        }
        vdb.close();
        video.addSubObject(annXml);
        video.addSubObject(new XmlObject("thumb_image")
            .addAttribute("encoding", "base64")
            .addAttribute("name", "thumb.jpg")
            .setText(XmlConverter.bitmapToBase64(mThumbMini)));
        return video;
    }
}
