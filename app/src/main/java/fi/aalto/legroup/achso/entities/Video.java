package fi.aalto.legroup.achso.entities;

import android.location.Location;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.storage.VideoRepository;

/**
 * A video entity that represents a video and is an aggregate root for annotations.
 *
 * TODO: Extending VideoInfo is semantically wrong and may result in some problems later on.
 */
public class Video extends VideoInfo implements JsonSerializable {

    protected transient VideoRepository repository;

    protected User author;
    protected Location location;
    protected List<Annotation> annotations;

    @SuppressWarnings("UnusedDeclaration")
    private Video() {
        // For serialization
        super();
    }

    public Video(VideoRepository repository, Uri uri, UUID id, String title, String genre,
                 String tag, Date date, User author, Location location,
                 List<Annotation> annotations) {

        super(uri, id, title, genre, tag, date);

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

    public void setRepository(VideoRepository repository) {
        this.repository = repository;
    }

    public void setVideoUri(Uri uri) {
        this.uri = uri;
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

    public void setDate(Date date) {
        this.date = date;
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
