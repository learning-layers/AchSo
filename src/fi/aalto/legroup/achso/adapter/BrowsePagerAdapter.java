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

package fi.aalto.legroup.achso.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.VideoBrowserActivity;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.fragment.BrowseFragment;

public class BrowsePagerAdapter extends FragmentStatePagerAdapter {

    private int mCurrentIndex;
    private Context mContext;
    private HashMap<Integer, BrowseFragment> mFragments;
    private List<Integer> mOrderedFragments;
    private List<String> mPageTitles;
    // Page ids -- these are used in many places to recognize different features for pages
    // These are also used as keys for mFragments hashmap.
    public final static int MY_VIDEOS = 0;
    public final static int RECOMMENDED = 1;
    public final static int LATEST = 2;
    public final static int NEARBY = 3;
    public final static int BROWSE_BY_GENRE = 30;
    public static final int QR_SEARCH = 50;
    public final static int SEARCH = 60;


    /**
     * Browse pager adapter delivers browse page fragments. They need to be presentable as list,
     * where user can swipe to next fragment. However sometimes some fragments may not be
     * available in list, but they should still exist somewhere (search results, empty pages).
     *
     * Pages are stored in HashMap mFragments, but they are browsed by using list
     * mOrderedFragments, which contains keys to hash map.
     *
     * @param ctx
     * @param fm
     */

    public BrowsePagerAdapter(Context ctx, FragmentManager fm) {
        super(fm);
        mContext = ctx;
        // Initialize list of pages, the actual fragments don't exist until first navigated there
        mOrderedFragments = new ArrayList<Integer>();
        mOrderedFragments.add(MY_VIDEOS);
        mOrderedFragments.add(RECOMMENDED);
        mOrderedFragments.add(LATEST);
        mOrderedFragments.add(NEARBY);
        mOrderedFragments.add(BROWSE_BY_GENRE + SemanticVideo.Genre.GoodWork.ordinal());
        mOrderedFragments.add(BROWSE_BY_GENRE + SemanticVideo.Genre.Problem.ordinal());
        mOrderedFragments.add(BROWSE_BY_GENRE + SemanticVideo.Genre.TrickOfTrade.ordinal());
        mOrderedFragments.add(BROWSE_BY_GENRE + SemanticVideo.Genre.SiteOverview.ordinal());
        mFragments = new HashMap<Integer, BrowseFragment>(10);


        mPageTitles = new ArrayList<String>();
        mPageTitles.add(ctx.getString(R.string.my_videos));
        mPageTitles.add(ctx.getString(R.string.recommended_videos));
        mPageTitles.add(ctx.getString(R.string.latest_videos));
        mPageTitles.addAll(SemanticVideo.genreStrings.values());
    }

    /**
     * Get page from list of available pages. Create it if it doesn't exist yet.
     * @param i
     * @return
     */
    @Override
    public Fragment getItem(int i) {
        int key = mOrderedFragments.get(i);
        Log.i("BrowsePagerAdapter", "Getting page " + i + ", key " + key);
        BrowseFragment frag = mFragments.get(key);
        if (frag == null) {
            frag = new BrowseFragment();
            Bundle args = new Bundle();
            args.putInt("page_id", key);
            // 'query' helps page to remember its search arguments in pause/resume situations and
            // allows putting extra arguments
            // 'query_type' helps to distinguish which kind of search to send.
            if (key >= BROWSE_BY_GENRE && key <= BROWSE_BY_GENRE + SemanticVideo.Genre.values()
                    .length) {
                args.putInt("query_type", BROWSE_BY_GENRE);
                int genre_i = key - BROWSE_BY_GENRE;
                String q = SemanticVideo.englishGenreStrings.get(SemanticVideo.Genre.values()[genre_i]);
                args.putString("query", q);
                Log.i("BrowsePagerAdapter", "Constructed genre browsing fragment with query " + q);
            } else {
                args.putInt("query_type", key);
                args.putString("query", "");
            }
            frag.setArguments(args);
            mFragments.put(key, frag);
        }
        return frag;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        Log.i("BrowsePagerAdapter", "SetPrimaryItem called with position " + position);
        mCurrentIndex = position;
    }

    public int getCurrentPageId() {
        return mOrderedFragments.get(mCurrentIndex);
    }

    @Override
    public int getCount() {
        return mOrderedFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int i) {
        int page_id = mOrderedFragments.get(i);
        switch(page_id) {
            case MY_VIDEOS:
                return mContext.getString(R.string.my_videos);
            case RECOMMENDED:
                return mContext.getString(R.string.recommended_videos);
            case LATEST:
                return mContext.getString(R.string.latest_videos);
            case NEARBY:
                return mContext.getString(R.string.nearby_videos);
            case SEARCH:
                return mContext.getString(R.string.search_results);
            case QR_SEARCH:
                return mContext.getString(R.string.search_results);
            default:
                if (page_id >= BROWSE_BY_GENRE && page_id <= BROWSE_BY_GENRE +
                    SemanticVideo.Genre.values().length) {
                    int genre_i = page_id - BROWSE_BY_GENRE;
                    return SemanticVideo.genreStrings.get(SemanticVideo.Genre.values()[genre_i]);
                } else {
                    return "what page?";
                }
        }
    }
}
