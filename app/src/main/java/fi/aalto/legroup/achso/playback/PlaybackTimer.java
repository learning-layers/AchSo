package fi.aalto.legroup.achso.playback;

import android.media.MediaPlayer;
import android.os.Handler;

/**
 * Uses heuristics to predict MediaPlayer playback position more accurately. Won't be needed with
 * e.g. ExoPlayer.
 */
public final class PlaybackTimer {

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

    public PlaybackTimer(MediaPlayer player) {
        this.player = player;

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isRunning && listener != null) {
                    long position = calculateCorrectedPosition();
                    listener.onPlaybackTimerTick(position);
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

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        isRunning = true;
        startTime = System.nanoTime();
        startPos = player.getCurrentPosition();
    }

    public void stop() {
        isRunning = false;
    }

    public void release() {
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

        public void onPlaybackTimerTick(long playbackPosition);

    }

}
