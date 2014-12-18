package fi.aalto.legroup.achso.playback.strategies;

import android.os.Handler;

import java.util.Collection;

import fi.aalto.legroup.achso.entities.Annotation;

/**
 * Pauses for annotations. The duration is calculated from the number of annotations shown at once
 * and the length of their texts.
 *
 * @author Leo Nikkilä
 */
public class PauseStrategy extends AnnotationStrategy implements Runnable {

    public static final int DURATION_PER_CHAR = 100;
    public static final int MINIMUM_DURATION = 2000;

    private PauseListener listener;

    private Handler handler = new Handler();
    private boolean inProgress = false;

    public PauseStrategy(PauseListener listener) {
        this.listener = listener;
    }

    @Override
    public void execute(Collection<Annotation> annotations) {
        int totalDuration = 0;

        for (Annotation annotation : annotations) {
            int duration = annotation.getText().length() * DURATION_PER_CHAR;

            if (duration < MINIMUM_DURATION) {
                duration = MINIMUM_DURATION;
            }

            totalDuration += duration;
        }

        inProgress = true;
        listener.startAnnotationPause(totalDuration);
        handler.postDelayed(this, totalDuration);
    }

    /**
     * Stops the pause if one is going on.
     */
    public void stop() {
        if (inProgress) {
            inProgress = false;
            handler.removeCallbacks(this);
            listener.stopAnnotationPause();
        }
    }

    @Override
    public void release() {
        handler.removeCallbacks(this);
    }

    /**
     * Called by the handler when the pause ends.
     */
    @Override
    public void run() {
        stop();
    }

    public static interface PauseListener {

        public void startAnnotationPause(int duration);

        public void stopAnnotationPause();

    }

}
