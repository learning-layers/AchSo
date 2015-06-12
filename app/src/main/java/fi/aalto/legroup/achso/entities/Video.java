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

    /**
     * Chooses a value to use in a merged video result.
     * @param a First option for the value.
     * @param b Second option for the value.
     * @param old The common ancestor value of `a` and `b`. Is null in two-way merges.
     * @param preferA Used for tie-breaking if there isn't enough info to decide will use this.
     * @return Either the value of `a` or `b` depending on which is the better choice for merging.
     */
    protected static <T> T chooseMergeValue(T a, T b, T old, boolean preferA) {
        if (Objects.equal(a, b)) {
            // Simple case: The values are the same, no need to merge.
            return a;
        }

        if (old != null) {
            // If this is a three-way merge and one of the values is the same as the common
            // ancestor then we assume that it's unchanged and return the other one.
            if (Objects.equal(a, old))
                return b;
            if (Objects.equal(b, old))
                return a;
        }

        // TODO: The tiebreaker could use some kind of a dialog if we want to be super explicit
        // about losing work
        // In this case either both have changed his is a two-way merge, the only option is to
        // use some tiebreaker, for example which file was more recently modified.
        return preferA ? a : b;
    }

    /**
     * Merge two video manifests.
     * @param a The first video to merge.
     * @param b The second video to merge.
     * @param old An shared ancestor for the two videos to merge. May be null but results in a
     *            worse merge result. For example annotations can't be deleted in a two-way merge.
     */
    public static Video merge(Video a, Video b, Video old) throws MergeException {

        // The UUID:s _must_ match, otherwise the merge doesn't make any sense.
        if (!Objects.equal(a.getId(), b.getId())) {
            throw new MergeException("Trying to merge two unrelated videos");
        }

        Video video = new Video();
        video.setId(a.getId());

        // Prefer newer values if no other option
        boolean preferA = a.getLastModified().getTime() > b.getLastModified().getTime();

        // Simply choose one of the values for the simple fields (merging title additions changes
        // be too complicated in scope for this for example)
        video.setVideoUri(chooseMergeValue(a.getVideoUri(), b.getVideoUri(),
                old != null ? old.getVideoUri() : null, preferA));
        video.setThumbUri(chooseMergeValue(a.getThumbUri(), b.getThumbUri(),
                old != null ? old.getThumbUri() : null, preferA));
        video.setTitle(chooseMergeValue(a.getTitle(), b.getTitle(),
                old != null ? old.getTitle() : null, preferA));
        video.setGenre(chooseMergeValue(a.getGenre(), b.getGenre(),
                old != null ? old.getGenre() : null, preferA));
        video.setTag(chooseMergeValue(a.getTag(), b.getTag(),
                old != null ? old.getTag() : null, preferA));
        video.setDate(chooseMergeValue(a.getDate(), b.getDate(),
                old != null ? old.getDate() : null, preferA));
        video.setAuthor(chooseMergeValue(a.getAuthor(), b.getAuthor(),
                old != null ? old.getAuthor() : null, preferA));
        video.setLocation(chooseMergeValue(a.getLocation(), b.getLocation(),
                old != null ? old.getLocation() : null, preferA));

        // Put all the annotations in Sets so we can quickly check for identical ones.
        Set<Annotation> annotationsA = new HashSet<>(a.getAnnotations());
        Set<Annotation> annotationsB = new HashSet<>(b.getAnnotations());
        Set<Annotation> annotationsOld = old != null ? new HashSet<>(old.getAnnotations())
                : Collections.<Annotation>emptySet();

        int reservedAnnotationCount = annotationsA.size() + annotationsB.size();

        List<Annotation> mergedAnnotations = new ArrayList<>(reservedAnnotationCount);
        Set<Annotation> annotationsAll = new HashSet<>(reservedAnnotationCount);

        // Because `annotationsAll` is a Set we don't have to worry about duplicate Annotations.
        annotationsAll.addAll(annotationsA);
        annotationsAll.addAll(annotationsB);

        // Iterate through every annotation in `a` and `b`
        for (Annotation annotation : annotationsAll) {

            // annotation: An annotation which is in at least one of the videos to merge

            if (annotationsA.contains(annotation) && annotationsB.contains(annotation)) {
                // Both videos contain the annotation, so keep it for sure.
                mergedAnnotations.add(annotation);
            }

            // Here the annotation is missing from one of the videos to merge.

            if (annotationsOld.contains(annotation)) {
                // If the common ancestor contains the annotation it means it wasn't touched in
                // the other one and deleted in the other one, so we can delete it since it
                // wasn't touched in the other video.
                continue;
            }

            // The old one doesn't contain it so it is new in one of the videos, keep it. This
            // can result in double annotations in the case that two people for example move a
            // single annotation at the same time, but this should be rare and this way it's
            // impossible to lose work due to merging.
            mergedAnnotations.add(annotation);
        }

        video.setAnnotations(mergedAnnotations);

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
