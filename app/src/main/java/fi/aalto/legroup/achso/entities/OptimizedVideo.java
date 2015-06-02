package fi.aalto.legroup.achso.entities;

import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
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

    private transient String manifestUri;
    private transient VideoRepository repository;

    private String videoUri;
    private String thumbUri;
    private UUID id;
    private String title;
    private String genre;
    private String tag;
    private long dateInMs;
    private long lastModifiedInMs;
    private double locationLatitude;
    private double locationLongitude;
    private float locationAccuracy;
    private boolean hasLocation;
    private boolean hasLastModified;

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

    protected static ArrayList<User> userPool = new ArrayList<>();

    /**
     * Make the user object unique.
     * @param user User object to make unique.
     * @return Index to the user in the pool.
     */
    public static int internUser(User user) {

        // Map null to -1
        if (user == null) {
            return -1;
        }

        int maxUsers = userPool.size();
        for (int i = 0; i < maxUsers; i++) {
            if (userPool.get(i).equals(user))
                return i;
        }
        userPool.add(user);
        return maxUsers;
    }

    /**
     * Retrieve the unique User object.
     * @param index User object to make unique.
     * @return The user object for the index
     */
    public static User getInternedUser(int index) {

        // Map null to -1
        if (index == -1)
            return null;

        return userPool.get(index);
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
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

    public int getAnnotationCount() {
        return annotationTime.length;
    }

    public String getAnnotationText(int i) {

        // Annotation text data is stored in one giant buffer and is referenced with indices
        int start = annotationTextStartEnd[i * 2];
        int end = annotationTextStartEnd[i * 2 + 1];

        // Mark null with negative
        if (start < 0 || end < 0) {
            return null;
        } else if (start == end) {
            return "";
        } else {
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

    public boolean isRemote() {
        return !isLocal();
    }

    /**
     * Holds a Video object and a persistent list of Annotations.
     *
     * You can request Video objects with specified amount of annotations from this and it will set
     * the annotations to be a sub-list of the persistent annotation list, so there is no need to
     * allocate annotations if it has enough storage for them. Otherwise it will allocate more
     * storage.
     */
    public static class PooledVideo {
        private Video video;
        private ArrayList<Annotation> annotations;
        private Location location;
        private boolean inUse;

        private static final int defaultAnnotationCount = 32;
        private static final boolean defaultHasLocation = true;

        private static final String TAG = PooledVideo.class.getSimpleName();

        /**
         * Create a new pooled Video instance with a default annotation capacity.
         */
        public PooledVideo() {
            this(defaultAnnotationCount, defaultHasLocation);
        }

        /**
         * Create a new pooled Video instance.
         *
         * @param annotationCountHint The number of annotations to reserve storage for in advance.
         *                            This is only a hint and if a video requires more annotations
         *                            it will allocate when inflated.
         * @param hasLocationHint Should the pooled video create a Location instance in advance.
         *                        This is only a hint and if a video requires a location it will
         *                        allocate when inflated.
         */
        public PooledVideo(int annotationCountHint, boolean hasLocationHint) {
            video = new Video();
            reserveAnnotations(annotationCountHint);
            if (hasLocationHint) {
                location = new Location("pooled");
            }
        }

        private void reserveAnnotations(int count) {

            if (annotations != null) {
                annotations.ensureCapacity(count);
            } else {
                annotations = new ArrayList<>(count);
            }

            while (annotations.size() < count) {
                Annotation annotation = new Annotation();

                // Annotations are expected to have a position ready so we don't need to create
                // new position objects all the time
                annotation.setPosition(new PointF());

                annotations.add(annotation);
            }
        }

        /**
         * How many annotations can be stored without having to allocate.
         */
        public int getAnnotationCapacity() {
            return annotations.size();
        }

        /**
         * Is the Video object of this pooled instance still in use.
         */
        public boolean isInUse() {
            return inUse;
        }

        /**
         * Create the real Video object with the specified annotation count.
         * Note: You should call free() after you are done with the video object.
         *
         * @param annotationCount How many annotations should the video contains. Does not allocate
         *                        if `annotationCount` is less or equal than
         *                        `getAnnotationCapacity()`.
         *                        Otherwise expands the annotation capacity to fit.
         * @param hasLocation Should the returned Video have it's location field initialized, if
         *                    set to true the location is initialized into the persistent location
         *                    of this pool.
         */
        public Video create(int annotationCount, boolean hasLocation) {

            if (inUse) {
                // Fallback, this shouldn't happen often
                Log.w(TAG, "Created a new Video object from pool (old wasn't released)");
                return new PooledVideo(annotationCount, hasLocation).create(annotationCount,
                        hasLocation);
            }

            reserveAnnotations(annotationCount);
            List<Annotation> subAnn = annotations.subList(0, annotationCount);
            video.setAnnotations(subAnn);
            if (hasLocation) {
                if (location == null) {
                    location = new Location("dynamic pooled");
                }
                video.setLocation(location);
            } else {
                video.setLocation(null);
            }
            return video;
        }

        /**
         * Mark that the Video can be re-used from this pool safely again.
         */
        public void free() {

            // Can be re-used safely again.
            inUse = false;
        }
    };

    /**
     * Store the data of the video in a more GC friendly way.
     */
    public OptimizedVideo(Video video) {

        List<Annotation> annotations = video.getAnnotations();
        int annotationCount = annotations.size();

        manifestUri = video.getManifestUri().toString();
        repository = video.getRepository();

        videoUri = video.getVideoUri().toString();
        thumbUri = video.getThumbUri().toString();
        id = video.getId();
        title = video.getTitle();

        genre = video.getGenre();
        if (genre != null) {
            // Intern genres because there are only a few options for the field
            genre = genre.intern();
        }

        tag = video.getTag();
        dateInMs = video.getDate().getTime();
        if (video.getLastModified() != null) {
            hasLastModified = true;
            lastModifiedInMs = video.getLastModified().getTime();
        }
        authorUserIndex = internUser(video.getAuthor());

        Location location = video.getLocation();

        if (location != null) {
            hasLocation = true;
            locationLatitude = location.getLatitude();
            locationLongitude = location.getLongitude();
            locationAccuracy = location.getAccuracy();
        }

        annotationTime = new long[annotationCount];
        annotationXY = new float[annotationCount * 2];
        annotationTextStartEnd = new int[annotationCount * 2];
        annotationAuthorUserIndex = new int[annotationCount];

        StringBuilder annotationBufferBuilder = new StringBuilder();

        for (int i = 0; i < annotationCount; i++) {
            Annotation annotation = annotations.get(i);

            annotationTime[i] = annotation.getTime();

            PointF position = annotation.getPosition();
            annotationXY[i * 2] = position.x;
            annotationXY[i * 2 + 1] = position.y;

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
                start = annotationBufferBuilder.length();
                annotationBufferBuilder.append(text);
                end = annotationBufferBuilder.length();
            }

            annotationTextStartEnd[i * 2] = start;
            annotationTextStartEnd[i * 2 + 1] = end;
            annotationAuthorUserIndex[i] = internUser(annotation.getAuthor());
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
        Video video = pooled.create(annotationCount, hasLocation);

        video.setManifestUri(Uri.parse(manifestUri));
        video.setRepository(repository);

        video.setVideoUri(Uri.parse(videoUri));
        video.setThumbUri(Uri.parse(thumbUri));
        video.setId(id);
        video.setTitle(title);
        video.setGenre(genre);
        video.setTag(tag);
        video.setDate(new Date(dateInMs));
        if (hasLastModified) {
            video.setLastModified(new Date(lastModifiedInMs));
        } else {
            video.setLastModified(null);
        }
        video.setAuthor(getInternedUser(authorUserIndex));

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
            Annotation annotation = annotations.get(i);

            annotation.setTime(annotationTime[i]);

            PointF position = annotation.getPosition();
            if (position == null) {
                position = new PointF();
                annotation.setPosition(position);
            }
            position.set(annotationXY[i * 2], annotationXY[i * 2 + 1]);

            annotation.setText(getAnnotationText(i));
            annotation.setAuthor(getInternedUser(annotationAuthorUserIndex[i]));
        }

        return video;
    }

    /**
     * Inflate the optimized video data into a heavier Video object.
     * Note: This version allocates a new video object, you probably should use some static
     * PooledVideo and use `inflate` instead of this.
     */
    public Video inflateNew() {
        return inflate(new PooledVideo(annotationTime.length, hasLocation));
    }

    public boolean save() {
        try {
            this.repository.save(this);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}

