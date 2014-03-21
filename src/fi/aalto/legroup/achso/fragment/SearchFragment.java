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

package fi.aalto.legroup.achso.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.adapter.ImageAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.MultimediaOperationsTask;
import fi.aalto.legroup.achso.util.SearchResultCache;
import fi.aalto.legroup.achso.view.ExpandableGridView;

public class SearchFragment extends BrowseFragment {

    public static final int TITLE_QUERY = 0;
    public static final int QR_QUERY = 1;
    private ExpandableGridView mRemoteGrid;
    private AsyncTask<String, Double, ArrayList<SemanticVideo>> mFetchTask;
    private int mSeparatorPosition;


    public SearchFragment() {
        mFetchTask = null;
    }

    public List<SemanticVideo> query(String mQuery) {
        List<SemanticVideo> result = null;
        if (mQueryType == TITLE_QUERY) {
            result = VideoDBHelper.queryVideoCacheByTitle(mQuery);
        } else if (mQueryType == QR_QUERY) {
            result = VideoDBHelper.queryVideoCacheByQrCode(mQuery);
        }
        return result;
    }

    public void refresh() {
        Log.i("SearchGridFragment", "Refreshing search results");

        if (mUsesGrid) {
            if (mGrid != null) {
                mGrid.invalidateViews();
                ImageAdapter local_adapter = new ImageAdapter(getActivity(), query(mQuery));
                mGrid.setAdapter(local_adapter);
                ((ArrayAdapter) mGrid.getAdapter()).notifyDataSetChanged();
                //local_adapter.notifyDataSetChanged();
                if (mGrid.getAdapter() != local_adapter) {
                    Log.e("SearchGridFragment", "set and get adapter differ");
                }

                ImageAdapter remote_adapter;
                if (SearchResultCache.getLastSearch() != null) {
                    Log.i("SearchGridFragment", "Getting last results from cache");
                    mSearchProgress.setVisibility(View.GONE);
                    remote_adapter = new ImageAdapter(getActivity(), SearchResultCache.getLastSearch());
                    mRemoteGrid.setAdapter(remote_adapter);
                    remote_adapter.notifyDataSetChanged();
                } else if (App.hasConnection()) {
                    Log.i("SearchGridFragment", "Has connection, trying remote search");
                    if (mFetchTask != null) {
                        mFetchTask.cancel(true);
                    }
                    mFetchTask = new MultimediaOperationsTask(getActivity(), mRemoteGrid, mSearchProgress, true);
                    mFetchTask.execute(mQuery); // Fetch and populate remote grid
                } else {
                    Log.i("SearchGridFragment", "No connection, showing empty list");
                    remote_adapter = new ImageAdapter(getActivity(), Collections.<SemanticVideo>emptyList());
                    mRemoteGrid.setAdapter(remote_adapter);
                    remote_adapter.notifyDataSetChanged();
                }
            }
        } else {
            if (mList != null) {
                mList.invalidateViews();
                if (mPage != -1) {
                    mList.setAdapter(new ImageAdapter(getActivity(), VideoDBHelper.getVideoCache(mPage)));
                } else {
                    List<SemanticVideo> videos = query(mQuery);
                    if (SearchResultCache.getLastSearch() == null) {
                        videos.add(null);
                        mSeparatorPosition = videos.size();

                        if (mFetchTask != null) {
                            mFetchTask.cancel(true);
                        }
                        if (App.hasConnection()) {
                            mFetchTask = new MultimediaOperationsTask(getActivity(), mList, mSearchProgress, false);
                            mFetchTask.execute(mQuery); // Fetch and populate remote grid
                        }
                    } else {
                        mSearchProgress.setVisibility(View.GONE);
                    }
                    mList.setAdapter(new ImageAdapter(getActivity(), videos));
                }
                ((ArrayAdapter) mList.getAdapter()).notifyDataSetChanged();
            }
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        view.setSelected(true);
        long video_id;
        if (mUsesGrid) {
            if (parent == mRemoteGrid) {
                mCallbacks.onRemoteItemSelected(position);
                return;
            }
            video_id = ((SemanticVideo) getListAdapter().getItem(position)).getId();
        } else {
            if (position >= mSeparatorPosition) {
                mCallbacks.onRemoteItemSelected(position - 1); // Remove separator from position
                return;
            }
            video_id = VideoDBHelper.getByPosition(position).getId();
        }
        mCallbacks.onItemSelected(video_id);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (mUsesGrid) {
            view = inflater.inflate(R.layout.search_results_grid, null);
            mRemoteGrid = (ExpandableGridView) view.findViewById(R.id.remote_video_grid);
            mRemoteGrid.setExpanded(true);
            mRemoteGrid.setOnItemClickListener(this);
            ExpandableGridView mLocalGrid = (ExpandableGridView) view.findViewById(R.id.main_menu_grid);
            mLocalGrid.setExpanded(true);

            mGrid = mLocalGrid;
            mGrid.setOnItemClickListener(this);
            mGrid.setOnItemLongClickListener(this);
            mGrid.setMultiChoiceModeListener(this);
        } else {
            view = super.onCreateView(inflater, container, savedInstanceState);

        }
        if (view != null) {
            mSearchProgress = (ProgressBar) view.findViewById(R.id.search_progress);
        }
        return view;
    }

    public void onPause() {
        super.onPause();
        if (mFetchTask != null) {
            mFetchTask.cancel(true);
            mFetchTask = null;
        }
    }
}
