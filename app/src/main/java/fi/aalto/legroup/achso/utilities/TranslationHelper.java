package fi.aalto.legroup.achso.utilities;

import android.content.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fi.aalto.legroup.achso.R;

public class TranslationHelper {

    private Map<String, String> genreMap = new HashMap<>();
    private String language;

    // Use get()
    private TranslationHelper(Context context, String language) {
        this.language = language;

        String[] genreIds = context.getResources().getStringArray(R.array.genre_ids);
        String[] genreTexts = context.getResources().getStringArray(R.array.genre_texts);

        if (genreIds.length == genreTexts.length) {
            for (int i = 0; i < genreIds.length; i++) {
                genreMap.put(genreIds[i], genreTexts[i]);
            }
        }
    }

    /**
     * Get the localized string for a genre id.
     * Eg. good_work -> Good work
     */
    public String getGenreText(String genreId) {
        String text = genreMap.get(genreId);
        if (text != null) {
            return text;
        } else {
            return genreId;
        }
    }

    // Singleton
    private static TranslationHelper helper;
    public static TranslationHelper get(Context context)
    {
        String language = Locale.getDefault().getLanguage();
        if (helper == null || !helper.language.equals(language)) {
            helper = new TranslationHelper(context, language);
        }

        return helper;
    }
}
