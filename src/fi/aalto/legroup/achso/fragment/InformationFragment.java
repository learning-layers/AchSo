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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.InformationActivity;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.Dialog;

public class InformationFragment extends ListFragment {

    private SemanticVideo mVideo;
    private List<? extends Map<String, ?>> mMaps;
    private Context mCtx;

    public InformationFragment() {
    }

    public InformationFragment(SemanticVideo video) {
        mVideo = video;
    }

    private HashMap<String, String> getData(int rId, String text) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("lhs", getResources().getString(rId));
        map.put("rhs", text);
        return map;
    }

    private String firstLetterToUppercase(String str) {
        char[] arr = str.toCharArray();
        arr[0] = Character.toUpperCase(arr[0]);
        return new String(arr);
    }

    private ArrayList<HashMap<String, String>> generateData() {
        ArrayList<HashMap<String, String>> maps = new ArrayList<HashMap<String, String>>();
        maps.add(getData(R.string.semanticvideo_title, mVideo.getTitle()));
        maps.add(getData(R.string.semanticvideo_genre, mVideo.getGenreText()));
        maps.add(getData(R.string.semanticvideo_creator, mVideo.getCreator() == null ?
                getResources().getString(R.string.semanticvideo_unknown_creator) :
                mVideo.getCreator()));
        maps.add(getData(R.string.semanticvideo_qrcode, mVideo.getQrCode() == null ? getResources().getString(R.string.semanticvideo_no_qr) : mVideo.getQrCode()));
        maps.add(getData(R.string.semanticvideo_uploaded, firstLetterToUppercase(Boolean.toString(mVideo.isUploaded()))));
        maps.add(getData(R.string.semanticvideo_latitude, mVideo.getLocation() == null ?
                getResources().getString(R.string.semanticvideo_unknown_location) :
                Double.toString(mVideo.getLocation().getLatitude())));
        maps.add(getData(R.string.semanticvideo_longitude, mVideo.getLocation() == null ?
                getResources().getString(R.string.semanticvideo_unknown_location) :
                Double.toString(mVideo.getLocation().getLongitude())));
        maps.add(getData(R.string.semanticvideo_accuracy, mVideo.getLocation() == null ?
                getResources().getString(R.string.semanticvideo_unknown_location) :
                Double.toString(mVideo.getLocation().getAccuracy())));
        return maps;
    }

    private void setAdapter() {
        setListAdapter(new SimpleAdapter(getActivity(),
                mMaps,
                android.R.layout.simple_list_item_2,
                new String[]{"lhs", "rhs"},
                new int[]{android.R.id.text1, android.R.id.text2}));
    }

    private AlertDialog getAction(String key, String value) {
        if (key.equals(getString(R.string.semanticvideo_title))) {
            return Dialog.getTextSetterDialog(mCtx, mVideo, value, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mMaps = generateData();
                    setAdapter();
                    ((InformationActivity) getActivity()).informationChanged = true;
                }
            }, mVideo);
        } else if (key.equals(getString(R.string.semanticvideo_genre))) {
            return Dialog.getGenreDialog(mCtx, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mVideo.setGenre(SemanticVideo.Genre.values()[which]);
                    VideoDBHelper dbh = new VideoDBHelper(mCtx);
                    dbh.update(mVideo);
                    dbh.close();

                    mMaps = generateData();
                    setAdapter();
                    ((InformationActivity) getActivity()).informationChanged = true;

                    dialog.dismiss();
                }
            }, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    // default behavior is fine -- OnCancelListener is required here,
                    // but it's abstract class and cannot be used without overriding something.
                }
            });
        } else return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mMaps = generateData();
        setAdapter();

        mCtx = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_information, container, false);
    }

    @Override
    public void onListItemClick(final ListView l, View view, int position, long id) {
        TwoLineListItem li = (TwoLineListItem) view;
        TextView li1 = li.getText1();
        TextView li2 = li.getText2();
        CharSequence cs1 = li1 != null ? li1.getText() : null;
        CharSequence cs2 = li2 != null ? li2.getText() : null;

        AlertDialog actionDialog = getAction(cs1 != null ? cs1.toString() : "", cs2 != null ? cs2.toString() : "");
        if (actionDialog != null) {
            actionDialog.show();
        }
    }
}
