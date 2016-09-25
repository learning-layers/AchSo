package fi.aalto.legroup.achso.entities;

import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;

import com.google.common.primitives.Longs;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.storage.VideoRepository;

/**
 * Store videos in a more GC friendly way. Should be equivalent with the Video class.
 * You should use this class directly as much as possible and inflating to Video only when necessary.
 *
 * These should round trip:
 *   new OptimizedVideo(video).inflate()
 *   new OptimizedVideo(optimizedVideo.inflate())
 */
public class OptimizedVideo {

    private String manifestUri;
    private VideoRepository repository;

    private String videoUri;
    private String thumbUri;
    private String deleteUri;

    private String cacheThumbUri;
    private String cacheVideoUri;

    private int startTime;
    private int endTime;

    private UUID id;
    private String title;
    private String tag;
    private long dateInMs;
    private int revision;
    private long lastModifiedInMs;
    private int rotation;
    private double locationLatitude;
    private double locationLongitude;
    private float locationAccuracy;
    private boolean hasLocation;
    private boolean hasLastModified;
    private boolean isTemporary;

    // This stores all the annotation text data. The single annotations refer
    // to parts of this buffer with indices specified in annotationTextStartEnd.
    private String annotationTextBuffer;

    // Index to the User pool
    private int authorUserIndex;

    // Annotations
    // X and Y coordinates are stored in interleaved pairs
    // Text is stored in (begin, end) pairs to annotationTextBuffer
    // Author indices are to user pool
    private long[] annotationTime;
    private float[] annotationXY;
    private int[] annotationTextStartEnd;
    private int[] annotationAuthorUserIndex;
    private long[] annotationCreatedTimestampInMs;
    private long lastModified;
    private int formatVersion;

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Uri getVideoUri() {
        return Uri.parse(videoUri);
    }

    public Uri getThumbUri() {
        return Uri.parse(thumbUri);
    }

    public Uri getManifestUri() {
        return Uri.parse(manifestUri);
    }

