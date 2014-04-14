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

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.VideoBrowserActivity;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.fragment.BrowseFragment;
import fi.aalto.legroup.achso.fragment.SearchFragment;

public class BrowsePagerAdapter extends FragmentStatePagerAdapter {

    private Context mContext;
    private BrowseFragment[] mFragments;

    public BrowsePagerAdapter(Context ctx, FragmentManager fm) {
        super(fm);
        mContext = ctx;
        mFragments = new BrowseFragment[SemanticVideo.genreStrings.size() + 1];
    }

    @Override
    public Fragment getItem(int i) {
        if (mFragments[i] == null) {
            BrowseFragment ret = new BrowseFragment();
            Bundle args = new Bundle();
            args.putString("TAG", "items");
            args.putInt("page", i);
            args.putString("query", "");
            args.putInt("query_type", SearchFragment.TITLE_QUERY);
            args.putBoolean("usesGrid", VideoBrowserActivity.isTablet(mContext));
            ret.setArguments(args);
            mFragments[i] = ret;
        }
        return mFragments[i];
    }

    @Override
    public int getCount() {
        return SemanticVideo.genreStrings.size() + 1;
    }

    @Override
    public CharSequence getPageTitle(int page) {
        if (page == 0) return mContext.getResources().getString(R.string.first_page);
        else return SemanticVideo.genreStrings.get(SemanticVideo.Genre.values()[page - 1]);
    }
}
