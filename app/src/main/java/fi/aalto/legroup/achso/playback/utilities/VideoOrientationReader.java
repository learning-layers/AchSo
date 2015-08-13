package fi.aalto.legroup.achso.playback.utilities;

import android.content.Context;
import android.os.Build;

import java.io.File;

public class VideoOrientationReader {

    private VideoOrientationReader() {
        // Static only
    }

    /**
     * Returns the orientation of the given video, or -1 if it cannot be read.
     */
    public static int readOrientation(Context context, File file) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            return FrameworkOrientationReader.readOrientation(context, file);
        } else {
            return Mp4ParserOrientationReader.readOrientation(file);
        }
    }
}
