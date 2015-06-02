package fi.aalto.legroup.achso.entities;

import android.graphics.PointF;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
     * @param hasLocationHint     Should the pooled video create a Location instance in advance.
     *                            This is only a hint and if a video requires a location it will
     *                            allocate when inflated.
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

        // Pre-create annotation objects for the
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
     * @param hasLocation     Should the returned Video have it's location field initialized, if
     *                        set to true the location is initialized into the persistent location
     *                        of this pool.
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
}
