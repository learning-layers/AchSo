package fi.aalto.legroup.achso.authoring;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Video;

public final class ExportDialogFragment extends DialogFragment {

    private static final String ARG_EMAIL = "ARG_EMAIL";

    private TextView emailText;
    private Activity activity;

    private ArrayList<Video> videos;

    public static ExportDialogFragment newInstance(String email,  ArrayList<Video> videos) {
        ExportDialogFragment fragment = new ExportDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        fragment.setVideos(videos);
        return fragment;
    }

    private  void setVideos(ArrayList<Video> videos) {
        this.videos = videos;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.activity = getActivity();

        LayoutInflater inflater = LayoutInflater.from(this.activity);

        // Building dialogs with views is one of the rare cases where null as the root is valid.
        View view = inflater.inflate(R.layout.fragment_export, null);

        this.emailText = (TextView) view.findViewById(R.id.export_email);

        // Pre-fill email and name fields
        String email = getArguments().getString(ARG_EMAIL);
        this.emailText.setText(email);

        // Don't close the dialog if the user taps the background
        setCancelable(false);

        return new MaterialDialog.Builder(this.activity)
                .title(getResources().getString(R.string.video_export_title, this.videos.size()))
                .positiveText(R.string.feedback_send)
                .negativeText(R.string.cancel)
                .callback(new ButtonCallback())
                .customView(view, true)
                .build();
    }

    private  void exportVideos() {
        ExportHelper.ExportPayload payload = new ExportHelper.ExportPayload(this.emailText.getText().toString(), this.videos);
        ExportHelper.ExportVideosTask task = new ExportHelper.ExportVideosTask(new BrowseExportCallback());
        task.execute(payload);
    }

    private class ButtonCallback extends MaterialDialog.ButtonCallback {
        @Override
        public void onPositive(MaterialDialog dialog) {
            exportVideos();
        }
    }

    private class BrowseExportCallback implements ExportHelper.ExportCallback {
        @Override
        public void success(ExportHelper.ExportResponse response) {
            System.out.println(response.message);
        }

        @Override
        public void failure(String reason) {
            System.out.println(reason);
        }
    }
}