    public Location getLocation() {
        if (!hasLocation) {
            return null;
        }

        Location location = new Location("optimized getter");
        location.setLatitude(locationLatitude);
        location.setLongitude(locationLongitude);
        location.setAccuracy(locationAccuracy);
        return location;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getRevision() {
        return revision;
    }

    public VideoRepository getRepository() {
        return this.repository;
    }

    public void setRepository(VideoRepository repository) {
        this.repository = repository;
    }

    public boolean hasCachedFiles() {
        return this.cacheThumbUri != null && this.cacheVideoUri != null;
    }

    public Uri getCacheThumbUri() {
        return Uri.parse(cacheThumbUri);
    }

    public Uri getCacheVideoUri() {
        return Uri.parse(cacheVideoUri);
    }

    public long getLastModified() {
        return lastModified;
    }
    /**
     * Returns number of annotations the video has, query annotation details with functions
     * taking index as a parameter
     */
    public int getAnnotationCount() {
        return annotationTime.length;
    }

    /**
     * Get the text specified for the annotation at index.
     * @param annotationIndex In range [0, getAnnotationCount()[
     * @return The text (subtitle) of the annotation
     */
    public String getAnnotationText(int annotationIndex) {

        // Annotation text data is stored in one giant buffer and is referenced with indices
        // The indices are stored in { Start0, End0, Start1, End1, ...} interleaved array
        int start = annotationTextStartEnd[annotationIndex * 2];
        int end = annotationTextStartEnd[annotationIndex * 2 + 1];

        if (start < 0 || end < 0) {

            // Mark null with negative indices
            return null;
        } else if (start == end) {

            // If the range is empty it's better not to refer to the buffer unnecessarily so
            // return an empty string
            return "";
        } else {

            // This shouldn't create a copy of the buffer, and it doesn't at the moment. If it
            // does somehow in some newer system need to tell Java to create a reference instead.
            return annotationTextBuffer.substring(start, end);
        }
    }

    public boolean isLocal() {
        Uri videoUri = getVideoUri();

        // Uris without a scheme are assumed to be local
        if (videoUri.isRelative()) {
            return true;
        }

        String scheme = videoUri.getScheme().trim().toLowerCase();

        switch (scheme) {
            case "file":
            case "content":
                return true;

            default:
                return false;
        }
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public boolean isRemote() {
        return !isLocal();
    }

    /**
     * Store the data of the video in a more GC friendly way.
     */
    public OptimizedVideo(Video video) {
        List<Annotation> annotations = video.getAnnotations();
        int annotationCount = annotations.size();

        // Store Uri objects as string (They internally are just wrapped string)
        if (video.getManifestUri() != null) {
            manifestUri = video.getManifestUri().toString();
        } else {
            manifestUri = null;
        }

        videoUri = video.getVideoUri().toString();
        thumbUri = video.getThumbUri().toString();

        if (video.getDeleteUri() != null) {
            deleteUri = video.getDeleteUri().toString();
        } else {
            deleteUri = null;
        }

        if (video.getCacheVideoUri() != null) {
            cacheVideoUri = video.getCacheVideoUri().toString();
        } else {
            cacheVideoUri = null;
        }

        if (video.getCacheThumbUri() != null) {
            cacheThumbUri = video.getCacheThumbUri().toString();
        } else {
            cacheThumbUri = null;
        }

        startTime = video.getStartTime();
        endTime = video.getEndTime();

        isTemporary = video.getIsTemporary();
        repository = video.getRepository();

        id = video.getId();
        title = video.getTitle();
        tag = video.getTag();
        rotation = video.getRotation();

        // Store Date objects as long (They internally are just wrapped long)
        dateInMs = video.getDate().getTime();
        if (video.getLastModified() != null) {
            hasLastModified = true;
            lastModifiedInMs = video.getLastModified().getTime();
        }

        revision = video.getRevision();
        formatVersion = video.getFormatVersion();

        // Intern the user and store as index so we don't have so much object references
        authorUserIndex = UserPool.internUser(video.getAuthor());

        // Store location's internal representation (or more specifically the fields we care about)
        Location location = video.getLocation();
        if (location != null) {
            hasLocation = true;
            locationLatitude = location.getLatitude();
            locationLongitude = location.getLongitude();
            locationAccuracy = location.getAccuracy();
        }

        // Allocate space for the annotations
        annotationTime = new long[annotationCount];
        annotationXY = new float[annotationCount * 2];
        annotationTextStartEnd = new int[annotationCount * 2];
        annotationAuthorUserIndex = new int[annotationCount];
        annotationCreatedTimestampInMs = new long[annotationCount];

        // This buffer will contain all the annotation text data
        // The single annotation texts are just substrings of this
        StringBuilder annotationBufferBuilder = new StringBuilder();

        for (int i = 0; i < annotationCount; i++) {
            Annotation annotation = annotations.get(i);

            annotationTime[i] = annotation.getTime();

            // The position is stored in interleaved { X0, Y0, X1, Y1, ...} array
            PointF position = annotation.getPosition();
            annotationXY[i * 2] = position.x;
            annotationXY[i * 2 + 1] = position.y;

            // The text is stored as interleaved { Start0, End0, Start1, End1, ...} index array
            // Those indices define substrings inside the annotation buffer string
            String text = annotation.getText();
            int start, end;

            if (text == null) {
                // Mark null with negative
                start = -1;
                end = -1;
            } else if (text.isEmpty()) {
                // Don't append empty strings
                start = 0;
                end = 0;
            } else {
                // Append the string to the buffer but save the index before and after it
                start = annotationBufferBuilder.length();
                annotationBufferBuilder.append(text);
                end = annotationBufferBuilder.length();
            }

            annotationTextStartEnd[i * 2] = start;
            annotationTextStartEnd[i * 2 + 1] = end;

            // Intern the user and store as index so we don't have so much object references
            annotationAuthorUserIndex[i] = UserPool.internUser(annotation.getAuthor());

            // Store Date objects as long (They internally are just wrapped long)
            annotationCreatedTimestampInMs[i] = annotation.getCreatedTimestamp().getTime();
        }

        annotationTextBuffer = annotationBufferBuilder.toString();
    }

    /**
     * Inflate the optimized video data into a heavier Video object.
     * Does not allocate if:
     *   - pooled has enough annotation capacity
     *   - pooled video has location object or this doesn't have location
     *   - every annotation has position object
     *
     * @param pooled The pooled video object to use for this, it has only one internal Video object
     *               so for every alive Video object there should be a PooledVideo. If the Video
     *               isn't needed anymore you can safely use the PooledVideo again to retrieve a
     *               new Video without having to allocate it.
     */
    public Video inflate(PooledVideo pooled) {

        int annotationCount = annotationTime.length;

        // Create the video object from the PooledVideo class, creates the list of the
        // annotations and the location object if needed.
        Video video = pooled.create(annotationCount, hasLocation);

        // Parse the Uri objects back from strings
        if (manifestUri != null) {
            video.setManifestUri(Uri.parse(manifestUri));
        } else {
            video.setManifestUri(null);
        }

        if (cacheVideoUri != null) {
            video.setCacheVideoUri(Uri.parse(cacheVideoUri));
        } else {
            video.setCacheVideoUri(null);
        }

        if (cacheThumbUri != null) {
            video.setCacheThumbUri(Uri.parse(cacheThumbUri));
        } else {
            video.setCacheThumbUri(null);
        }

        video.setVideoUri(Uri.parse(videoUri));
        video.setThumbUri(Uri.parse(thumbUri));

        if (deleteUri != null) {
            video.setDeleteUri(Uri.parse(deleteUri));
        } else {
            video.setDeleteUri(null);
        }

        video.setStartTime(startTime);

        if (endTime == 0) {
            video.setEndTime(Integer.MAX_VALUE);
        } else {
            video.setEndTime(endTime);
        }

        video.setIsTemporary(isTemporary);
        video.setRepository(repository);
        video.setId(id);
        video.setTitle(title);
        video.setTag(tag);
        video.setRotation(rotation);

        // Create the Date objects from the longs
        video.setDate(new Date(dateInMs));
        if (hasLastModified) {
            video.setLastModified(new Date(lastModifiedInMs));
        } else {
            video.setLastModified(null);
        }

        video.setRevision(revision);
        video.setFormatVersion(formatVersion);

        // Retrieve the author with the index from the user pool
        video.setAuthor(UserPool.getInternedUser(authorUserIndex));

        // Recreate the location from the location fields.
        // Note: If we are creating the video in place there might be a Location object already,
        // so we can just fill the data in there instead of creating a new Location object.
        if (hasLocation) {
            Location location = video.getLocation();
            if (location == null) {
                location = new Location("inflated");
                video.setLocation(location);
            }
            location.setLatitude(locationLatitude);
            location.setLongitude(locationLongitude);
            location.setAccuracy(locationAccuracy);
        } else {
            video.setLocation(null);
        }

        List<Annotation> annotations = video.getAnnotations();
        for (int i = 0; i < annotationCount; i++) {

            // The video comes from PooledVideo it has already allocated the list of the
            // annotations, see beginning of this function
            Annotation annotation = annotations.get(i);

            annotation.setTime(annotationTime[i]);

            // The location object should exist for the annotation already.
            PointF position = annotation.getPosition();
            if (position == null) {
                position = new PointF();
                annotation.setPosition(position);
            }
            position.set(annotationXY[i * 2], annotationXY[i * 2 + 1]);

            // Retrieve the substring from the annotation text buffer.
            annotation.setText(getAnnotationText(i));

            // Retrieve the interned user with the index.
            annotation.setAuthor(UserPool.getInternedUser(annotationAuthorUserIndex[i]));

            // Create the Date objects from the longs1
            annotation.setCreatedTimestamp(new Date(annotationCreatedTimestampInMs[i]));
        }

        return video;
    }

    /**
     * Inflate the optimized video data into a heavier Video object.
     * Note: This version allocates a new video object, you probably should use some static
     * PooledVideo and use `inflate` instead of this.
     */
    public Video inflate() {
        return inflate(new PooledVideo(annotationTime.length, hasLocation));
    }

    public static class CreateTimeComparator implements Comparator<OptimizedVideo> {

        @Override
        public int compare(OptimizedVideo lhs, OptimizedVideo rhs) {
            long lhsDateInMs = lhs.dateInMs;
            long rhsDateInMs = rhs.dateInMs;
            return Longs.compare(lhsDateInMs, rhsDateInMs);
        }
    }
}

