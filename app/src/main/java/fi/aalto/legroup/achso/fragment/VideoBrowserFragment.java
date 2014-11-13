package fi.aalto.legroup.achso.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.ActionbarActivity;
import fi.aalto.legroup.achso.adapter.VideoBrowserGridAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.helper.QRHelper;
import fi.aalto.legroup.achso.helper.UploadHelper;
import fi.aalto.legroup.achso.helper.VideoHelper;
import fi.aalto.legroup.achso.service.UploaderService;
import fi.aalto.legroup.achso.view.VideoGridItemView;

public class VideoBrowserFragment extends Fragment {

    public static enum Type {
        home, search, genre
    }

    private VideoBrowserGridAdapter gridAdapter;
    private GridView grid;
    private List<SemanticVideo> videos;
    private ArrayList<Integer> selection;
    private TextView emptyText;
    private Menu menu;
    private Type type;

    public VideoGridItemView getViewForVideo(SemanticVideo video) {
        int position = this.videos.indexOf(video);

        if(position < 0) {
            return null;
        }

        return (VideoGridItemView)this.grid.getChildAt(position);
    }

    public void setVideos(List<SemanticVideo> videos) {
        this.videos = videos;

        if (this.emptyText != null) {
            if (this.videos.size() > 0) {
                this.emptyText.setVisibility(View.GONE);
            } else {
                this.emptyText.setVisibility(View.VISIBLE);
            }
        }

        if (this.gridAdapter != null) {
            this.gridAdapter.setVideos(this.videos);
            this.gridAdapter.notifyDataSetChanged();
        }
    }

    public void setType(Type type) {
        this.type = type;
    }

    private List<SemanticVideo> selectionAsVideos() {
        List<SemanticVideo> list = new ArrayList<SemanticVideo>();

        for (int i : this.selection) {
            list.add(this.videos.get(i));
        }

        return list;
    }

    public void closeContextualActionMode() {
        this.grid.setChoiceMode(GridView.CHOICE_MODE_NONE);
        this.grid.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View videoList = inflater.inflate(R.layout.fragment_video_browser, container, false);
        this.selection = new ArrayList<Integer>();

        this.grid = (GridView) videoList.findViewById(R.id.grid_video_list);
        this.gridAdapter = new VideoBrowserGridAdapter(videoList.getContext());
        this.gridAdapter.setVideos(this.videos);
        this.grid.setAdapter(this.gridAdapter);

        this.emptyText = (TextView) videoList.findViewById(R.id.empty_text);
        this.emptyText.setVisibility(View.GONE);

        if (this.videos != null) {
            if (this.videos.size() > 0) {
                this.emptyText.setVisibility(View.GONE);
            } else {
                this.emptyText.setVisibility(View.VISIBLE);
            }
        }




        this.grid.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        this.grid.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
                videoAtIndexWasChecked(i, b);
                if(selection.size() > 1) {
                    VideoBrowserFragment.this.menu.findItem(R.id.action_view_video_info).setVisible(false);
                } else {
                    VideoBrowserFragment.this.menu.findItem(R.id.action_view_video_info).setVisible(true);
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.video_context_menu, menu);
                VideoBrowserFragment.this.menu = menu;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_qr_to_video:
                        QRHelper.readQRCodeForVideos(getActivity(), selectionAsVideos(), actionMode);
                        break;
                    case R.id.action_delete:
                        VideoHelper.deleteVideos(getActivity(), selectionAsVideos(), actionMode);
                        break;
                    case R.id.action_upload:
                        UploadHelper.uploadVideos(getActivity(), selectionAsVideos(), actionMode);
                        break;
                    case R.id.action_view_video_info:
                        VideoHelper.viewVideoInfo(getActivity(), selectionAsVideos().get(0), actionMode);
                        break;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                selection.clear();
            }
        });

        this.grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                videoAtIndexWasClicked(i);
            }
        });

        return videoList;
    }

    protected void videoAtIndexWasClicked(int index) {
        VideoHelper.showVideo(this.getActivity(), this.videos.get(index));
    }

    protected void videoAtIndexWasChecked(int index, boolean isChecked) {
        int position = this.selection.indexOf(index);
        if (position > -1) {
            this.selection.remove(position);
        } else {
            this.selection.add(index);
        }
    }
}