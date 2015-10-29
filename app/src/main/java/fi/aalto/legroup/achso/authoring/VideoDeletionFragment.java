package fi.aalto.legroup.achso.authoring;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;

/**
 * A dialog fragment that confirms the deletion of the given videos and then deletes them.
 */
public final class VideoDeletionFragment extends DialogFragment {

    private List<UUID> videoIds;

    public static VideoDeletionFragment newInstance(List<UUID> videoIds) {
        VideoDeletionFragment fragment = new VideoDeletionFragment();

        fragment.setVideos(videoIds);

        return fragment;
    }

    @Nonnull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.deletion_title)
                .content(R.string.deletion_question)
                .positiveText(R.string.delete)
                .negativeText(R.string.cancel)
                .callback(new DialogCallback())
                .build();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setVideos(List<UUID> videoIds) {
        this.videoIds = videoIds;
    }

    private void deleteQuietly(UUID videoId) {
        try {
            // TODO: Should not rely on a specific instance
            App.videoRepository.delete(videoId);
        } catch (IOException e) {
            e.printStackTrace();
            SnackbarManager.show(
                Snackbar.with(getActivity()).text(e.getMessage()));
        }
    }

    private class DialogCallback extends MaterialDialog.ButtonCallback {

        @Override
        public void onPositive(MaterialDialog dialog) {
            for (UUID videoId : videoIds) {
                deleteQuietly(videoId);
            }
        }

    }

}
