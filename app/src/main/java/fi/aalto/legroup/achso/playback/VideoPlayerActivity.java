package fi.aalto.legroup.achso.playback;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
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
import android.widget.Toast;

import com.bugsnag.android.Bugsnag;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.authoring.QRHelper;
import fi.aalto.legroup.achso.authoring.VideoHelper;
import fi.aalto.legroup.achso.browsing.DetailActivity;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.local.ExportCreatorTask;
import fi.aalto.legroup.achso.storage.local.ExportCreatorTaskResultEvent;
import fi.aalto.legroup.achso.utilities.RepeatingTask;
import fi.aalto.legroup.achso.views.MarkedSeekBar;

/**
 * Handles view logic for the video player controls. Actual playback is handled by
 * VideoPlayerFragment.
 *
 * TODO: Extract annotation editing into a separate fragment.
 */
public final class VideoPlayerActivity extends ActionBarActivity implements AnnotationEditor,
        VideoPlayerFragment.PlaybackStateListener, SeekBar.OnSeekBarChangeListener,
        View.OnClickListener {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";

    // How long the user can be inactive before the controls get hidden (in milliseconds)
    private static final int CONTROLS_HIDE_DELAY = 5000;

    // Animation duration for hiding and showing controls (in milliseconds)
    private static final int CONTROLS_ANIMATION_DURATION = 300;

    private VideoPlayerFragment playerFragment;

    private RelativeLayout controlsOverlay;
    private LinearLayout playbackControls;
    private LinearLayout annotationControls;

    private ImageButton playPauseButton;
    private TextView elapsedTimeText;
    private MarkedSeekBar seekBar;

    private Toolbar toolbar;
    private Button deleteButton;
    private Button saveButton;
    private EditText annotationText;

    private Video video;

    private Uri intentFile;

    private Handler controllerVisibilityHandler = new Handler();
    private SeekBarUpdater seekBarUpdater = new SeekBarUpdater();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.bus.register(this);
        setContentView(R.layout.activity_video_player);

        Intent intent = this.getIntent();
        this.intentFile = intent.getData();

        this.toolbar = (Toolbar) this.findViewById(R.id.toolbar);

        setSupportActionBar(this.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        controlsOverlay = (RelativeLayout) findViewById(R.id.controlsOverlay);
        playbackControls = (LinearLayout) findViewById(R.id.playbackControls);
        playPauseButton = (ImageButton) findViewById(R.id.playPauseButton);
        elapsedTimeText = (TextView) findViewById(R.id.elapsedTimeText);
        seekBar = (MarkedSeekBar) findViewById(R.id.seekBar);

        playPauseButton.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();


        UUID videoId = null;
        if (this.intentFile != null) {
            String filename = new File(this.intentFile.getPath()).getName();
            VideoHelper.moveFile(this.intentFile, App.localStorageDirectory.getPath() + "/");
            videoId = VideoHelper.unpackAchsoFile(filename);
            App.videoInfoRepository.invalidateAll();
        } else {
            videoId = (UUID) getIntent().getSerializableExtra(ARG_VIDEO_ID);
        }

        try {
            video = App.videoRepository.get(videoId);
            populateVideoInformation();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        playerFragment = (VideoPlayerFragment)
                getFragmentManager().findFragmentById(R.id.videoPlayerFragment);

        try {
            playerFragment.setVideo(video);
            playerFragment.setListener(this);
            playerFragment.setAnnotationEditor(this);

            playerFragment.prepare();
        } catch (IOException e) {
            // TODO: Show error message
            Bugsnag.notify(e);
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        App.bus.unregister(this);
        seekBarUpdater.stop();
        controllerVisibilityHandler.removeCallbacksAndMessages(null);

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.bus.register(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        List<UUID> videos = Arrays.asList(video.getId());

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_share:
                new ExportCreatorTask(this).execute(video.getId());
                return true;

            case R.id.action_view_video_info:
                Intent informationIntent = new Intent(this, DetailActivity.class);
                informationIntent.putExtra(DetailActivity.ARG_VIDEO_ID, video.getId());
                startActivity(informationIntent);
                return true;

            case R.id.action_delete:
                VideoHelper.deleteVideos(VideoPlayerActivity.this, videos, null);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when a view with this listener is clicked.
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.playPauseButton:
                togglePlayback();
                break;
        }
    }

    private void populateVideoInformation() {
        toolbar.setTitle(video.getTitle());
        toolbar.setSubtitle(video.getGenre());
    }

    private void refreshRenderedAnnotations() {
        List<Annotation> annotations = video.getAnnotations();
        List<Integer> markers = new ArrayList<>();

        playerFragment.setAnnotations(annotations);

        for (Annotation annotation : annotations) {
            markers.add((int) annotation.getTime());
        }

        seekBar.setMarkers(markers);
    }

    public void togglePlayback() {
        if (playerFragment.getState() == VideoPlayerFragment.State.PLAYING) {
            playerFragment.pause();
        } else {
            playerFragment.play();
        }
    }

    private void anchorSubtitleContainerTo(View view) {
        int height = 0;

        if (view != null) {
            height = view.getHeight();
        }

        playerFragment.getSubtitleContainer().setPadding(0, 0, 0, height);
    }

    /**
     * Show the controls overlay.
     */
    private void showControlsOverlay() {
        cancelControlsOverlayHide();

        controlsOverlay.animate().alpha(1).setDuration(CONTROLS_ANIMATION_DURATION).start();
        toolbar.animate().translationY(0).setDuration(CONTROLS_ANIMATION_DURATION).start();

        anchorSubtitleContainerTo(playbackControls);
    }

    /**
     * Hide controls using a delay.
     */
    private void hideControlsOverlay() {
        controllerVisibilityHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Don't hide if we're paused
                if (playerFragment.getState() == VideoPlayerFragment.State.PAUSED) {
                    return;
                }

                toolbar.animate().alpha(0).setDuration(CONTROLS_ANIMATION_DURATION).start();
                controlsOverlay.animate().alpha(0).setDuration(CONTROLS_ANIMATION_DURATION).start();

                anchorSubtitleContainerTo(null);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        QRHelper.readQRCodeResult(this, requestCode, resultCode, data);
    }

    @Override
    public void createAnnotation(PointF position) {
        // Allow creating annotations only when paused
        if (playerFragment.getState() != VideoPlayerFragment.State.PAUSED) {
            return;
        }

        long time = playerFragment.getPlaybackPosition();

        Annotation annotation = new Annotation(time, position, "", App.loginManager.getUser());

        video.getAnnotations().add(annotation);

        if (!video.save()) {
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
        }

        editAnnotation(annotation);

        refreshRenderedAnnotations();
    }

    @Override
    public void moveAnnotation(Annotation annotation, PointF position) {
        annotation.setPosition(position);

        if (!video.save()) {
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
        }
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

                if (!video.save()) {
                    Toast.makeText(VideoPlayerActivity.this, R.string.storage_error,
                            Toast.LENGTH_LONG).show();
                }

                refreshRenderedAnnotations();

                hideAnnotationControls();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                video.getAnnotations().remove(annotation);

                if (!video.save()) {
                    Toast.makeText(VideoPlayerActivity.this, R.string.storage_error,
                            Toast.LENGTH_LONG).show();
                }

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
     * Fired when the seek bar value changes. Updates the elapsed time to reflect the state of the
     * seek bar and tells the player fragment to seek if the change was user-initiated.
     */
    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
        long elapsedTime = (long) (progress / 1000f);
        String elapsedTimeString = DateUtils.formatElapsedTime(elapsedTime);

        elapsedTimeText.setText(elapsedTimeString);

        if (fromUser) {
            playerFragment.seekTo(progress);
        }
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

    @Subscribe
    public void onExportCreatorTaskResult(ExportCreatorTaskResultEvent event) {
        List<Uri> uris = event.getResult();

        if (uris == null || uris.isEmpty()) {
            App.showError(R.string.error_sharing);
            return;
        }

        Intent intent;

        if (uris.size() == 1) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(uris));
        }

        intent.setType("application/achso");

        startActivity(Intent.createChooser(intent, getString(R.string.video_share)));
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
