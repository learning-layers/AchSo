/**
 * Copyright 2013 Aalto university, see AUTHORS
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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.InformationActivity;
import fi.aalto.legroup.achso.activity.VideoViewerActivity;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.Dialog;
import fi.aalto.legroup.achso.remote.RemoteResultCache;

public class VideoViewerFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";
    private View.OnClickListener mOnInfoClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent informationIntent = new Intent(getActivity(), InformationActivity.class);
            informationIntent.putExtra(ARG_ITEM_ID, selected_video.getId());
            getActivity().startActivityForResult(informationIntent, VideoViewerActivity.REQUEST_VIDEO_INFORMATION);
        }
    };
    public static final String ARG_ITEM_CACHE_POSITION = "item_position_in_search_cache";
    private SemanticVideo selected_video;

    public VideoViewerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long itemid = getActivity().getIntent().getLongExtra(ARG_ITEM_ID, -1);
        if (itemid != -1) {
            Log.i("VideoViewerFragment", "creating details, have ID " + itemid);
            selected_video = VideoDBHelper.getById(itemid);
        } else {
            Log.i("VideoViewerFragment", "creating details, using cached remote video");
            selected_video = RemoteResultCache.getSelectedVideo();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video_viewer, container, false);
        assert(rootView != null);
        if (selected_video != null) {
            final TextView title = ((TextView) rootView.findViewById(R.id.video_title));
            title.setText(selected_video.getTitle());
            assert(title != null && title.getText() != null);
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Dialog.getTextSetterDialog(getActivity(), selected_video, title.getText().toString(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            title.setText(selected_video.getTitle());
                        }
                    }, selected_video).show();
                }
            });
            TextView genre = (TextView) rootView.findViewById(R.id.video_genre);
            genre.setText(SemanticVideo.genreStrings.get(selected_video.getGenre()));
            int alpha = 0x88; // Alpha value of the genre text. Between 0x00 and 0xFF.
            int actualAlpha = (alpha << 24) | 0x00FFFFFF;
            switch (selected_video.getGenre()) {
                case Problem:
                    genre.setTextColor(getResources().getColor(R.color.achso_red) & actualAlpha);
                    break;
                case GoodWork:
                    genre.setTextColor(getResources().getColor(R.color.achso_green) & actualAlpha);
                    break;
                case TrickOfTrade:
                    genre.setTextColor(getResources().getColor(R.color.achso_yellow) & actualAlpha);
                    break;
                case SiteOverview:
                    genre.setTextColor(getResources().getColor(R.color.achso_blue) & actualAlpha);
                    break;
            }
            ImageView info = (ImageView) rootView.findViewById(R.id.more_information_button);
            info.setOnClickListener(mOnInfoClick);
        }
        return rootView;
    }
}
