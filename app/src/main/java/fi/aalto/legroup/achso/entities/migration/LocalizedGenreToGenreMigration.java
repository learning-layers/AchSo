package fi.aalto.legroup.achso.entities.migration;

import android.content.Context;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Video;

/*
    Update old localized genres to the locale independent genre ids. See ACH-104.
 */
public class LocalizedGenreToGenreMigration extends VideoMigration {

    public LocalizedGenreToGenreMigration() {
        super(1);
    }

    @Override
    public void migrate(Context context, Video video) {
        // Genres have been removed from AchSo as of April 29, 2016
    //    String [] genreIds = context.getResources().getStringArray(R.array.genre_ids);
    //    String[] genreTexts = context.getResources().getStringArray(R.array.genre_texts);

    //    // In case none is found just use the first genre.
    //    String genre = genreIds[0];

   //     if (genreIds.length == genreTexts.length) {
   //         String videoGenre = video.getGenre();
   //         for (int i = 0; i < genreTexts.length; i++) {
   //             if (genreIds[i].equals(videoGenre)) {
   //                 return;
   //             }
   //             if (genreTexts[i].equals(videoGenre)) {
   //                 genre = genreIds[i];
   //             }
   //         }
   //     }

   //     video.setGenre(genre);
    }
}
