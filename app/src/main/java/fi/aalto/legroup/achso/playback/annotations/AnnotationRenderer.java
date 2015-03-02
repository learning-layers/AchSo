package fi.aalto.legroup.achso.playback.annotations;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.TrackRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import fi.aalto.legroup.achso.entities.Annotation;

import static fi.aalto.legroup.achso.app.AppPreferences.ANNOTATION_PAUSE_DURATION;

/**
 * Renderer for annotation "tracks" that handles all playback-related functionality, e.g. pausing,
 * and delegates all view-related tasks to special strategy classes. Runs on ExoPlayer's playback
 * thread, but Strategy implementations are run on the main thread so that they can access the UI.
 *
 * @author Leo Nikkilä
 */
public final class AnnotationRenderer extends TrackRenderer implements Runnable {

    /**
     * How long a pause should last for each annotation.
     */
    private static final int PAUSE_PER_ANNOTATION_MILLISECONDS_BASE = 2000;

    /**
     * How long to pause per character in annotation
     */
    private static final int PAUSE_PER_CHARACTER_MILLISECONDS = 50;

    /**
     * List of annotations. Access to this field needs to be synchronised, as it is modified across
     * multiple threads.
     */
    private final List<Annotation> annotations = new ArrayList<>();

    /**
     * List of rendered annotations. Recycled to avoid creating new lists with each render. This
     * should only be used in #renderAnnotations().
     */
    private final List<Annotation> renderList = new ArrayList<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler pauseHandler = new Handler();

    private final List<Strategy> strategies;

    /**
     * Whether playback is paused for an annotation.
     */
    private boolean isPaused = false;

    /**
     * Previous continuous playback position in microseconds.
     */
    private long previousPosition = 0;

    /**
     * Length for the pause per annotation that will be populated from settings
     */
    private int annotationPauseLength;

    @Nullable
    private EventListener listener;

    public AnnotationRenderer(Context context, @Nullable EventListener listener,
                              Strategy... strategies) {
        this.listener = listener;
        this.strategies = Arrays.asList(strategies);
        this.annotationPauseLength = readAnnotationLengthSetting(context);
    }

    public AnnotationRenderer(Context context, Strategy... strategies) {
        this(context, null, strategies);
    }

