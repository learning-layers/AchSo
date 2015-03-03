package fi.aalto.legroup.achso.playback.utilities;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

import java.io.File;

import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION;

/**
 * Reads the orientation of a video using Android's MediaMetadataRetriever.
 *
 * @author Leo Nikkilä
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public final class FrameworkOrientationReader {

    private FrameworkOrientationReader() {
        // Static only
    }

    /**
     * Returns the orientation of the given video, or -1 if it cannot be determined.
     */
    public static int readOrientation(Context context, File file) {
        MediaMetadataRetriever retriever = null;

        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, Uri.fromFile(file));

            return Integer.parseInt(retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION));
        } catch (NumberFormatException e) {
            return -1;
        } finally {
            if (retriever != null) {
                retriever.release();
            }
        }
    }

}
