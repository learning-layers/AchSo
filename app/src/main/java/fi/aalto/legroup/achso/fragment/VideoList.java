package fi.aalto.legroup.achso.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fi.aalto.legroup.achso.R;

public class VideoList extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View videoList = inflater.inflate(R.layout.fragment_video_list, container, false);
        return videoList;
    }
}