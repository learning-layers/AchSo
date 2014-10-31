package fi.aalto.legroup.achso.adapter;

import fi.aalto.legroup.achso.fragment.VideoList;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by lassi on 31.10.14.
 */
public class VideoListTabAdapter extends FragmentStatePagerAdapter {
    private Context context;

    public VideoListTabAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
    }

    public Fragment getItem(int i) {
        Fragment fragment = Fragment.instantiate(this.context, VideoList.class.getName());
        return fragment;
    }

    public int getCount() {
        return 5;
    }
}