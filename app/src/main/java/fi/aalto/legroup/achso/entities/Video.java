package fi.aalto.legroup.achso.entities;

import android.location.Location;
import android.net.Uri;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.storage.VideoRepository;

/**
 * A video entity that represents a video and is an aggregate root for annotations.
 */
public class Video implements JsonSerializable {

    protected transient Uri manifestUri;
    protected transient VideoRepository repository;
    protected transient Date lastModified;

    protected Uri videoUri;
    protected Uri thumbUri;
    protected UUID id;
    protected String title;
    protected String genre;
    protected String tag;
    protected int rotation;
    protected Date date;
    protected int revision;

    protected User author;
    protected Location location;
    protected List<Annotation> annotations;

    Video() {
        // For serialization and pooling
        revision = 0;
    }

    public Video(VideoRepository repository, Uri manifestUri, Uri videoUri, Uri thumbUri, UUID id,
                 String title, String genre, String tag, int rotation, Date date, User author,
                 Location location, List<Annotation> annotations) {

        this.manifestUri = manifestUri;
        this.videoUri = videoUri;
        this.thumbUri = thumbUri;
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.tag = tag;
        this.rotation = rotation;
        this.date = date;

        this.repository = repository;
        this.author = author;
        this.location = location;
        this.annotations = annotations;
    }
    
    /**
     * Convenience method for saving a video.
     * @return True if succeeded, false otherwise.
     */
    public boolean save() {
        try {
            this.repository.save(this);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    public Uri getManifestUri() {
        return manifestUri;
    }

    public void setManifestUri(Uri manifestUri) {
        this.manifestUri = manifestUri;
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

    public int getRotation() {
        return this.rotation;
    }

    public Date getDate() {
        return this.date;
    }

    public int getRevision() {
        return this.revision;
    }


    public void setRepository(VideoRepository repository) {
        this.repository = repository;
    }

    public VideoRepository getRepository() { return this.repository; }

    public void setVideoUri(Uri videoUri) {
        this.videoUri = videoUri;
    }

    public void setThumbUri(Uri thumbUri) {
        this.thumbUri = thumbUri;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }


    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Date getLastModified() {
        return this.lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public List<Annotation> getAnnotations() {
        if (this.annotations == null) {
            this.annotations = new ArrayList<>();
        }

        return this.annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

}
