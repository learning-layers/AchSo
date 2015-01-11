package fi.aalto.legroup.achso.fragment;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bugsnag.android.Bugsnag;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.annotation.AnnotationEditor;
import fi.aalto.legroup.achso.annotation.AnnotationRenderService;
import fi.aalto.legroup.achso.annotation.PlaybackTimer;
import fi.aalto.legroup.achso.annotation.renderers.MarkerRenderer;
import fi.aalto.legroup.achso.annotation.renderers.PauseRenderer;
import fi.aalto.legroup.achso.annotation.renderers.SubtitleRenderer;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.view.MarkerCanvas;

/**
 * Provides a convenient fragment for playing annotated videos. The host is responsible for
 * implementing any playback controls.
 *
 * TODO: Decouple from MediaPlayer and provide an ExoPlayer alternative for API 16 and up.
 */
public class VideoPlayerFragment extends Fragment implements TextureView.SurfaceTextureListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnVideoSizeChangedListener,
        PauseRenderer.PauseListener {

    public static final String STATE_AUTO_PLAY = "STATE_AUTO_PLAY";
    public static final String STATE_INITIAL_POSITION = "STATE_INITIAL_POSITION";

    private State state = State.UNPREPARED;

    private AnnotationEditor annotationEditor;

    private FrameLayout videoContainer;
    private TextureView videoSurface;
    private MarkerCanvas markerCanvas;

    private ProgressBar bufferProgress;
    private ProgressBar pauseProgress;

    private LinearLayout subtitleContainer;

    private MediaPlayer mediaPlayer;
    private PlaybackTimer playbackTimer;

    private AnnotationRenderService annotationRenderService;
    private PauseRenderer pauseRenderer;

    private PlaybackStateListener listener;

    private boolean isMediaPlayerPrepared = false;
    private boolean isSurfacePrepared = false;

    private boolean stateAutoPlay = true;
    private long stateInitialPosition = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        return inflater.inflate(R.layout.fragment_video_player, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore a previously saved state if one exists
        if (savedInstanceState != null) {
            stateAutoPlay = savedInstanceState.getBoolean(STATE_AUTO_PLAY);
            stateInitialPosition = savedInstanceState.getLong(STATE_INITIAL_POSITION);
        }

        videoContainer = (FrameLayout) view.findViewById(R.id.surfaceContainer);

        markerCanvas = (MarkerCanvas) view.findViewById(R.id.markerContainer);
        videoSurface = (TextureView) view.findViewById(R.id.videoSurface);
        videoSurface.setSurfaceTextureListener(this);

        bufferProgress = (ProgressBar) view.findViewById(R.id.bufferProgress);
        pauseProgress = (ProgressBar) view.findViewById(R.id.pauseProgress);

        pauseProgress.setVisibility(View.GONE);

        subtitleContainer = (LinearLayout) view.findViewById(R.id.subtitleContainer);
    }

    @Override
    public void onResume() {
        super.onResume();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnVideoSizeChangedListener(this);
        mediaPlayer.setOnPreparedListener(this);

        annotationRenderService = new AnnotationRenderService();
        pauseRenderer = new PauseRenderer(this);

        playbackTimer = new PlaybackTimer(mediaPlayer);
        playbackTimer.setListener(new PlaybackTimer.Listener() {
            @Override
            public void onPlaybackTimerTick(long position) {
                annotationRenderService.render(position);
            }
        });
    }

    @Override
    public void onPause() {
        // Set values for onSaveInstanceState()
        stateAutoPlay = mediaPlayer.isPlaying();
        stateInitialPosition = mediaPlayer.getCurrentPosition();

        isMediaPlayerPrepared = false;
        isSurfacePrepared = false;

        playbackTimer.release();
        annotationRenderService.release();
        mediaPlayer.release();

        mediaPlayer = null;

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // The values to these are set in onPause() since the MediaPlayer has already been
        // destroyed when we get here.
        savedInstanceState.putBoolean(STATE_AUTO_PLAY, stateAutoPlay);
        savedInstanceState.putLong(STATE_INITIAL_POSITION, stateInitialPosition);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Prepare the fragment for video playback.
     *
     * @throws IOException if the video file is unreachable
     */
    public void prepare() throws IOException {
        if (annotationEditor == null) throw new IllegalStateException("Annotation editor unset.");

        annotationRenderService.addRenderer(new MarkerRenderer(annotationEditor, markerCanvas));
        annotationRenderService.addRenderer(new SubtitleRenderer(subtitleContainer));
        annotationRenderService.addRenderer(pauseRenderer);

        mediaPlayer.prepareAsync();
    }

    /**
     * We're not prepared unless both the MediaPlayer and the Surface are ready.
     */
    private void finishPreparing() {
        if (!isMediaPlayerPrepared || !isSurfacePrepared) return;

        SurfaceTexture texture = videoSurface.getSurfaceTexture();
        Surface surface = new Surface(texture);
        mediaPlayer.setSurface(surface);

        bufferProgress.setVisibility(View.GONE);

        seekTo(stateInitialPosition);
        setState(State.PREPARED);

        if (stateAutoPlay) play();
    }

    public void play() {
        // Stop an annotation pause if one is in progress
        pauseRenderer.stop();

        mediaPlayer.start();
        playbackTimer.start();
        setState(State.PLAYING);
    }

    public void pause() {
        mediaPlayer.pause();
        playbackTimer.stop();
        setState(State.PAUSED);
    }

    public void seekTo(long position) {
        mediaPlayer.seekTo((int) position);

        annotationRenderService.recalculateRendered(position);

        if (mediaPlayer.isPlaying()) {
            annotationRenderService.render(position);
        } else {
            // If paused and seeking, do fuzzy rendering so we can show annotations on screen even
            // if the user didn't seek to the exact position.
            annotationRenderService.fuzzyRender(position);
        }
    }

    public void setListener(PlaybackStateListener listener) {
        this.listener = listener;
    }

    public void setVideo(Video video) throws IOException {
        mediaPlayer.setDataSource(getActivity(), video.getVideoUri());
    }

    public void setAnnotations(List<Annotation> annotations) {
        annotationRenderService.setAnnotations(annotations);
        annotationRenderService.recalculateRendered(getPlaybackPosition());
        annotationRenderService.render(getPlaybackPosition());
    }

    public void setAnnotationEditor(AnnotationEditor annotationEditor) {
        this.annotationEditor = annotationEditor;
    }

    public State getState() {
        return state;
    }

    public long getPlaybackPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return mediaPlayer.getDuration();
    }

    public LinearLayout getSubtitleContainer() {
        return subtitleContainer;
    }

    private void setState(State state) {
        this.state = state;

        if (listener != null) listener.onPlaybackStateChanged(state);
    }

    @Override
    public void startAnnotationPause(int duration) {
        if (state != State.PLAYING) return;

        mediaPlayer.pause();
        playbackTimer.stop();
        setState(State.ANNOTATION_PAUSED);

        // Show and animate progress bar
        pauseProgress.setVisibility(View.VISIBLE);

        ObjectAnimator.ofInt(pauseProgress, "progress", 0, pauseProgress.getMax())
                .setDuration(duration)
                .start();
    }

    @Override
    public void stopAnnotationPause() {
        if (state != State.ANNOTATION_PAUSED) return;

        pauseProgress.setVisibility(View.GONE);

        play();
    }

    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        isSurfacePrepared = true;
        finishPreparing();
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture texture) {}

    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {}

    /**
     * Called when the MediaPlayer is ready.
     */
    @Override
    public void onPrepared(MediaPlayer player) {
        isMediaPlayerPrepared = true;
        finishPreparing();
    }

    /**
     * Resize the surface container to match the aspect ratio of the video but still fill its
     * parent view.
     */
    @Override
    public void onVideoSizeChanged(MediaPlayer player, int width, int height) {
        View containerParent = ((View) videoContainer.getParent());
        if (containerParent == null) return;

        float videoAspectRatio = (float) width / height;

        int parentWidth = containerParent.getWidth();
        int parentHeight = containerParent.getHeight();

        float widthRatio = (float) parentWidth / width;
        float heightRatio = (float) parentHeight / height;

        int containerWidth;
        int containerHeight;

        if (widthRatio > heightRatio) {
            containerWidth = (int) (parentHeight * videoAspectRatio);
            containerHeight = parentHeight;
        } else {
            containerWidth = parentWidth;
            containerHeight = (int) (parentWidth / videoAspectRatio);
        }

        ViewGroup.LayoutParams params = videoContainer.getLayoutParams();

        params.width = containerWidth;
        params.height = containerHeight;

        videoContainer.setLayoutParams(params);
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        pause();
        seekTo(0);
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        SparseArray<String> messages = new SparseArray<>();

        messages.put(MediaPlayer.MEDIA_ERROR_SERVER_DIED, "Lost connection to media server.");
        messages.put(MediaPlayer.MEDIA_ERROR_UNKNOWN, "Unknown error with media player.");
        messages.put(MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK,
                "Video not valid for streaming.");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            messages.put(MediaPlayer.MEDIA_ERROR_IO, "Network error.");
            messages.put(MediaPlayer.MEDIA_ERROR_MALFORMED, "Malformed media data.");
            messages.put(MediaPlayer.MEDIA_ERROR_TIMED_OUT, "Connection timed out.");
            messages.put(MediaPlayer.MEDIA_ERROR_UNSUPPORTED, "Unsupported media format.");
        }

        String defaultMessage = String.format("Media player error: (%s, %s)", what, extra);
        String message = messages.get(extra, defaultMessage);

        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

        Bugsnag.notify(new Exception(defaultMessage));

        return true;
    }

    public enum State {
        UNPREPARED,
        PREPARED,
        PLAYING,
        PAUSED,
        ANNOTATION_PAUSED
    }

    public static interface PlaybackStateListener {

        public void onPlaybackStateChanged(State state);

    }

}
