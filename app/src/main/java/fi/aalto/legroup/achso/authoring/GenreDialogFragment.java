package fi.aalto.legroup.achso.authoring;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import fi.aalto.legroup.achso.R;

public class GenreDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private int selection = 0;

    private String[] genres;
    private Callback callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        genres = getResources().getStringArray(R.array.genres);

        setCancelable(false);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.genre_selection_title)
                .setSingleChoiceItems(genres, selection, this)
                .setPositiveButton(R.string.select_genre, this)
                .create();
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

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * Called when an item is selected.
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                finish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                // Not used
                break;

            case DialogInterface.BUTTON_NEUTRAL:
                // Not used
                break;

            default:
                selection = which;
        }
    }

    private void finish() {
        if (callback != null) {
            String selectedGenre = genres[selection];
            callback.onGenreSelected(selectedGenre);
        }
    }

    public static interface Callback {

        public void onGenreSelected(String genre);

    }

}
