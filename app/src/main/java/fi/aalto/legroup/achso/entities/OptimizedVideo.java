package fi.aalto.legroup.achso.entities;

import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.storage.VideoRepository;

/**
 * Store videos in a more GC friendly way. Should be equivaent with the Video class.
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
    private User author;
    private double locationLatitude;
    private double locationLongitude;
    private float locationAccuracy;

    // Annotations
    // X and Y coordinates are stored in interleaved pairs
    private long[] annotationTime;
    private float[] annotationXY;
    private String[] annotationText;
    private User[] annotationAuthor;

    protected static ArrayList<User> userPool = new ArrayList<>();

    /**
     * Make the user object unique.
     * @param user User object to make unique.
     * @return User object equal to `user` but which is shared between equal ones.
     */
    public static User internUser(User user) {

        // TODO: This could be changed to a set?
        for (User u : userPool) {
            if (u.equals(user))
                return u;
        }
        userPool.add(user);
        return user;
    }

    /**
     * Make the user object unique.
     * @param name Forwarded to User constructor.
     * @param uri Forwarded to User constructor.
     * @return User object equal to `new User(name, uri)` but which is shared between equal ones.
     */
    public static User internNewUser(String name, Uri uri) {

        // TODO: This could be changed to a set?
        for (User u : userPool) {
            if (Objects.equal(u.getName(), name) && Objects.equal(u.getUri(), uri))
                return u;
        }
        User user = new User(name, uri);
        userPool.add(user);
        return user;
    }

    /**
     * Holds a Video object and a persistent list of Annotations.
     *
     * You can request Video objects with specified amount of annotations from this and it will set
     * the annotations to be a sub-list of the persistent annotation list, so there is no need to
     * allocate annotations if it has enough storage for them. Otherwise it will allocate more
     * storage.
     */
    public class PooledVideo {
        private Video video;
        private ArrayList<Annotation> annotations;

        /**
         * Create a new pooled Video instance.
         *
         * @param annotationCountHint The number of annotations to reserve storage for in advance.
         *                            This is only a hint and if a video requires more annotations
         *                            it will allocate when inflated.
         */
        public PooledVideo(int annotationCountHint) {
            video = new Video();
            reserveAnnotations(annotationCountHint);
        }

        private void reserveAnnotations(int count) {

            annotations.ensureCapacity(count);
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
         * Create the real Video object with the specified annotation count.
         *
         * @param annotationCount How many annotations should the video contains. Does not allocate
         *                        if `annotationCount` is less or equal than
         *                        `getAnnotationCapacity()`.
         *                        Otherwise expands the annotation capacity to fit.
         */
        public Video create(int annotationCount) {

            reserveAnnotations(annotationCount);
            List<Annotation> subAnn = annotations.subList(0, annotationCount);
            video.setAnnotations(subAnn);
            return video;
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
        tag = video.getTag();
        dateInMs = video.getDate().getTime();
        author = internUser(video.getAuthor());

        Location location = video.getLocation();
        locationLatitude = location.getLatitude();
        locationLongitude = location.getLongitude();
        locationAccuracy = location.getAccuracy();

        annotationTime = new long[annotationCount];
        annotationXY = new float[annotationCount * 2];
        annotationText = new String[annotationCount];
        annotationAuthor = new User[annotationCount];

        for (int i = 0; i < annotationCount; i++) {
            Annotation annotation = annotations.get(i);

            annotationTime[i] = annotation.getTime();

            PointF position = annotation.getPosition();
            annotationXY[i * 2] = position.x;
            annotationXY[i * 2 + 1] = position.y;
            annotationText[i] = annotation.getText();
            annotationAuthor[i] = internUser(annotation.getAuthor());
        }
    }

    /**
     * Inflate the optimized video data into a heavier Video object.
     *
     * @param pooled The pooled video object to use for this, it has only one internal Video object
     *               so for every alive Video object there should be a PooledVideo. If the Video
     *               isn't needed anymore you can safely use the PooledVideo again to retrieve a
     *               new
     *               Video without having to allocate it.
     */
    public Video inflate(PooledVideo pooled) {

        int annotationCount = annotationTime.length;
        Video video = pooled.create(annotationCount);

        video.setManifestUri(Uri.parse(manifestUri));
        video.setRepository(repository);

        video.setVideoUri(Uri.parse(videoUri));
        video.setThumbUri(Uri.parse(thumbUri));
        video.setId(id);
        video.setTitle(title);
        video.setGenre(genre);
        video.setTag(tag);
        video.setDate(new Date(dateInMs));
        video.setAuthor(author);

        Location location = video.getLocation();
        if (location == null) {
            location = new Location("cached");
            video.setLocation(location);
        }
        location.setLatitude(locationLatitude);
        location.setLongitude(locationLongitude);
        location.setAccuracy(locationAccuracy);

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

            annotation.setText(annotationText[i]);
            annotation.setAuthor(annotationAuthor[i]);
        }

        return video;
    }

    /**
     * Inflate the optimized video data into a heavier Video object.
     * Note: This version allocates a new video object, you probably should use some static
     * PooledVideo and use `inflate` instead of this.
     */
    public Video inflateNew() {
        return inflate(new PooledVideo(annotationTime.length));
    }
}

