package fi.aalto.legroup.achso.entities;

import android.net.Uri;

import com.google.common.base.Objects;

import java.util.Date;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;

/**
 * An information object that describes a video entity.
 */
public class VideoInfo implements JsonSerializable {

    protected transient Uri uri;

    protected UUID id;
    protected String title;
    protected String genre;
    protected String tag;
    protected Date date;

    @SuppressWarnings("UnusedDeclaration")
    protected VideoInfo() {
        // For serialization
    }

    protected VideoInfo(Uri uri, UUID id, String title, String genre, String tag, Date date) {
        this.uri = uri;
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.tag = tag;
        this.date = date;
    }

    public boolean isLocal() {
        // Uris without a scheme are assumed to be local
        if (this.uri.isRelative()) {
            return true;
        }

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
        return this.uri;
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

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        VideoInfo that = (VideoInfo) object;

        return Objects.equal(uri, that.uri)
                && Objects.equal(id, that.id)
                && Objects.equal(title, that.title)
                && Objects.equal(genre, that.genre)
                && Objects.equal(tag, that.tag)
                && Objects.equal(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri, id, title, genre, tag, date);
    }

}
