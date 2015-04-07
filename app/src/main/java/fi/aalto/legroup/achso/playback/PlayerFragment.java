package fi.aalto.legroup.achso.playback;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.FrameworkSampleSource;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.rollbar.android.Rollbar;

import java.util.List;

import javax.annotation.Nullable;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.playback.annotations.AnnotationRenderer;
import fi.aalto.legroup.achso.playback.annotations.MarkerStrategy;
import fi.aalto.legroup.achso.playback.annotations.SubtitleStrategy;
import fi.aalto.legroup.achso.playback.utilities.VideoOrientationPatcher;
import fi.aalto.legroup.achso.views.MarkerCanvas;

import static android.media.MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT;

/**
 * Provides a convenient fragment for playing annotated videos. The host is responsible for
 * implementing any playback controls.
 */
public final class PlayerFragment extends Fragment implements ExoPlayer.Listener,
        TextureView.SurfaceTextureListener, MediaCodecVideoTrackRenderer.EventListener,
        AnnotationRenderer.EventListener {

    public static final String STATE_AUTO_PLAY = "STATE_AUTO_PLAY";
    public static final String STATE_INITIAL_POSITION = "STATE_INITIAL_POSITION";

    // Number of ExoPlayer renderers in total
    private static final int RENDERER_COUNT = 3;

    // Number of framework renderers (currently video and audio)
    private static final int DOWNSTREAM_RENDERER_COUNT = 2;

    private State state = State.UNPREPARED;

    private FrameLayout videoContainer;
    private TextureView videoSurface;
    private MarkerCanvas markerCanvas;

    private ProgressBar bufferProgress;
    private ProgressBar pauseProgress;

    private LinearLayout subtitleContainer;

    private ExoPlayer exoPlayer;
    private TrackRenderer videoRenderer;
    private AnnotationRenderer annotationRenderer;

    private PlaybackStateListener listener;

    private VideoOrientationPatcher orientationPatcher;

    private boolean isExoPlayerPrepared = false;
    private boolean isSurfacePrepared = false;

    private boolean stateAutoPlay = true;
    private long stateInitialPosition = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
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

        // ExoPlayer doesn't handle video orientation correctly: we need to fix it manually.
        orientationPatcher = new VideoOrientationPatcher(getActivity(), this);

        // Control the media volume instead of the ringer volume
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        super.onResume();

        exoPlayer = ExoPlayer.Factory.newInstance(RENDERER_COUNT);
        exoPlayer.addListener(this);
    }

    @Override
    public void onPause() {
        // Set values for onSaveInstanceState()
        stateAutoPlay = exoPlayer.getPlayWhenReady();
        stateInitialPosition = exoPlayer.getCurrentPosition();

        isExoPlayerPrepared = false;
        isSurfacePrepared = false;

        exoPlayer.release();
        exoPlayer = null;

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // The values to these are set in onPause() since ExoPlayer has already been destroyed.
        savedInstanceState.putBoolean(STATE_AUTO_PLAY, stateAutoPlay);
        savedInstanceState.putLong(STATE_INITIAL_POSITION, stateInitialPosition);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Prepare the fragment for video playback.
     */
    public void prepare(Uri videoUri, AnnotationEditor annotationEditor) {
        orientationPatcher.setVideoUri(videoUri);
        orientationPatcher.setView(videoSurface);

        SampleSource source = new FrameworkSampleSource(
                getActivity(),
                videoUri,
                null,
                DOWNSTREAM_RENDERER_COUNT
        );

        // The video renderer runs on another thread: we need to supply a handler on the main
        // thread in order to receive events.
        Handler mainHandler = new Handler(Looper.getMainLooper());

        videoRenderer = new MediaCodecVideoTrackRenderer(
                source,
                VIDEO_SCALING_MODE_SCALE_TO_FIT,
                0,
                mainHandler,
                orientationPatcher,
                0
        );

        annotationRenderer = new AnnotationRenderer(
                getActivity(),
                this,
                new MarkerStrategy(markerCanvas, annotationEditor),
                new SubtitleStrategy(R.layout.subtitle, subtitleContainer)
        );

        TrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(source);

        exoPlayer.prepare(videoRenderer, annotationRenderer, audioRenderer);
    }

    /**
     * We're not prepared unless both ExoPlayer and the surface are ready.
     */
    private void finishPreparing() {
        if (!(isExoPlayerPrepared && isSurfacePrepared)) {
            return;
        }

        SurfaceTexture texture = videoSurface.getSurfaceTexture();
        Surface surface = new Surface(texture);

        // Tell the video renderer which surface to use
        exoPlayer.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);

        bufferProgress.setVisibility(View.GONE);

        seekTo(stateInitialPosition);
        setState(State.PREPARED);

        if (stateAutoPlay) {
            play();
        }
    }

    public void play() {
        exoPlayer.setPlayWhenReady(true);
    }

    public void pause() {
        exoPlayer.setPlayWhenReady(false);
    }

    public void seekTo(long position) {
        exoPlayer.seekTo(position);
    }

    public void setAnnotations(List<Annotation> annotations) {
        annotationRenderer.setAnnotations(annotations);
    }

    public void setListener(PlaybackStateListener listener) {
        this.listener = listener;
    }

    public State getState() {
        return state;
    }

    public long getPlaybackPosition() {
        return exoPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return exoPlayer.getDuration();
    }

    public LinearLayout getSubtitleContainer() {
        return subtitleContainer;
    }

    private void setState(State state) {
        this.state = state;

        if (listener != null) {
            listener.onPlaybackStateChanged(state);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        isSurfacePrepared = true;
        finishPreparing();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture texture) {}

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {}

    /**
     * Invoked when the value returned from either ExoPlayer#getPlayWhenReady() or
     * ExoPlayer#getPlaybackState() changes.
     *
     * @param playWhenReady Whether playback will proceed when ready.
     * @param playbackState One of the STATE constants defined in this class.
     */
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_READY:
                if (!isExoPlayerPrepared) {
                    isExoPlayerPrepared = true;
                    finishPreparing();
                }

                break;

            case ExoPlayer.STATE_ENDED:
                pause();
                seekTo(0);
                break;
        }
    }

    /**
     * Invoked when the current value of ExoPlayer#getPlayWhenReady() has been reflected by the
     * internal playback thread.
     *
     * An invocation of this method will shortly follow any call to
     * ExoPlayer#setPlayWhenReady(boolean) that changes the state. If multiple calls are made in
     * rapid succession, then this method will be invoked only once, after the final state has been
     * reflected.
     */
    @Override
    public void onPlayWhenReadyCommitted() {
        if (exoPlayer.getPlayWhenReady()) {
            setState(State.PLAYING);
        } else {
            setState(State.PAUSED);
        }
    }

    /**
     * Invoked when an error occurs. The playback state will transition to ExoPlayer#STATE_IDLE
     * immediately after this method is invoked. The player instance can still be used, and
     * ExoPlayer#release() must still be called on the player should it no longer be required.
     */
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        SnackbarManager.show(Snackbar.with(getActivity()).text(R.string.playback_error));
        Rollbar.reportException(error);
    }

    /**
     * Invoked each time there's a change in the size of the video being rendered.
     */
    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
        ViewParent parent = videoContainer.getParent();
        View parentView;

        if (parent instanceof View) {
            parentView = (View) parent;
        } else {
            // The container does not have a view parent, using layout parameters won't work. This
            // should not happen since we're in control of the layout.
            return;
        }

        float videoAspectRatio = (float) width / height;

        int parentWidth = parentView.getWidth();
        int parentHeight = parentView.getHeight();

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

    /**
     * Invoked when a decoder fails to initialize.
     */
    @Override
    public void onDecoderInitializationError(MediaCodecTrackRenderer.
                                                         DecoderInitializationException error) {
        Rollbar.reportException(error);
    }

    /**
     * Invoked when a decoder operation raises a CryptoException.
     */
    @Override
    public void onCryptoError(MediaCodec.CryptoException error) {
        Rollbar.reportException(error);
    }

    /**
     * Invoked when a frame is rendered to a surface for the first time following that surface
     * having been set as the target for the renderer.
     */
    @Override
    public void onDrawnToSurface(Surface surface) {}

    /**
     * Invoked to report the number of frames dropped by the renderer.
     */
    @Override
    public void onDroppedFrames(int count, long elapsed) {}

    /**
     * Invoked when an annotation pause starts.
     *
     * @param duration Pause duration in milliseconds.
     */
    @Override
    public void onAnnotationPauseStart(int duration) {
        // Show and animate progress bar
        pauseProgress.setVisibility(View.VISIBLE);

        ObjectAnimator.ofInt(pauseProgress, "progress", 0, pauseProgress.getMax())
                .setDuration(duration)
                .start();

        setState(State.ANNOTATION_PAUSED);
    }

    /**
     * Invoked when an annotation pause ends.
     */
    @Override
    public void onAnnotationPauseEnd() {
        pauseProgress.setVisibility(View.GONE);

        if (state == State.ANNOTATION_PAUSED) {
            setState(State.PLAYING);
        }
    }

    public static enum State {
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
