package fi.aalto.legroup.achso.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.fragment.VideoBrowserFragment;
import fi.aalto.legroup.achso.util.App;

/**
 * Created by lassi on 31.10.14.
 */
public class VideoTabAdapter extends FragmentStatePagerAdapter {
    String[] tabNames;

    private final int EXTRA_TABS = 1;

    private Map<Integer, Object> activeItems = new HashMap<>();

    private Context context;

    public VideoTabAdapter(Context context, FragmentManager manager) {
        super(manager);

        this.context = context;
        this.tabNames = this.context.getResources().getStringArray(R.array.genres);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getString(R.string.my_videos);
        }

        if (position - 1 < this.tabNames.length) {
            return this.tabNames[position - 1];
        }

        return Integer.toString(position);
    }

    @Override
    public Fragment getItem(int position) {
        List<UUID> videos = this.getVideosForPosition(position);
        return VideoBrowserFragment.newInstance(videos);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object item = super.instantiateItem(container, position);

        activeItems.put(position, item);

        return item;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object item) {
        activeItems.remove(position);

        super.destroyItem(container, position, item);
    }

    @Override
    public int getCount() {
        return this.tabNames.length + this.EXTRA_TABS;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        for (Map.Entry<Integer, Object> entry : activeItems.entrySet()) {
            Object item = entry.getValue();

            if (item instanceof VideoBrowserFragment) {
                int position = entry.getKey();

                List<UUID> videos = getVideosForPosition(position);
                ((VideoBrowserFragment) item).setVideos(videos);
            }
        }
    }

    private List<UUID> getVideosForPosition(int position) {
        switch (position) {
            case 0:
                try {
                    return App.videoInfoRepository.getAll();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            default:
                try {
                    return App.videoInfoRepository.getByGenreString(this.tabNames[position - 1]);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
        }

        return new ArrayList<>();
    }

}
