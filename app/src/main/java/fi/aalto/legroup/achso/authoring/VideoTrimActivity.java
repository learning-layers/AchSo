package fi.aalto.legroup.achso.authoring;

import android.content.Intent;
import android.graphics.PointF;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.serialization.Serializable;
import fi.aalto.legroup.achso.playback.AnnotationEditor;
import fi.aalto.legroup.achso.playback.PlayerFragment;
import fi.aalto.legroup.achso.views.MarkedSeekBar;

public class VideoTrimActivity extends ActionBarActivity implements PlayerFragment.PlaybackStateListener,
        SeekBar.OnSeekBarChangeListener, AnnotationEditor, View.OnClickListener {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";

    private Video video;
    private UUID id;
    private int startTrimTime;
    private int endTrimTime;
    private PlayerFragment playerFragment;

    private LinearLayout playbackControls;

    private MarkedSeekBar seekBar;

    private ImageButton playPauseButton;

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
        seekBar = (MarkedSeekBar) findViewById(R.id.seekBar);
        playPauseButton.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        this.id = UUID.fromString(intent.getStringExtra(ARG_VIDEO_ID));
    }

    @Override
    public void createAnnotation(PointF position) {

    }

    @Override
    public void editAnnotation(Annotation annotation) {

    }

    @Override
    public void moveAnnotation(Annotation annotation, PointF position) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onPlaybackStateChanged(PlayerFragment.State state) {

    }

    @Override
    public void onClick(View view) {

    }
}
