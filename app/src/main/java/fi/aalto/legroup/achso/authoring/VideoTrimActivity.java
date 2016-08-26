package fi.aalto.legroup.achso.authoring;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.PointF;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.playback.AnnotationEditor;
import fi.aalto.legroup.achso.playback.PlayerFragment;
import fi.aalto.legroup.achso.utilities.RepeatingTask;

public class VideoTrimActivity extends ActionBarActivity implements PlayerFragment.PlaybackStateListener,
        SeekBar.OnSeekBarChangeListener, AnnotationEditor, View.OnClickListener {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";

    private Video video;
    private UUID id;

    private int startTrimTime = 0;
    private int endTrimTime = 0;
    private int videoLength = 0;

    private PlayerFragment playerFragment;

    private SeekBar seekBar;
    private RangeSeekBar<Integer> rangeSeekBar;

    private ImageButton playPauseButton;

    private SeekBarUpdater seekBarUpdater = new SeekBarUpdater();

    private int percentageToVideoLength(float percentage) {
        return Math.round(videoLength * percentage);
    }

    private int lengthToPercentage(int length) {
        return Math.round(100 * (length / (float) videoLength));
    }

    private void loadVideo(UUID videoId) {
        Video video;
        try {
            video = App.videoRepository.getVideo(videoId).inflate();
        } catch (IOException e) {
            SnackbarManager.show(Snackbar.with(this).text("Error loading video!"));
            return;
        }

        this.video = video;
        playerFragment = (PlayerFragment)
                getFragmentManager().findFragmentById(R.id.videoPlayerFragment);

        playerFragment.setListener(this);
        playerFragment.prepare(video, this);
    }

    private void setRangeBarValues(Video video) {
        int start = video.getStartTime();
        int end = video.getEndTime();

        int minValue = lengthToPercentage(start);
        rangeSeekBar.setSelectedMinValue(minValue);

        if (end != Integer.MAX_VALUE) {
            int maxValue = lengthToPercentage(end);
            rangeSeekBar.setSelectedMaxValue(maxValue);
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        loadVideo(this.id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_trim);

        Intent intent = getIntent();

        playPauseButton = (ImageButton) findViewById(R.id.playPauseButton);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        rangeSeekBar = (RangeSeekBar<Integer>) findViewById(R.id.trimBar);
        playPauseButton.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        this.id = UUID.fromString(intent.getStringExtra(ARG_VIDEO_ID));


        rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                int newStartTime = percentageToVideoLength(minValue / 100.0f);
                int newEndTime = percentageToVideoLength(maxValue / 100.0f);

                setVideoStart(newStartTime);
                setVideoEnd(newEndTime);
            }
        });
    }

    private void setVideoStart(int newStart) {
        this.startTrimTime = newStart;
        long currentPlaytime = playerFragment.getPlaybackPosition();

        if (currentPlaytime < newStart) {
            playerFragment.pause();
            playerFragment.seekTo(startTrimTime);
            seekBar.setProgress(startTrimTime);
        }

    }
    private void setVideoEnd(int newEnd) {
        if (newEnd == Integer.MAX_VALUE) return;

        this.endTrimTime = newEnd;
        long currentPlaytime = playerFragment.getPlaybackPosition();

        if (currentPlaytime > newEnd) {
            playerFragment.pause();
            playerFragment.seekTo(endTrimTime);
            seekBar.setProgress(endTrimTime);
        }
    }

    @Override
    public void createAnnotation(PointF position) {
        // No-op
    }

    @Override
    public void editAnnotation(Annotation annotation) {
        // No-op
    }

    @Override
    public void moveAnnotation(Annotation annotation, PointF position) {
        // No-op
    }

    @Override
    public void onBackPressed() {
        video.setStartTime(startTrimTime);
        video.setEndTime(endTrimTime);
        video.purgeAnnotationsEarlierThan(startTrimTime);
        video.purgeAnnotationsOlderThan(endTrimTime);
        video.save(null);

        super.onBackPressed();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && progress > startTrimTime && progress < endTrimTime) {
            playerFragment.seekTo(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        seekBarUpdater.stop();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        seekBarUpdater.run();
    }

    private final class SeekBarUpdater extends RepeatingTask {

        // How often the seek bar should be updated (in milliseconds)
        private static final int UPDATING_FREQUENCY = 250;

        public SeekBarUpdater() {
            super(UPDATING_FREQUENCY);
        }

        @Override
        protected void doWork() {
            int progress = (int) playerFragment.getPlaybackPosition();
            animateTo(progress);
        }

        private void animateTo(int progress) {

            if (progress > endTrimTime) {
                progress = endTrimTime;
                playerFragment.pause();
            }

            int oldProgress = seekBar.getProgress();

            // Only animate if playback is progressing forwards, otherwise it's confusing
            if (oldProgress < progress) {
                ObjectAnimator animator =
                        ObjectAnimator.ofInt(seekBar, "progress", oldProgress, progress);

                animator.setDuration(UPDATING_FREQUENCY);
                animator.setInterpolator(new LinearInterpolator());

                animator.start();
            } else {
                seekBar.setProgress(progress);
            }
        }

    }
    @Override
    public void onPlaybackStateChanged(PlayerFragment.State state) {
        switch (state) {
            case PREPARED:
                // Initialise the seek bar now that we have a duration and a position
                videoLength = (int) playerFragment.getDuration();
                endTrimTime = videoLength;

                setRangeBarValues(this.video);
                setVideoStart(video.getStartTime());
                setVideoEnd(video.getEndTime());

                seekBar.setMax(videoLength);
                seekBar.setProgress((int) playerFragment.getPlaybackPosition());
                seekBarUpdater.run();
                break;

            case PLAYING:
                playPauseButton.setImageResource(R.drawable.ic_action_pause);
                break;

            case FINISHED:
                playerFragment.seekTo(startTrimTime);
                break;

            case PAUSED:
                playPauseButton.setImageResource(R.drawable.ic_action_play);
                break;
        }
    }

    @Override
    protected void onPause() {
        seekBarUpdater.stop();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.playPauseButton:
                togglePlayback();
                break;
        }
    }

    public void togglePlayback() {
        if (playerFragment.getPlaybackPosition() > endTrimTime) {
            return;
        }  else if (playerFragment.getState() == PlayerFragment.State.PLAYING) {
            playerFragment.pause();
        } else {
            playerFragment.play();
        }
    }
}