    /**
     * Sets the listener that should be notified of events related to this renderer. Pass in null
     * to remove the current listener.
     */
    public void setListener(@Nullable EventListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the annotations that will be rendered. This implementation is thread-safe.
     */
    public void setAnnotations(List<Annotation> annotations) {
        synchronized (this.annotations) {
            this.annotations.clear();
            this.annotations.addAll(annotations);
        }

        // Re-render annotations due to the change
        clearAnnotations();
        renderAnnotations(previousPosition, false);
    }

    /**
     * Renders annotations at the given time.
     *
     * @param position     Position to render annotations for.
     * @param isContinuous Whether playback is continuous (playing) or discrete (paused, seeking).
     */
    private void renderAnnotations(long position, boolean isContinuous) {
        renderList.clear();

        synchronized (annotations) {
            for (Annotation annotation : annotations) {
                if (shouldRender(annotation, position, isContinuous)) {
                    renderList.add(annotation);
                }
            }
        }

        previousPosition = position;

        // If renderList was passed to the main thread, by the time it was executed, it might have
        // been already cleared. Shallow copying it here avoids concurrency issues without creating
        // new lists when there are no annotations to render.
        final List<Annotation> internalRenderList = new ArrayList<>(renderList);

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (Strategy strategy : strategies) {
                    strategy.render(internalRenderList);
                }
            }
        });
    }

    /**
     * Returns whether the annotation should be rendered given the conditions.
     *
     * @param annotation   Annotation that could be rendered.
     * @param position     Current playback position in microseconds.
     * @param isContinuous Whether playback is continuous (playing) or discrete (paused, seeking).
     */
    private boolean shouldRender(Annotation annotation, long position, boolean isContinuous) {
        long annotationTime = annotation.getTime();

        // Annotation times are stored as milliseconds: rounding is necessary. This results in a
        // window of 1 ms even with discrete playback. For correct behaviour, it's important to
        // round the right timestamp. With continuous playback it should be the annotation, but
        // with discrete it should be the playback position.
        if (isContinuous) {
            return annotationTime * 1000 > previousPosition && annotationTime * 1000 <= position;
        } else {
            return position / 1000 == annotationTime;
        }
    }

    /**
     * Clears rendered annotations.
     */
    private void clearAnnotations() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (Strategy strategy : strategies) {
                    strategy.clear();
                }
            }
        });
    }

    /**
     * Pauses for the given duration.
     */
    private void startPause(final int duration) {
        isPaused = true;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onAnnotationPauseStart(duration);
                }
            }
        });

        pauseHandler.postDelayed(this, duration);
    }

    /**
     * Stops a pause if in effect.
     */
    private void stopPause() {
        if (isPaused) {
            isPaused = false;

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onAnnotationPauseEnd();
                    }
                }
            });
        }

        pauseHandler.removeCallbacks(this);
    }

    /**
     * Called when a pause times out.
     */
    @Override
    public void run() {
        stopPause();
    }

    /**
     * Invoked to make progress when the renderer is in the STATE_UNPREPARED state.
     */
    @Override
    protected int doPrepare() {
        // Prepared instantly
        return STATE_PREPARED;
    }

    /**
     * Invoked to make progress when the renderer is in the STATE_ENABLED or STATE_STARTED states.
     */
    @Override
    protected void doSomeWork(long position, long elapsedRealTime) throws ExoPlaybackException {
        if (position == previousPosition) {
            return;
        }

        renderAnnotations(position, true);

        if (renderList.isEmpty()) {
            return;
        }

        int pauseDuration = 0;

        // Pause for a fixed duration per annotation + a duration dependent on the content length.
        for (Annotation annotation : renderList) {
            pauseDuration += this.annotationPauseLength;
            pauseDuration += annotation.getText().length() * PAUSE_PER_CHARACTER_MILLISECONDS;
        }

        startPause(pauseDuration);
    }

    /**
     * Seeks to a specified time in the track.
     */
    @Override
    protected void seekTo(long position) throws ExoPlaybackException {
        if (position == previousPosition) {
            return;
        }

        // If seeking in the middle of a pause
        stopPause();
        clearAnnotations();

        renderAnnotations(position, false);
    }

    /**
     * Called when the renderer is started.
     */
    @Override
    protected void onStarted() throws ExoPlaybackException {
        super.onStarted();

        // Clear any annotations that might be left on screen when transitioning from discrete to
        // continuous playback.
        clearAnnotations();
    }

    /**
     * Called when the renderer is released.
     */
    @Override
    protected void onReleased() throws ExoPlaybackException {
        stopPause();

        synchronized (annotations) {
            annotations.clear();
        }

        listener = null;

        super.onReleased();
    }

    /**
     * Returns the current playback position.
     */
    @Override
    protected long getCurrentPositionUs() {
        return previousPosition;
    }

    /**
     * Whether the renderer is ready for the ExoPlayer instance to transition to STATE_ENDED.
     */
    @Override
    protected boolean isEnded() {
        // Ready if there is no ongoing annotation pause. When playing, this will be checked before
        // checking isReady(). Both of these must return false for a pause.
        return !isPaused;
    }

    /**
     * Whether the renderer is able to immediately render media from the current position.
     */
    @Override
    protected boolean isReady() {
        // Ready if there is no ongoing annotation pause. Returning false forces ExoPlayer to pause
        // until this or isEnded() returns true again.
        return !isPaused;
    }

    /**
     * Returns the duration of the media being rendered.
     */
    @Override
    protected long getDurationUs() {
        // Equal to the longest track
        return MATCH_LONGEST_US;
    }

    /**
     * Returns an estimate of the absolute position in microseconds up to which data is buffered.
     */
    @Override
    protected long getBufferedPositionUs() {
        // Buffered instantly
        return END_OF_TRACK_US;
    }

    /**
     * Reads the base duration for an annotation pause from the shared preferences.
     */
    private int readAnnotationLengthSetting(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String durationDefault = Integer.toString(PAUSE_PER_ANNOTATION_MILLISECONDS_BASE);
        String durationString = preferences.getString(ANNOTATION_PAUSE_DURATION, durationDefault);

        return Integer.parseInt(durationString);
    }

    /**
     * Strategy for specifying different actions for annotations.
     */
    public static interface Strategy {

        /**
         * Render the specified annotations. This is run on the main thread.
         */
        public void render(final List<Annotation> annotations);

        /**
         * Clear the previous render. This is run on the main thread.
         */
        public void clear();

    }

    /**
     * Listener for events related to this renderer.
     */
    public static interface EventListener {

        /**
         * Invoked when an annotation pause starts. This is run on the main thread.
         *
         * @param duration Pause duration in milliseconds.
         */
        public void onAnnotationPauseStart(int duration);

        /**
         * Invoked when an annotation pause ends. This is run on the main thread.
         */
        public void onAnnotationPauseEnd();

    }

}
