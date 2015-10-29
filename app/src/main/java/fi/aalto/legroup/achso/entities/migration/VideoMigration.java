package fi.aalto.legroup.achso.entities.migration;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

import fi.aalto.legroup.achso.entities.Video;

/**
 * Inherit this to create a migration to update a video manifest from an old format version to a
 * newer one.
 */
public abstract class VideoMigration {

    private static VideoMigration[] allMigrations = {
            new LocalizedGenreToGenreMigration(),
    };

    private int formatVersion;

    /**
     * Get the list of migrations for updating a video from {@code formatVersion }
     * @param formatVersion Format version of the video to migrate.
     */
    public static List<VideoMigration> get(int formatVersion) {
        List<VideoMigration> migrations = Arrays.asList(allMigrations);

        int startIndex = migrations.size();
        for (int i = 0; i < migrations.size(); i++) {
            if (formatVersion < migrations.get(i).formatVersion) {
                startIndex = i;
                break;
            }
        }

        return migrations.subList(startIndex, migrations.size());
    }

    /**
     * @param formatVersion Format version that the migration migrates _to_, applied to all videos
     *                      below the specified version.
     */
    public VideoMigration(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    /**
     * Perform the migration.
     */
    public abstract void migrate(Context context, Video video);
}
