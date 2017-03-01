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

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.VideoRepository;

public final class DetailFragment extends Fragment {

    private TextView titleField;
    private TextView creatorField;
    private TextView uploadedField;
    private TextView uploadedFieldLabel;

    private ImageButton titleEditButton;

    private ArrayList<Video> videos;

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
        creatorField = (TextView) view.findViewById(R.id.creatorField);
        uploadedField = (TextView) view.findViewById(R.id.uploadedField);
        uploadedFieldLabel = (TextView) view.findViewById(R.id.uploadedFieldLabel);

        titleEditButton = (ImageButton) view.findViewById(R.id.titleEditButton);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        videos = ((DetailActivity) getActivity()).getVideos();

        populateInformation();

        if (videos.size() == 1) {
            titleEditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showTitleEditDialog();
                }
            });
        } else {
            titleEditButton.setVisibility(View.GONE);
        }
    }

    private void populateInformation() {
        Video video = videos.get(0);

        Set authors = new TreeSet<String>();

        String uploaded;
        String authorName = "";

        for (int i = 0; i < videos.size(); i++) {
            String name = videos.get(i).getAuthor().getName();

            if (Strings.isNullOrEmpty(name)) {
                name = getString(R.string.semanticvideo_unknown_creator);
            }

            if (!authors.contains(name)) {
                authors.add(name);
            }
        }

        for (Object obj: authors) {
            String name = (String) obj;
            authorName += name + ", ";
        }

        authorName = authorName.substring(0, authorName.length() - 2);

        if (video.isRemote()) {
            uploaded = getString(R.string.yes);
        } else {
            uploaded = getString(R.string.no);
        }

        creatorField.setText(authorName);

        if (videos.size() == 1) {
            titleField.setText(video.getTitle());
            uploadedField.setText(uploaded);
        } else {
            titleField.setText(videos.size() + " videos");
            uploadedField.setVisibility(View.GONE);
            uploadedFieldLabel.setVisibility(View.GONE);
        }
    }

    public class SaveTitleCallback implements VideoRepository.VideoCallback {
        @Override
        public void found(Video video) { }

        @Override
        public void notFound() {
            SnackbarManager.show(Snackbar.with(getActivity()).text(R.string.storage_error));
        }
    }

    private void showTitleEditDialog() {
        final EditText text = new EditText(getActivity());
        final Video video = videos.get(0);

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
                        video.save(new SaveTitleCallback());

                        populateInformation();
                    }
                })
                .show();
    }

}
