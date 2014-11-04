package fi.aalto.legroup.achso.fragment;

import android.content.Intent;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.VideoViewerActivity;
import fi.aalto.legroup.achso.adapter.VideoBrowserGridAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;

public class VideoBrowserFragment extends Fragment {
    private VideoBrowserGridAdapter gridAdapter;
    private GridView grid;
    private List<SemanticVideo> videos;

    public void setVideos(List<SemanticVideo> videos) {
        this.videos = videos;
        if (this.gridAdapter != null) {
            this.gridAdapter.setVideos(this.videos);
            this.grid.invalidate();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View videoList = inflater.inflate(R.layout.fragment_video_browser, container, false);
        this.grid = (GridView) videoList.findViewById(R.id.grid_video_list);
        this.gridAdapter = new VideoBrowserGridAdapter(videoList.getContext());
        this.gridAdapter.setVideos(this.videos);
        this.grid.setAdapter(this.gridAdapter);
        this.grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                videoAtIndexWasClicked(i);
            }
        });

        return videoList;
    }

    protected void videoAtIndexWasClicked(int index) {
        SemanticVideo video = this.videos.get(index);
        Intent detailIntent = new Intent(this.getActivity(), VideoViewerActivity.class);
        detailIntent.putExtra(VideoViewerFragment.ARG_ITEM_ID, video.getId());
        startActivity(detailIntent);
    }
}