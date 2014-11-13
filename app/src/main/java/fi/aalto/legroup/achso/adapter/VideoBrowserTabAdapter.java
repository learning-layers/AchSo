package fi.aalto.legroup.achso.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.fragment.VideoBrowserFragment;

/**
 * Created by lassi on 31.10.14.
 */
public class VideoBrowserTabAdapter extends FragmentStatePagerAdapter {
    private Context context;
    private String searchQuery = null;
    private VideoBrowserFragment searchPage = null;
    private int genrePageLimit = 1;
    private HashMap<Integer, VideoBrowserFragment> activeFragments;


    public VideoBrowserTabAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.activeFragments = new HashMap<Integer, VideoBrowserFragment>();
        this.context = context;
    }

    public void closeContextualActionMode() {
        for (Integer i : this.activeFragments.keySet()) {
            this.activeFragments.get(i).closeContextualActionMode();
        }
    }

    public List<SemanticVideo> getVideosForPosition(int position) {
        if (position >= this.genrePageLimit) {
            return VideoDBHelper.getVideosByGenre(SemanticVideo.Genre.values()[position - this.genrePageLimit]);
        } else {
            switch (position) {
                case 0:
                    return VideoDBHelper.getVideoCache();
            }
        }

        return new ArrayList<SemanticVideo>();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        VideoBrowserFragment videoBrowser = (VideoBrowserFragment) super.instantiateItem(container, position);

        videoBrowser.setVideos(this.getVideosForPosition(position));

        if (position >= this.genrePageLimit) {
            videoBrowser.setType(VideoBrowserFragment.Type.genre);
        } else {
            switch (position) {
                case 0:
                    videoBrowser.setType(VideoBrowserFragment.Type.home);
                    break;
            }
        }
        return videoBrowser;
    }

    public Fragment getItem(int i) {
        Fragment fragment = Fragment.instantiate(this.context, VideoBrowserFragment.class.getName());
        this.activeFragments.put(i, (VideoBrowserFragment) fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        this.activeFragments.remove(object);
        super.destroyItem(container, position, object);
    }


    public VideoBrowserFragment getFragmentAtIndex(int index) {
        return this.activeFragments.get(index);
    }

    public int getCount() {
        int count = SemanticVideo.Genre.values().length + this.genrePageLimit;
        return count;
    }
}