package fi.aalto.legroup.achso.playback.utilities;

import com.google.common.base.Objects;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.util.Matrix;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

/**
 * Reads the orientation of an MP4 video using mp4parser.
 */
public final class Mp4ParserOrientationReader {

    /**
     * Handler for video tracks in MP4 files, see ISO/IEC 14496-12.
     */
    private static final String VIDEO_TRACK_HANDLER = "vide";

    private Mp4ParserOrientationReader() {
        // Static only
    }

    /**
     * Returns the orientation of the given video, or -1 if it cannot be determined.
     */
    public static int readOrientation(File file) {
        DataSource dataSource = null;

        try {
            dataSource = new FileDataSourceImpl(file);

            Movie movie = MovieCreator.build(dataSource);
            Matrix matrix = parseMatrix(movie);

            return getDegrees(matrix);
        } catch (IOException e) {
            return -1;
        } finally {
            closeQuietly(dataSource);
        }
    }

    /**
     * Returns the best rotation matrix for the given movie, or null if none is found.
     */
    @Nullable
    private static Matrix parseMatrix(Movie movie) {
        Matrix movieMatrix = movie.getMatrix();
        Matrix trackMatrix = null;

        // Only video tracks have matrices that are useful to us.
        for (Track track : movie.getTracks()) {
            if (track.getHandler().equals(VIDEO_TRACK_HANDLER)) {
                trackMatrix = track.getTrackMetaData().getMatrix();
                break;
            }
        }

        // If the matrices are equal, there won't be any confusion.
        if (Objects.equal(movieMatrix, trackMatrix)) {
            return movieMatrix;
        }

        // If either of them is null, return the non-null one.
        if (movieMatrix == null) {
            return trackMatrix;
        } else if (trackMatrix == null) {
            return movieMatrix;
        }

        // This is rare: they're different and neither of them is null. We have to make an educated
        // guess. The track's matrix is more specific — it could be that the video file has many
        // tracks with different orientations. Since Android defaults to playing the first track,
        // let's return that one.
        return trackMatrix;
    }

    /**
     * Returns the corresponding degree value for the given rotation matrix, or -1 if the matrix
     * is not one of Matrix.ROTATE_0, Matrix.ROTATE_90, Matrix.ROTATE_180 or Matrix.ROTATE_270.
     */
    private static int getDegrees(@Nullable Matrix matrix) {
        if (Matrix.ROTATE_0.equals(matrix)) {
            return 0;
        } else if (Matrix.ROTATE_90.equals(matrix)) {
            return 90;
        } else if (Matrix.ROTATE_180.equals(matrix)) {
            return 180;
        } else if (Matrix.ROTATE_270.equals(matrix)) {
            return 270;
        }

        return -1;
    }

    private static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
