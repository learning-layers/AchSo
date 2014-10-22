package fi.aalto.legroup.achso.fragment;

import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.api.client.util.Strings;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.InformationActivity;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.Dialog;

public class InformationFragment extends Fragment {

    TextView titleField;
    TextView genreField;
    TextView creatorField;
    TextView qrCodeField;
    TextView uploadedField;

    ImageButton titleEditButton;
    ImageButton genreEditButton;

    SemanticVideo video;

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

        final InformationActivity activity = (InformationActivity) getActivity();
        video = activity.getVideo();

        populateInformation();

        titleEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog.getTextSetterDialog(activity, video, video.getTitle(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                populateInformation();
                            }
                        }, video).show();
            }
        });

        genreEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog.getGenreDialog(activity, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        video.setGenre(SemanticVideo.Genre.values()[which]);

                        VideoDBHelper dbHelper = new VideoDBHelper(activity);
                        dbHelper.update(video);
                        dbHelper.close();

                        populateInformation();
                    }
                }, null).show();
            }
        });
    }

    private void populateInformation() {
        String creator = video.getCreator();
        String qrCode = video.getQrCode();
        String uploaded;

        if (Strings.isNullOrEmpty(creator)) {
            creator = getString(R.string.semanticvideo_unknown_creator);
        }

        if (Strings.isNullOrEmpty(qrCode)) {
            qrCode = getString(R.string.semanticvideo_no_qr);
        }

        if (video.isUploaded()) {
            uploaded = getString(R.string.yes);
        } else {
            uploaded = getString(R.string.no);
        }

        titleField.setText(video.getTitle());
        genreField.setText(video.getGenreText());
        creatorField.setText(creator);
        qrCodeField.setText(qrCode);
        uploadedField.setText(uploaded);
    }

}
