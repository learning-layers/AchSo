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

import fi.aalto.legroup.achso.activity.VideoBrowserActivity;
import fi.aalto.legroup.achso.fragment.BrowseFragment;
import fi.aalto.legroup.achso.fragment.SearchFragment;

public class SearchPagerAdapter extends FragmentStatePagerAdapter {

    private Context mContext;
    private BrowseFragment[] mFragments;
    private String mQuery;
    private boolean mIsTitleQuery;

    public SearchPagerAdapter(Context ctx, FragmentManager fm, String query, boolean istitlequery) {
        super(fm);
        mContext = ctx;
        mFragments = new BrowseFragment[1];
        mQuery = query;
        mIsTitleQuery = istitlequery;
    }

    @Override
    public Fragment getItem(int i) {
        if (mFragments[i] == null) {
            SearchFragment ret = new SearchFragment();
            Bundle args = new Bundle();
            args.putString("TAG", "items");
            args.putInt("page", -1);
            args.putString("query", mQuery);
            args.putInt("query_type", (mIsTitleQuery) ? SearchFragment.TITLE_QUERY : SearchFragment.QR_QUERY);
            args.putBoolean("usesGrid", VideoBrowserActivity.isTablet(mContext));
            ret.setArguments(args);
            mFragments[i] = ret;
        }
        return mFragments[i];
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public CharSequence getPageTitle(int page) {
        return "Search: " + mQuery;
    }
}
