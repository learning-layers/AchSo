package fi.aalto.legroup.achso.playback;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.googlecode.mp4parser.FileDataSourceImpl;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import static android.media.MediaCodec.CryptoException;
import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION;
import static com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;

/**
 * Reads a video's rotation and transforms the given surface texture accordingly. This is an ugly
 * but working attempt to fix a bug in ExoPlayer: https://github.com/google/ExoPlayer/issues/91
 *
 * TODO: Get rid of this when the issue is fixed.
 *
 * On API 16 and below:
 *   - Video orientation must be read using a third-party library, e.g. mp4parser
 *
 * On API 17 and above:
 *   - Video orientation can be read using MediaMetadataRetriever
 *
 * On API 20 and below:
 *   - ExoPlayer does not rotate the video automatically
 *
 * On API 21 and above:
 *   - ExoPlayer rotates the video automatically
 *
 * On all API levels:
 *   - Reported video dimensions in onVideoSizeChanged must be swapped if the video is portrait
 *
 * @author Leo Nikkilä
 */
public final class VideoOrientationPatcher implements MediaCodecVideoTrackRenderer.EventListener,
        View.OnLayoutChangeListener {

    private Context context;
    private MediaCodecVideoTrackRenderer.EventListener delegate;

    private int rotationDegrees = 0;
    private boolean isPortrait = false;

    @Nullable
    private TextureView view;

    public VideoOrientationPatcher(Context context,
                                   MediaCodecVideoTrackRenderer.EventListener delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public void setVideoUri(@Nullable Uri videoUri) {
        if (videoUri != null) {
            rotationDegrees = readOrientation(context, videoUri);
            isPortrait = (rotationDegrees == 90 || rotationDegrees == 270);
        }
    }

    public void setView(@Nullable TextureView view) {
        if (this.view != null) {
            this.view.removeOnLayoutChangeListener(this);
        }

        if (view != null) {
            view.addOnLayoutChangeListener(this);
        }

        this.view = view;
    }

    /**
     * Returns the orientation of the given video, or -1 if it cannot be read.
     */
    private int readOrientation(Context context, Uri videoUri) {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                return readOrientationApi17Plus(context, videoUri);
            } else {
                return readOrientationApi16(videoUri);
            }
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Reads orientation metadata using MediaMetadataRetriever and returns the angle of the video.
     *
     * @throws IOException If the video cannot be parsed.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int readOrientationApi17Plus(Context context, Uri videoUri) throws IOException {
        MediaMetadataRetriever retriever = null;

        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, videoUri);

            return Integer.parseInt(retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid orientation.", e);
        } finally {
            if (retriever != null) {
                retriever.release();
            }
        }
    }

    /**
     * Reads orientation metadata using mp4parser and returns the angle of the video.
     *
     * @throws IOException If the video cannot be parsed.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int readOrientationApi16(Uri videoUri) throws IOException {
        String scheme = videoUri.getScheme();

        if (scheme == null || !scheme.equals("file")) {
            throw new IOException("Scheme must be file://.");
        }

        File videoFile = new File(videoUri.getPath());
        IsoFile isoFile = new IsoFile(new FileDataSourceImpl(videoFile));

        com.googlecode.mp4parser.util.Matrix rotationMatrix = parseRotationMatrix(isoFile);

        return getRotationAngle(rotationMatrix);
    }

    /**
     * Returns the mp4parser rotation matrix for the given file or null if none is found.
     */
    @Nullable
    private com.googlecode.mp4parser.util.Matrix parseRotationMatrix(IsoFile file) {
        MovieBox movieBox = file.getMovieBox();

        if (movieBox == null) {
            return null;
        }

        for (Box box : movieBox.getBoxes()) {
            String type = box.getType();

            if (type.equals("trak")) {
                TrackHeaderBox headerBox = ((TrackBox) box).getTrackHeaderBox();

                if (headerBox == null) {
                    return null;
                }

                return headerBox.getMatrix();
            }
        }

        return null;
    }

    /**
     * Returns the angle for the given rotation matrix. Returns -1 if the matrix is not one of
     * Matrix.ROTATE_0, Matrix.ROTATE_90, Matrix.ROTATE_180 or Matrix.ROTATE_270.
     */
    private int getRotationAngle(@Nullable com.googlecode.mp4parser.util.Matrix matrix) {
        if (matrix == null) {
            return -1;
        } else if (matrix.equals(com.googlecode.mp4parser.util.Matrix.ROTATE_0)) {
            return 0;
        } else if (matrix.equals(com.googlecode.mp4parser.util.Matrix.ROTATE_90)) {
            return 90;
        } else if (matrix.equals(com.googlecode.mp4parser.util.Matrix.ROTATE_180)) {
            return 180;
        } else if (matrix.equals(com.googlecode.mp4parser.util.Matrix.ROTATE_270)) {
            return 270;
        } else {
            return -1;
        }
    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {
        delegate.onDroppedFrames(count, elapsed);
    }

    @Override
    @SuppressWarnings("SuspiciousNameCombination")
    public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
        // Reverse the reported width and height if the video is portrait
        if (isPortrait) {
            delegate.onVideoSizeChanged(height, width, pixelWidthHeightRatio);
        } else {
            delegate.onVideoSizeChanged(width, height, pixelWidthHeightRatio);
        }
    }

    @Override
    public void onDrawnToSurface(Surface surface) {
        delegate.onDrawnToSurface(surface);
    }

    @Override
    public void onDecoderInitializationError(DecoderInitializationException exception) {
        delegate.onDecoderInitializationError(exception);
    }

    @Override
    public void onCryptoError(CryptoException exception) {
        delegate.onCryptoError(exception);
    }

    /**
     * Rotates the video when the TextureView's layout bounds change.
     * Video dimensions are preserved.
     */
    @Override
    public void onLayoutChange(View changedView, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {

        // Not needed on API 21 and up
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            return;
        }

        if (view == null || changedView != view) {
            return;
        }

        Matrix transform = new Matrix();

        int width = right - left;
        int height = bottom - top;

        float pivotX = width / 2f;
        float pivotY = height / 2f;

        transform.postRotate(rotationDegrees, pivotX, pivotY);

        // If the video is portrait, preserve its dimensions by scaling them back
        if (isPortrait) {
            float aspectRatio = (float) width / height;
            transform.postScale(1 * aspectRatio, 1 / aspectRatio, pivotX, pivotY);
        }

        view.setTransform(transform);
        view.invalidate();
    }

}
