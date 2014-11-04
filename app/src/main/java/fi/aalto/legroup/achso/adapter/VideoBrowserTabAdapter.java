package fi.aalto.legroup.achso.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.Objects;

import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.fragment.VideoBrowserFragment;

/**
 * Created by lassi on 31.10.14.
 */
public class VideoBrowserTabAdapter extends FragmentStatePagerAdapter {
    private Context context;
    private VideoDBHelper databaseHelper;

    public VideoBrowserTabAdapter(Context context, FragmentManager fm, VideoDBHelper database) {
        super(fm);
        this.databaseHelper = database;
        this.context = context;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        VideoBrowserFragment videoBrowser = (VideoBrowserFragment)super.instantiateItem(container, position);

        if (position > 0) {
            videoBrowser.setVideos(VideoDBHelper.getVideosByGenre(SemanticVideo.Genre.values()[position - 1].name()));
        } else {
            videoBrowser.setVideos(VideoDBHelper.getVideoCache());
        }
        return videoBrowser;
    }

    public Fragment getItem(int i) {
        return Fragment.instantiate(this.context, VideoBrowserFragment.class.getName());
    }

    public int getCount() {
        return 5;
    }
}