package fi.aalto.legroup.achso.entities;

import android.location.Location;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.storage.VideoRepository;

/**
 * A video entity that represents a video and is an aggregate root for annotations.
 */
public class Video implements JsonSerializable {

    /**
     * This is the current video format version that is saved by the app.
     * If you plan to increment this, make sure you add a {@code VideoMigration } to update the old
     * videos. Remember to add the migration to {@code VideoMigration#allMigrations }
     */
    public static int VIDEO_FORMAT_VERSION = 1;

    protected transient Uri manifestUri;
    protected transient VideoRepository repository;
    protected transient Date lastModified;
    protected static final Pattern  uuidPattern = Pattern.compile("");


    protected Uri videoUri;
    protected Uri thumbUri;
    protected Uri deleteUri;
    protected Uri videoCacheUri;
    protected Uri thumbCacheUri;

    protected UUID id;
    protected String title;
    protected String tag;
    protected int rotation;
    protected Date date;
    protected int revision;
    protected int formatVersion;
    protected transient boolean isTemporary;

    protected int startTime;
    protected int endTime;

    protected User author;
    protected Location location;
    protected List<Annotation> annotations;
    protected transient boolean isLastAnnotationEmpty;


    Video() {
        // For serialization and pooling
        revision = 0;
        formatVersion = 0;
    }

    public Video(VideoRepository repository, Uri manifestUri, Uri videoUri, Uri thumbUri,
                 Uri videoCacheUri, Uri thumbCacheUri,  UUID id, String title,
                 String tag, int rotation, Date date, User author,
                 Location location, int formatVersion, List<Annotation> annotations) {

        this.manifestUri = manifestUri;
        this.videoUri = videoUri;
        this.thumbUri = thumbUri;
        this.thumbCacheUri = thumbCacheUri;
        this.videoCacheUri = videoCacheUri;
        this.id = id;
        this.title = title;
        this.tag = tag;
        this.rotation = rotation;
        this.date = date;

        this.repository = repository;
        this.author = author;
        this.location = location;
        this.formatVersion = formatVersion;
        this.annotations = annotations;

        this.isLastAnnotationEmpty = false;

        // Flag to indicate whether or not video should be persisted on cache
        // Eg. in search results...
        this.isTemporary = false;

        this.startTime = 0;
        this.endTime = Integer.MAX_VALUE;
    }

    /**
     * Convenience method for saving a video.
     */
    public void save(VideoRepository.VideoCallback callback) {
        // TODO: Show error messages at failing to save temporary videos
        try {
            this.repository.save(this, callback);
        } catch (Exception e) {
            if (callback != null) {
                callback.notFound();
            }
            e.printStackTrace();
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

    public boolean hasCachedFiles() {
        return this.thumbCacheUri != null && this.videoCacheUri != null;
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

    public Uri getPlaybackUri () {
        if (this.hasCachedFiles()) {
            return this.videoCacheUri;
        } else {
            return this.videoUri;
        }
    }

    private interface AnnotationComparer {
        boolean compareTimes(long first, long second);
    }


    private void purgeOutOfBoundsAnnotations(int time, AnnotationComparer comparer) {
        ArrayList<Annotation> newAnnotations = new ArrayList<Annotation>();

        for (Annotation annotation : annotations) {
            if (comparer.compareTimes(annotation.getTime(), time)) {
                newAnnotations.add(annotation);
            }
        }

        setAnnotations(newAnnotations);
    }
    public void purgeAnnotationsOlderThan(int time) {
        if (this.annotations == null || !this.isLocal()) {
            return;
        }

        purgeOutOfBoundsAnnotations(time, new AnnotationComparer() {
            @Override
            public boolean compareTimes(long first, long second) {
                return first < second;
            }
        });
    }

    public void purgeAnnotationsEarlierThan(int time) {
        if (this.annotations == null || !this.isLocal()) {
            return;
        }

        purgeOutOfBoundsAnnotations(time, new AnnotationComparer() {
            @Override
            public boolean compareTimes(long first, long second) {
                return first > second;
            }
        });
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

    public String getTag() {
        return this.tag;
    }

    public int getStartTime() { return this.startTime; }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() { return this.endTime; }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public void removeTrimming() {
        setStartTime(0);
        setEndTime(Integer.MAX_VALUE);
    }

    public boolean hasTrimming() {
        return this.startTime != 0 || this.endTime != Integer.MAX_VALUE;
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

    public boolean getIsTemporary() { return this.isTemporary; }

    public void setIsTemporary(boolean isTemporary) { this.isTemporary = isTemporary; }

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

    public void setCacheVideoUri(Uri cacheVideoUri) {
        this.videoCacheUri  = cacheVideoUri;
    }

    public void setCacheThumbUri(Uri cacheThumbUri) {
        this.thumbCacheUri  = cacheThumbUri;
    }

    public Uri getCacheVideoUri() {
        return this.videoCacheUri;
    }

    public Uri getCacheThumbUri() {
        return this.thumbCacheUri;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public void addNewAnnotation (Annotation annotation) {
        this.isLastAnnotationEmpty = true;
        this.annotations.add(annotation);
    }

    public void setIsLastAnnotationEmpty (boolean isLastAnnotationEmpty) {
        this.isLastAnnotationEmpty  = isLastAnnotationEmpty;
    }

    public boolean getIsLastAnnotationEmpty() {
        return this.isLastAnnotationEmpty;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    public void setDeleteUri(Uri deleteUri) {
        this.deleteUri = deleteUri;
    }

    public Uri getDeleteUri() {
        return deleteUri;
    }

    public static boolean isStringValidVideoID(String IDCandidate) {
        try {
            UUID test = UUID.fromString(IDCandidate);
            return true;
        } catch(IllegalArgumentException iaex) {
            return false;
        }
    }
}

