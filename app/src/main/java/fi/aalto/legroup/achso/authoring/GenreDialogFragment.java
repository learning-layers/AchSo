package fi.aalto.legroup.achso.authoring;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import fi.aalto.legroup.achso.R;

/**
 * A dialog fragment for selecting a genre for videos.
 */
public final class GenreDialogFragment extends DialogFragment {

    @Nullable
    private Callback callback;

    private String[] genreIds, genreTexts;

    private int selection = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy the fragment on orientation change, preserving the selection and callback
        // fields.
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        genreIds = getResources().getStringArray(R.array.genre_ids);
        genreTexts = getResources().getStringArray(R.array.genre_texts);

        DialogCallback dialogCallback = new DialogCallback();

        setCancelable(false);

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.genre_selection_title)
                .positiveText(R.string.select_genre)
                .items(genreTexts)
                .itemsCallbackSingleChoice(selection, dialogCallback)
                .alwaysCallSingleChoiceCallback()
                .callback(dialogCallback)
                .build();
    }

    /**
     * Workaround for the bug where the dialog gets dismissed on rotation when using
     * setRetainInstance(true).
     *
     * See https://code.google.com/p/android/issues/detail?id=17423
     */
    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();

        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }

        super.onDestroyView();
    }

    /**
     * Sets the callback that should be notified with events from this fragment. Pass null to
     * remove the callback.
     *
     * @param callback Callback that should be set or null.
     *
     * @return This fragment for chaining.
     */
    public GenreDialogFragment setCallback(@Nullable Callback callback) {
        this.callback = callback;
        return this;
    }

    /**
     * Callback that should be notified with events from this fragment.
     */
    public static interface Callback {

        /**
         * Called when the user has selected a genre using the dialog.
         *
         * @param genre User's selected genre.
         */
        public void onGenreSelected(String genre);

    }

    /**
     * Class for listening to dialog events.
     */
    private class DialogCallback extends MaterialDialog.ButtonCallback implements
            MaterialDialog.ListCallbackSingleChoice {

        /**
         * Called each time the user selects an item.
         */
        @Override
        public boolean onSelection(MaterialDialog dialog, View item, int index, CharSequence label) {
            selection = index;
            return true;
        }

        /**
         * Called when the user presses the positive button.
         */
        @Override
        public void onPositive(MaterialDialog dialog) {
            String genre = genreIds[selection];

            if (callback != null) {
                callback.onGenreSelected(genre);
            }
        }

    }

}
