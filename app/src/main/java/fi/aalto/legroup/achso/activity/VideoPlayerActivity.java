package fi.aalto.legroup.achso.activity;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.annotation.AnnotationEditor;
import fi.aalto.legroup.achso.annotation.AnnotationFactory;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.fragment.VideoPlayerFragment;
import fi.aalto.legroup.achso.remote.RemoteResultCache;
import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.util.RepeatingTask;
import fi.aalto.legroup.achso.view.MarkedSeekBar;

/**
 * Handles view logic for the video player controls. Actual playback is handled by
 * VideoPlayerFragment.
 *
 * TODO: Extract annotation editing into a separate Fragment.
 */
public final class VideoPlayerActivity extends FragmentActivity implements AnnotationEditor,
        VideoPlayerFragment.PlaybackStateListener, SeekBar.OnSeekBarChangeListener {

    public static final String ARG_ITEM_ID = "ARG_ITEM_ID";
    public static final String ARG_ITEM_CACHE_POSITION = "ARG_ITEM_CACHE_POSITION";

    // How long the user can be inactive before the controls get hidden (in milliseconds)
    private static final int CONTROLS_HIDE_DELAY = 5000;

    // Animation duration for hiding and showing controls (in milliseconds)
    private static final int CONTROLS_ANIMATION_DURATION = 300;

    // How far should the forwards/backwards buttons skip (in milliseconds)
    private static final int SKIP_AMOUNT = 10000;

    private ActionBar actionBar;

    private VideoPlayerFragment playerFragment;

    private RelativeLayout controlsOverlay;
    private LinearLayout playbackControls;
    private LinearLayout annotationControls;

    private ImageButton playPauseButton;
    private TextView elapsedTimeText;
    private TextView totalTimeText;
    private MarkedSeekBar seekBar;

    private Button deleteButton;
    private Button saveButton;
    private EditText annotationText;

    private SemanticVideo video;

    private AnnotationFactory annotationFactory;

    private VideoDBHelper dbHelper;

    private Handler controllerVisibilityHandler = new Handler();
    private SeekBarUpdater seekBarUpdater = new SeekBarUpdater();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        actionBar = getActionBar();

        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        dbHelper = new VideoDBHelper(this);
        annotationFactory = new AnnotationFactory();

        controlsOverlay = (RelativeLayout) findViewById(R.id.controlsOverlay);
        playbackControls = (LinearLayout) findViewById(R.id.playbackControls);
        playPauseButton = (ImageButton) findViewById(R.id.playPauseButton);
        elapsedTimeText = (TextView) findViewById(R.id.elapsedTimeText);
        totalTimeText = (TextView) findViewById(R.id.totalTimeText);
        seekBar = (MarkedSeekBar) findViewById(R.id.seekBar);

        seekBar.setOnSeekBarChangeListener(this);

        Long videoId = getIntent().getLongExtra(ARG_ITEM_ID, -1);

        if (videoId == -1) {
            video = RemoteResultCache.getSelectedVideo();
        } else {
            video = VideoDBHelper.getById(videoId);
        }

        playerFragment = (VideoPlayerFragment)
                getFragmentManager().findFragmentById(R.id.videoPlayerFragment);

        populateVideoInformation();
    }

    private void refreshRenderedAnnotations() {
        List<Annotation> annotations;

        if (video.inLocalDB()) {
            annotations = dbHelper.getAnnotationsById(video.getId());
        } else {
            annotations = video.getAnnotations(this);
        }

        playerFragment.setAnnotations(annotations);

        List<Integer> markers = new ArrayList<Integer>();

        for (Annotation annotation : annotations) {
            markers.add((int) annotation.getStartTime());
        }

        seekBar.setMarkers(markers);
    }

    private void populateVideoInformation() {
        TextView title = (TextView) findViewById(R.id.videoTitle);
        TextView genre = (TextView) findViewById(R.id.videoGenre);

        title.setText(video.getTitle());
        genre.setText(video.getGenreText());

        switch (video.getGenre()) {
            case Problem:
                genre.setTextColor(getResources().getColor(R.color.achso_red));
                break;

            case GoodWork:
                genre.setTextColor(getResources().getColor(R.color.achso_green));
                break;

            case TrickOfTrade:
                genre.setTextColor(getResources().getColor(R.color.achso_yellow));
                break;

            case SiteOverview:
                genre.setTextColor(getResources().getColor(R.color.achso_blue));
                break;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        try {
            playerFragment.setVideo(video);
            playerFragment.setListener(this);
            playerFragment.setAnnotationEditor(this);

            playerFragment.prepare();
        } catch (IOException e) {
            // TODO: Show error message
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        seekBarUpdater.stop();
        controllerVisibilityHandler.removeCallbacksAndMessages(null);

        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void togglePlayback(View view) {
        if (playerFragment.getState() == VideoPlayerFragment.State.PLAYING) {
            playerFragment.pause();
        } else {
            playerFragment.play();
        }
    }

    public void skipForward(View view) {
        long position = playerFragment.getPlaybackPosition() + SKIP_AMOUNT;
        long end = playerFragment.getDuration();

        if (position > end) position = end;

        playerFragment.seekTo(position);
        seekBarUpdater.doWork();
    }

    public void skipBackward(View view) {
        long position = playerFragment.getPlaybackPosition() - SKIP_AMOUNT;

        if (position < 0) position = 0;

        playerFragment.seekTo(position);
        seekBarUpdater.doWork();
    }

    private void anchorSubtitleContainerTo(View view) {
        int height = 0;

        if (view != null) height = view.getHeight();

        playerFragment.getSubtitleContainer().setPadding(0, 0, 0, height);
    }

    /**
     * Show the controls overlay.
     */
    private void showControlsOverlay() {
        cancelControlsOverlayHide();

        controlsOverlay.animate().alpha(1).setDuration(CONTROLS_ANIMATION_DURATION).start();

        anchorSubtitleContainerTo(playbackControls);

        if (actionBar != null) actionBar.show();
    }

    /**
     * Hide controls using a delay.
     */
    private void hideControlsOverlay() {
        controllerVisibilityHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Don't hide if we're paused
                if (playerFragment.getState() == VideoPlayerFragment.State.PAUSED) return;

                controlsOverlay.animate().alpha(0).setDuration(CONTROLS_ANIMATION_DURATION).start();

                anchorSubtitleContainerTo(null);

                if (actionBar != null) actionBar.hide();
            }
        }, CONTROLS_HIDE_DELAY);
    }

    /**
     * Cancel pending hiding operations.
     */
    private void cancelControlsOverlayHide() {
        controllerVisibilityHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void createAnnotation(FloatPosition position) {
        // Allow creating annotations only when paused
        if (playerFragment.getState() != VideoPlayerFragment.State.PAUSED) return;

        long time = playerFragment.getPlaybackPosition();
        Annotation annotation = annotationFactory.create(video, time, position);

        dbHelper.insert(annotation);
        dbHelper.close();

        editAnnotation(annotation);

        refreshRenderedAnnotations();
    }

    @Override
    public void moveAnnotation(Annotation annotation, FloatPosition position) {
        annotation.setPosition(position);

        dbHelper.update(annotation);
        dbHelper.close();
    }

    @Override
    public void editAnnotation(final Annotation annotation) {
        showAnnotationControls();

        annotationText.setText(annotation.getText());

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = annotationText.getText().toString();

                annotation.setText(text);

                dbHelper.update(annotation);
                dbHelper.close();

                refreshRenderedAnnotations();

                hideAnnotationControls();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbHelper.delete(annotation);
                dbHelper.close();

                refreshRenderedAnnotations();

                hideAnnotationControls();
            }
        });
    }

    private void showAnnotationControls() {
        // Inflate the view stub if necessary
        if (annotationControls == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.annotationControls);
            annotationControls = (LinearLayout) stub.inflate();

            annotationText = (EditText) findViewById(R.id.annotationText);
            saveButton = (Button) findViewById(R.id.saveButton);
            deleteButton = (Button) findViewById(R.id.deleteButton);
        }

        playbackControls.setVisibility(View.GONE);
        annotationControls.setVisibility(View.VISIBLE);

        anchorSubtitleContainerTo(annotationControls);
    }

    private void hideAnnotationControls() {
        playbackControls.setVisibility(View.VISIBLE);
        annotationControls.setVisibility(View.GONE);

        anchorSubtitleContainerTo(playbackControls);
    }

    public void launchInformationActivity(View view) {
        Intent informationIntent = new Intent(this, InformationActivity.class);
        informationIntent.putExtra(ARG_ITEM_ID, video.getId());
        startActivity(informationIntent);
    }

    /**
     * Fired when the player fragment changes state.
     */
    @Override
    public void onPlaybackStateChanged(VideoPlayerFragment.State state) {
        switch (state) {
            case PREPARED:
                // Initialise the seek bar now that we have a duration and a position
                seekBar.setMax((int) playerFragment.getDuration());
                seekBar.setProgress((int) playerFragment.getPlaybackPosition());
                seekBarUpdater.run();

                refreshRenderedAnnotations();
                break;

            case PLAYING:
                hideControlsOverlay();

                seekBar.setEnabled(true);
                playPauseButton.setImageResource(R.drawable.ic_action_pause);
                break;

            case PAUSED:
                showControlsOverlay();

                playPauseButton.setImageResource(R.drawable.ic_action_play);
                break;

            case ANNOTATION_PAUSED:
                seekBar.setEnabled(false);
                playPauseButton.setImageResource(R.drawable.ic_action_play);
                break;
        }
    }

    /**
     * Fired when the seek bar value changes. Updates the elapsed and total time text views to
     * reflect the state of the seek bar and tells the player fragment to seek if the change was
     * user-initiated.
     */
    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
        long elapsedTime = (long) (progress / 1000f);
        long totalTime = (long) (bar.getMax() / 1000f);

        String elapsedTimeString = DateUtils.formatElapsedTime(elapsedTime);
        String totalTimeString = DateUtils.formatElapsedTime(totalTime);

        elapsedTimeText.setText(elapsedTimeString);
        totalTimeText.setText(totalTimeString);

        if (fromUser) playerFragment.seekTo(progress);
    }

    /**
     * Fired when the user starts seeking manually. Stops the seek bar updates so that the thumb
     * will stay in place.
     */
    @Override
    public void onStartTrackingTouch(SeekBar bar) {
        seekBarUpdater.stop();
    }

    /**
     * Fired when the user stops seeking manually. Starts updating the seek bar again.
     */
    @Override
    public void onStopTrackingTouch(SeekBar bar) {
        seekBarUpdater.run();
    }

    /**
     * Fired when the user touches the activity. Keeps the controls visible until the event ends.
     */
    @Override
    public boolean dispatchTouchEvent(@Nonnull MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                showControlsOverlay();
                break;

            case MotionEvent.ACTION_MOVE:
                cancelControlsOverlayHide();
                break;

            case MotionEvent.ACTION_UP:
                hideControlsOverlay();
                break;
        }

        return super.dispatchTouchEvent(event);
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
            int oldProgress = seekBar.getProgress();

            // Animate the bar so that playback appears to be smooth
            ObjectAnimator animator =
                    ObjectAnimator.ofInt(seekBar, "progress", oldProgress, progress);

            animator.setDuration(UPDATING_FREQUENCY);
            animator.setInterpolator(new LinearInterpolator());

            animator.start();
        }

    }

}
