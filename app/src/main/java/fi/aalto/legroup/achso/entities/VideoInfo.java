package fi.aalto.legroup.achso.entities;

import android.net.Uri;

import java.util.Date;
import java.util.UUID;

import fi.aalto.legroup.achso.serialization.json.JsonSerializable;

/**
 * An information object that describes a video entity.
 *
 * @author Leo Nikkilä
 */
public class VideoInfo implements JsonSerializable {

    protected Uri videoUri;
    protected Uri thumbUri;
    protected UUID id;
    protected String title;
    protected String genre;
    protected String tag;
    protected Date date;

    @SuppressWarnings("UnusedDeclaration")
    protected VideoInfo() {
        // For serialization
    }

    protected VideoInfo(Uri videoUri, Uri thumbUri, UUID id, String title, String genre,
                        String tag, Date date) {

        this.videoUri = videoUri;
        this.thumbUri = thumbUri;
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.tag = tag;
        this.date = date;
    }

    public boolean isLocal() {
        // Uris without a scheme are assumed to be local
        if (this.videoUri.isRelative()) return true;

        String scheme = getVideoUri().getScheme().trim().toLowerCase();

        switch (scheme) {
            case "file":
            case "content":
                return true;

            default:
                return false;
        }
    }

    public boolean isRemote() {
        return !isLocal();
    }

    public Uri getVideoUri() {
        return this.videoUri;
    }

    public Uri getThumbUri() {
        return this.thumbUri;
    }

    public UUID getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getGenre() {
        return this.genre;
    }

    public String getTag() {
        return this.tag;
    }

    public Date getDate() {
        return this.date;
    }

}
