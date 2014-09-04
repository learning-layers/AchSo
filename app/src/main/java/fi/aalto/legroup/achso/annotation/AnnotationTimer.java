package fi.aalto.legroup.achso.annotation;

import android.media.MediaPlayer;
import android.os.Handler;

public class AnnotationTimer {

    private Handler handler = new Handler();
    private Listener listener;
    private MediaPlayer player;

    private boolean isAlive = true;
    private boolean isRunning = false;

    // Some arbitrary but accurate system time in nanoseconds
    private long startTime;

    // The MediaPlayer's playback time in milliseconds
    private long startPos;

    // Drift adjustment in milliseconds
    private long driftAdjustment = 0;

    // How often we should check for annotations
    private static final int TICK_INTERVAL = 50;

    // How often we should adjust the predicted playback time for drifting
    private static final int DRIFT_ADJUSTMENT_INTERVAL = 2500;

    public AnnotationTimer(Listener listener, MediaPlayer player) {
        this.listener = listener;
        this.player = player;

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    long position = calculateCorrectedPosition();
                    AnnotationTimer.this.listener.onAnnotationTimerTick(position);
                }

                if (isAlive) handler.postDelayed(this, TICK_INTERVAL);
            }
        });

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isRunning) adjustForDrift();
                if (isAlive) handler.postDelayed(this, DRIFT_ADJUSTMENT_INTERVAL);
            }
        });
    }

    public void start() {
        isRunning = true;
        startTime = System.nanoTime();
        startPos = player.getCurrentPosition();
    }

    public void stop() {
        isRunning = false;
    }

    public void destroy() {
        isAlive = false;
        isRunning = false;
        listener = null;
        player = null;
    }

    private void adjustForDrift() {
        driftAdjustment = player.getCurrentPosition() - calculatePredictedPosition();
    }

    private long calculatePredictedPosition() {
        return startPos + (System.nanoTime() - startTime) / (long) 1e6;
    }

    private long calculateCorrectedPosition() {
        return calculatePredictedPosition() + driftAdjustment;
    }

    public interface Listener {
        public void onAnnotationTimerTick(long playbackPosition);
    }

}
