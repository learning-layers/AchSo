package fi.aalto.legroup.achso.playback.player;

import android.app.Activity;
import android.media.MediaCodec;
import android.net.Uri;
import android.view.Surface;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the exo player so others don't have to.
 */
public class VideoPlayer implements ExoPlayer.Listener, MediaCodecVideoTrackRenderer.EventListener {

    // For some reason ExoPlayer needs to be told how much memory to use. This value (10MiB) is just
    // taken from the ExoPlayer demo project.
    protected static final int EXO_BUFFER_SIZE_BYTES = 10 * 1024 * 1024;
    
    // ExoPlayer needs to be told also how many renderers use one source. We need one for the video
    // and one for the audio.
    protected static final int EXO_DOWNSTREAM_RENDERER_COUNT = 2;

    // Exo player error sources, see onExoError.
    protected static final int EXO_ERROR_SOURCE_PLAYER_ERROR = 1;
    protected static final int EXO_ERROR_SOURCE_DECODER_INITALIZE = 2;
    protected static final int EXO_ERROR_SOURCE_CRYPTO_ERROR = 3;

    protected List<TrackRenderer> extraRenderers = new ArrayList<>();

    protected ExoPlayer exoPlayer;
    protected MediaCodecVideoTrackRenderer videoRenderer;
    protected MediaCodecAudioTrackRenderer audioRenderer;
    
    protected Activity activity;
    protected String ownUserAgent;

    /**
     * Add an additional TrackRenderer to the ExoPlayer. Must be called before the initialization
     * methods.
     */
    public void addExtraRenderer(TrackRenderer renderer) {

        extraRenderers.add(renderer);
    }

    /**
     * Set up the ExoPlayer to read an mp4.
     * @param uri The location of the resource to play.
     */
    public void initializeMp4(Uri uri) {

        int rendererCount = EXO_DOWNSTREAM_RENDERER_COUNT + extraRenderers.size();
        ArrayList<TrackRenderer> allRenderers = new ArrayList<>(rendererCount);
        allRenderers.add(videoRenderer);
        allRenderers.add(audioRenderer);
        allRenderers.addAll(extraRenderers);

        exoPlayer = ExoPlayer.Factory.newInstance(rendererCount);
        exoPlayer.addListener(this);

        String userAgent = Util.getUserAgent(activity, ownUserAgent);
        DataSource dataSource = new DefaultUriDataSource(activity, userAgent);

        Mp4Extractor extractor = new Mp4Extractor();
        
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(
                uri, dataSource, extractor,
                
                // Number of track renderers dependent on this sample source.
                EXO_DOWNSTREAM_RENDERER_COUNT, 

                // The requested total buffer size for storing sample data, in bytes. The actual
                // allocated size may exceed the value passed in if the implementation requires it.
                EXO_BUFFER_SIZE_BYTES 
            );

        videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);

        exoPlayer.prepare((TrackRenderer[])allRenderers.toArray());
    }

    public void stop() {

        exoPlayer.release();
        exoPlayer = null;
    }

    /**
     * Returns a human readable error string of the error source.
     *
     * @param source One of the EXO_ERROR_SOURCE_ constants
     */
    static protected String getExoErrorSourceText(int source) {

        switch (source) {
            case EXO_ERROR_SOURCE_PLAYER_ERROR:
                return "Exo player error";
            case EXO_ERROR_SOURCE_DECODER_INITALIZE:
                return "Failed to initialize decoder";
            case EXO_ERROR_SOURCE_CRYPTO_ERROR:
                return "Cryptographic error";
            default:
                return "Internal error";
        }
    }

    /**
     * Generic ExoPlayer error handling. When any ExoPlayer callback happens it's forwarded here.
     *
     * @param source One of the EXO_ERROR_SOURCE_ constants
     * @param error  The exception which was given from exo player.
     */
    protected void onExoError(int source, Exception error) {
        throw new RuntimeException(getExoErrorSourceText(source) + ": " + error.getMessage(),
                error);
    }

    // Callbacks for ExoPlayer.Listener
    // ================================

    /**
     * Invoked when an error occurs.
     */
    @Override
    public void onPlayerError(ExoPlaybackException error) {
        onExoError(EXO_ERROR_SOURCE_PLAYER_ERROR, error);
    }

    /**
     * Invoked when the value returned from either ExoPlayer.getPlayWhenReady() or
     * ExoPlayer.getPlaybackState() changes.
     */
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    /**
     * Invoked when the current value of ExoPlayer.getPlayWhenReady() has been reflected by the
     * internal playback thread.
     */
    @Override
    public void onPlayWhenReadyCommitted() {

    }

    // Callbacks for MediaCodecTrackRenderer.EventListener
    // ===================================================

    /**
     * Invoked when a decoder operation raises a MediaCodec.CryptoException.
     */
    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {
        onExoError(EXO_ERROR_SOURCE_CRYPTO_ERROR, e);
    }

    /**
     * Invoked when a decoder fails to initialize.
     */
    @Override
    public void onDecoderInitializationError(
            MediaCodecTrackRenderer.DecoderInitializationException e) {
        onExoError(EXO_ERROR_SOURCE_DECODER_INITALIZE, e);
    }

    /**
     * Invoked when a decoder is successfully created.
     */
    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
            long initializationDurationMs) {

    }

    // Callbacks for MediaCodecVideoTrackRenderer.EventListener
    // ========================================================

    /**
     * Invoked when a frame is rendered to a surface for the first time following that surface
     * having been set as the target for the renderer.
     */
    @Override
    public void onDrawnToSurface(Surface surface) {
    }

    /**
     * Invoked to report the number of frames dropped by the renderer.
     */
    @Override
    public void onDroppedFrames(int count, long elapsed) {
    }

    /**
     * Invoked each time there's a change in the size of the video being rendered.
     */
    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
    }
}

