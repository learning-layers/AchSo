package fi.aalto.legroup.achso.browsing;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.api.client.util.Strings;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.authoring.GenreDialogFragment;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.utilities.TranslationHelper;

public final class DetailFragment extends Fragment {

    private TextView titleField;
    private TextView genreField;
    private TextView creatorField;
    private TextView qrCodeField;
    private TextView uploadedField;

    private ImageButton titleEditButton;
    private ImageButton genreEditButton;

    private Video video;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_information, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleField = (TextView) view.findViewById(R.id.titleField);
        genreField = (TextView) view.findViewById(R.id.genreField);
        creatorField = (TextView) view.findViewById(R.id.creatorField);
        qrCodeField = (TextView) view.findViewById(R.id.qrCodeField);
        uploadedField = (TextView) view.findViewById(R.id.uploadedField);

        titleEditButton = (ImageButton) view.findViewById(R.id.titleEditButton);
        genreEditButton = (ImageButton) view.findViewById(R.id.genreEditButton);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        video = ((DetailActivity) getActivity()).getVideo();

        populateInformation();

        titleEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTitleEditDialog();
            }
        });

        genreEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showGenreEditDialog();
            }
        });
    }

    private void populateInformation() {
        String authorName = video.getAuthor().getName();
        String qrCode = video.getId().toString();
        String uploaded;

        if (Strings.isNullOrEmpty(authorName)) {
            authorName = getString(R.string.semanticvideo_unknown_creator);
        }

        if (Strings.isNullOrEmpty(qrCode)) {
            qrCode = getString(R.string.semanticvideo_no_qr);
        }

        if (video.isRemote()) {
            uploaded = getString(R.string.yes);
        } else {
            uploaded = getString(R.string.no);
        }

        TranslationHelper translationHelper = TranslationHelper.get(getActivity());

        titleField.setText(video.getTitle());
        genreField.setText(translationHelper.getGenreText(video.getGenre()));
        creatorField.setText(authorName);
        qrCodeField.setText(qrCode);
        uploadedField.setText(uploaded);
    }

    private void showTitleEditDialog() {
        final EditText text = new EditText(getActivity());

        text.setSingleLine();
        text.setText(video.getTitle());

        new AlertDialog.Builder(getActivity())
                .setView(text)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        String title = text.getText().toString();

                        if (Strings.isNullOrEmpty(title)) return;

                        video.setTitle(title);

                        if (!video.save()) {
                            SnackbarManager.show(
                                    Snackbar.with(getActivity()).text(R.string.storage_error));
                        }

                        populateInformation();
                    }
                })
                .show();
    }

    private void showGenreEditDialog() {
        GenreDialogFragment fragment = new GenreDialogFragment();

        fragment.setCallback(new GenreDialogFragment.Callback() {
            @Override
            public void onGenreSelected(String genre) {
                video.setGenre(genre);

                if (!video.save()) {
                    SnackbarManager.show(Snackbar.with(getActivity()).text(R.string.storage_error));
                }

                populateInformation();
            }
        });

        fragment.show(getFragmentManager(), "GenreDialogFragment");
    }

}
