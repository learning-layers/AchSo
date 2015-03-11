package fi.aalto.legroup.achso.views.adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.melnykov.fab.ScrollDirectionListener;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.browsing.BrowserFragment;
import fi.aalto.legroup.achso.views.utilities.ScrollDirectionListenable;

public class VideoTabAdapter extends FragmentStatePagerAdapter implements
        ScrollDirectionListenable {

    private Map<Integer, Object> activeItems = new HashMap<>();
    private List<String> tabNames = new LinkedList<>();

    @Nullable
    private ScrollDirectionListener scrollListener;

    @Nullable
    private ScrollDirectionListenable scrollDelegate;

    public VideoTabAdapter(Context context, FragmentManager manager) {
        super(manager);

        String allVideos = context.getString(R.string.my_videos);
        String[] genres = context.getResources().getStringArray(R.array.genres);

        tabNames.add(allVideos);

        Collections.addAll(tabNames, genres);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position < tabNames.size()) {
            return tabNames.get(position);
        }

        return String.valueOf(position);
    }

    @Override
    public Fragment getItem(int position) {
        List<UUID> videos = getVideosForPosition(position);
        return BrowserFragment.newInstance(videos);
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

    /**
     * Forward the scroll direction listener to the currently active item.
     */
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (scrollDelegate != null) {
            scrollDelegate.setScrollDirectionListener(null);
        }

        if (object instanceof ScrollDirectionListenable) {
            scrollDelegate = ((ScrollDirectionListenable) object);
            scrollDelegate.setScrollDirectionListener(scrollListener);
        }

        // The new fragment is scrolled to the top
        if (scrollListener != null) {
            scrollListener.onScrollUp();
        }

        super.setPrimaryItem(container, position, object);
    }

    @Override
    public int getCount() {
        return tabNames.size();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        for (Map.Entry<Integer, Object> entry : activeItems.entrySet()) {
            Object item = entry.getValue();

            if (item instanceof BrowserFragment) {
                int position = entry.getKey();
                List<UUID> videos = getVideosForPosition(position);

                ((BrowserFragment) item).setVideos(videos);
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
                    String genre = tabNames.get(position);
                    return App.videoInfoRepository.getByGenreString(genre);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
        }

        return Collections.emptyList();
    }

    /**
     * Sets the scroll direction listener.
     *
     * @param listener Scroll direction listener, or null to remove a previously set listener.
     */
    @Override
    public void setScrollDirectionListener(@Nullable ScrollDirectionListener listener) {
        this.scrollListener = listener;
    }

}
