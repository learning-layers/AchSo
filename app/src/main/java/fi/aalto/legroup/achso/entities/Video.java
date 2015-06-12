package fi.aalto.legroup.achso.entities;

import android.location.Location;
import android.net.Uri;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.storage.VideoRepository;

/**
 * A video entity that represents a video and is an aggregate root for annotations.
 *
 * TODO: Extending VideoInfo is semantically wrong and may result in some problems later on.
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
    protected Date date;

    protected User author;
    protected Location location;
    protected List<Annotation> annotations;

    Video() {
        // For serialization and pooling
    }

    public Video(VideoRepository repository, Uri manifestUri, Uri videoUri, Uri thumbUri, UUID id,
                 String title, String genre, String tag, Date date, User author, Location location,
                 List<Annotation> annotations) {

        this.manifestUri = manifestUri;
        this.videoUri = videoUri;
        this.thumbUri = thumbUri;
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.tag = tag;
        this.date = date;

        this.repository = repository;
        this.author = author;
        this.location = location;
        this.annotations = annotations;
    }

    protected static <T> T mergeValue(T a, T b, T old, boolean preferA) {
        if (com.google.common.base.Objects.equal(a, b)) {
            return a;
        }

        if (old != null) {
            if (Objects.equal(a, old))
                return b;
            if (Objects.equal(b, old))
                return a;
        }

        return preferA ? a : b;
    }

    /**
     * Merge two video manifests.
     * @param a The first video to merge.
     * @param b The second video to merge.
     * @param old An shared ancestor for the two videos to merge. May be null but results in a worse merge result.
     */
    public static Video merge(Video a, Video b, Video old) throws MergeException {
        boolean preferA = a.getLastModified().getTime() > b.getLastModified().getTime();

        if (!Objects.equal(a.getId(), b.getId())) {
            throw new MergeException("Trying to merge two unrelated videos");
        }

        Video video = new Video();
        video.setId(a.getId());
        video.setVideoUri(mergeValue(a.getVideoUri(), b.getVideoUri(), old != null? old.getVideoUri() : null, preferA));
        video.setThumbUri(mergeValue(a.getThumbUri(), b.getThumbUri(), old != null ? old.getThumbUri() : null, preferA));
        video.setTitle(mergeValue(a.getTitle(), b.getTitle(), old != null ? old.getTitle() : null, preferA));
        video.setGenre(mergeValue(a.getGenre(), b.getGenre(), old != null ? old.getGenre() : null, preferA));
        video.setTag(mergeValue(a.getTag(), b.getTag(), old != null ? old.getTag() : null, preferA));
        video.setDate(mergeValue(a.getDate(), b.getDate(), old != null ? old.getDate() : null, preferA));
        video.setAuthor(mergeValue(a.getAuthor(), b.getAuthor(), old != null ? old.getAuthor() : null, preferA));
        video.setLocation(mergeValue(a.getLocation(), b.getLocation(), old != null ? old.getLocation() : null, preferA));

        Set<Annotation> aa = new HashSet<>(a.getAnnotations());
        Set<Annotation> ab = new HashSet<>(b.getAnnotations());
        Set<Annotation> ao = old != null ? new HashSet<>(old.getAnnotations()) : Collections.<Annotation>emptySet();
        Set<Annotation> all = new HashSet<>(aa.size() + ab.size());
        all.addAll(aa);
        all.addAll(ab);
        
        List<Annotation> merged = new ArrayList<>(aa.size() + ab.size());

        for (Annotation an : all) {

            if (aa.contains(an) && ab.contains(an)) {
                // Both contain the annotation, so keep it.
                merged.add(an);
            }

            // Here the annotation is missing from one of the videos to merge.

            if (ao.contains(an)) {
                // If the common ancestor contains the annotation it means it wasn't touched in the other one and deleted in the other one, so we can delete it since it wasn't touched in the other video.
                continue;
            }

            // The old one doesn't contain it so it is new in one of the videos, keep it.
            merged.add(an);
        }
        video.setAnnotations(merged);

        return video;
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

    public Date getDate() {
        return this.date;
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
