/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

    public final String TAG = this.getClass().getSimpleName();

    TextView titleField;
    TextView genreField;
    TextView creatorField;
    TextView qrCodeField;
    TextView uploadedField;

    ImageButton titleEditButton;
    ImageButton genreEditButton;

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
        final SemanticVideo video = activity.getVideo();

        populateInformationFromVideo(video);

        titleEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog.getTextSetterDialog(activity, video, video.getTitle(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                populateInformationFromVideo(video);
                            }
                        }, video).show();
            }
        });

        genreEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog.getGenreDialog(activity,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                video.setGenre(SemanticVideo.Genre.values()[which]);
                                VideoDBHelper dbh = new VideoDBHelper(activity);
                                dbh.update(video);
                                dbh.close();

                                populateInformationFromVideo(video);
                            }
                        }, new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {

                            }
                        }).show();
            }
        });
    }

    private void populateInformationFromVideo(SemanticVideo video) {
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
