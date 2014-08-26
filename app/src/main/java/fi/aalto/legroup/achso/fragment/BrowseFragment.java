/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.ActionbarActivity;
import fi.aalto.legroup.achso.activity.OldLoginActivity;
import fi.aalto.legroup.achso.activity.VideoBrowserActivity;
import fi.aalto.legroup.achso.adapter.BrowsePagerAdapter;
import fi.aalto.legroup.achso.adapter.VideoThumbAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.service.UploaderService;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.remote.RemoteFetchTask;
import fi.aalto.legroup.achso.remote.RemoteResultCache;
import com.google.zxing.integration.android.IntentIntegrator;

import static fi.aalto.legroup.achso.util.App.addPollingReminder;
import static fi.aalto.legroup.achso.util.App.doPendingPolls;


public class BrowseFragment extends Fragment implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, ActionMode.Callback, AbsListView.MultiChoiceModeListener {

    protected static final String CONTAINER_STATE = "containerState";
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onLocalItemSelected(long id) {
        }

        public void onRemoteItemSelected(int positionInCache, SemanticVideo sv) {
        }
    };
    Callbacks mCallbacks = sDummyCallbacks;
    protected ProgressBar mRemoteProgress;
    protected Parcelable mContainerState;
    protected String mSortBy;
    protected GridView mVideoGrid;
    protected ListView mVideoList;
    protected boolean mUsesGrid;
    int mPage = -1;
    String mQuery = "";
    int mQueryType = 0;
    private HashMap<Integer, SemanticVideo> mSelectedVideos;
    private ActionMode mActionMode = null;
    private AsyncTask<String, Double, List<SemanticVideo>> mFetchTask;
    private int mSeparatorPosition;
    private TextView mUrl;
    private LinearLayout mUrlArea;
    private TextView mUrlLabel;
    private TextView mNoConnectionMessage;
    private boolean mQrQuery;

    public BrowseFragment() {

        mSortBy = VideoDBHelper.KEY_CREATED_AT;
        mFetchTask = null;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater mi = mode.getMenuInflater();
        if (mi != null) {
            mi.inflate(R.menu.video_context_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                new AlertDialog.Builder(this.getActivity()).setTitle(R.string.deletion_title)
                        .setMessage(R.string.deletion_question)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                VideoDBHelper vdb = new VideoDBHelper(getActivity());
                                for (SemanticVideo sv : mSelectedVideos.values()) {
                                    vdb.delete(sv);
                                }
                                vdb.close();
                                mSelectedVideos.clear();
                                refreshLocalVideos();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
                return true;
            case R.id.action_upload:
                if (!App.login_state.isIn()) {
                    final Context ctx = this.getActivity();
                    new AlertDialog.Builder(this.getActivity()).setTitle(R.string.not_loggedin_nag_title)
                            .setMessage(R.string.not_loggedin_nag_text)
                            .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(ctx, OldLoginActivity.class));
                                    dialog.dismiss();
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
                } else {
                    for (int pos : mSelectedVideos.keySet()) {
                        SemanticVideo sv = mSelectedVideos.get(pos);
                        sv.setUploadStatus(SemanticVideo.UPLOAD_PENDING);
                        Intent uploadIntent = new Intent(getActivity(), UploaderService.class);
                        uploadIntent.putExtra(UploaderService.PARAM_IN, sv.getId());
                        if (App.allow_upload) {
                            getActivity().startService(uploadIntent);
                        } else {
                            Toast.makeText(App.getContext(), "Uploading is temporarily switched off. It " +
                                    "will be enabled in future version.",  Toast.LENGTH_LONG).show();
                        }
                    }
                    refreshLocalVideos();
                    mSelectedVideos.clear();
                    mode.finish();
                }
                return true;
            case R.id.action_qr_to_video:
                ((VideoBrowserActivity) getActivity()).setSelectedVideosForQrCode(mSelectedVideos);
                IntentIntegrator integrator = new IntentIntegrator(getActivity());
                App.setQrMode(App.ATTACH_QR);
                integrator.initiateScan(IntentIntegrator.ALL_CODE_TYPES);
                mSelectedVideos.clear();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mActionMode = mode;
        SemanticVideo sv = (SemanticVideo) getVideoAdapter().getItem(position);
        if (checked) {
            mSelectedVideos.put(position, sv);
        } else {
            mSelectedVideos.remove(sv);
        }
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        mSelectedVideos.clear();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        view.setSelected(true);
        SemanticVideo sv = (SemanticVideo) getVideoAdapter().getItem(position);
        if (sv != null) {
            if (sv.inLocalDB()) {
                mCallbacks.onLocalItemSelected(sv.getId());
            } else {
                if (sv.getRemoteVideo() != null && !sv.getRemoteVideo().isEmpty()) {
                    mCallbacks.onRemoteItemSelected(position, sv);
                    RemoteResultCache.setSelectedVideo(sv);
                } else {
                    Log.i("BrowseFragment", "Launching polling intent");
                    addPollingReminder(sv.getKey(), "testuser");
                    doPendingPolls();

                }
            }
        } else if (getVideoAdapter().getItemViewType(position) == VideoThumbAdapter
                    .ITEM_TYPE_RECORD_BUTTON) {
            ActionbarActivity activity = (ActionbarActivity) getActivity();
            activity.launchRecording();
        }
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // Needed for AsyncTask to survive orientation/activity change


        mSelectedVideos = new HashMap<Integer, SemanticVideo>();
        if (VideoDBHelper.getVideoCache() == null) {
            VideoDBHelper vdb = new VideoDBHelper(getActivity());
            vdb.updateVideoCache(mSortBy, true);
            vdb.close();
        }
        if (getArguments() != null) {
            Bundle args = getArguments();
            mPage = args.getInt("page_id");
            mQuery = args.getString("query");
            mQueryType = args.getInt("query_type");
        }
        mUsesGrid = App.isTablet();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (mUsesGrid) {
            view = inflater.inflate(R.layout.browse_results_grid, null);
            assert (view != null);
            mVideoGrid = (GridView) view.findViewById(R.id.video_grid);
            if (mVideoGrid != null) {
                mVideoGrid.setOnItemClickListener(this);
                mVideoGrid.setOnItemLongClickListener(this);
                mVideoGrid.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
                mVideoGrid.setMultiChoiceModeListener(this);
            }
        } else {
            view = inflater.inflate(R.layout.browse_results_list, null);
            assert (view != null);
            mVideoList = (ListView) view.findViewById(R.id.local_video_list);
            if (mVideoList != null) {
                mVideoList.setOnItemClickListener(this);
                mVideoList.setOnItemLongClickListener(this);
                mVideoList.setMultiChoiceModeListener(this);
            }
        }
        mRemoteProgress = (ProgressBar) view.findViewById(R.id.remote_progress);
        mRemoteProgress.setVisibility(LinearLayout.GONE);
        mUrl = (TextView) view.findViewById(R.id.url_found);
        mUrlArea = (LinearLayout) view.findViewById(R.id.url_found_area);
        mUrlLabel = (TextView) view.findViewById(R.id.url_found_label);
        mUrlArea.setVisibility(LinearLayout.GONE);
        mNoConnectionMessage = (TextView) view.findViewById(R.id.no_connection_message);
        mNoConnectionMessage.setVisibility(LinearLayout.GONE);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mQueryType == BrowsePagerAdapter.SEARCH) {
            VideoBrowserActivity vba = (VideoBrowserActivity) getActivity();
            mQuery = vba.getQueryString();
            if (vba.isQrSearch()) {
                mUrlArea.setVisibility(LinearLayout.VISIBLE);
                mUrl.setText(mQuery);
                mQrQuery = true;
                try {
                    URL url = new URL(mQuery);
                    mUrlLabel.setText(R.string.url_found_label);
                } catch (MalformedURLException e) {
                    mUrlLabel.setText(R.string.code_found_label);
                }
            } else {
                mQrQuery = false;
            }
        }

        refreshLocalVideos();
        refreshRemoteVideos();
        if (mContainerState != null) {
            if (mUsesGrid) {
                mVideoGrid.onRestoreInstanceState(mContainerState);
            } else {
                mVideoList.onRestoreInstanceState(mContainerState);
            }
        }
        mContainerState = null;


    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFetchTask != null) {
            mFetchTask.cancel(true);
            mFetchTask = null;
        }
    }

    public ListAdapter getVideoAdapter() {
        if (mUsesGrid) {
            return mVideoGrid.getAdapter();
        } else {
            return mVideoList.getAdapter();
        }
    }

    /**
     * Clean adapter data and reload it -- depends on VideoDBHelper if the list is really up to
     * date and not cached old list.
     */
    public void refreshLocalVideos() {
        VideoThumbAdapter va = (VideoThumbAdapter) getVideoAdapter();
        if (va == null) {
            va = new VideoThumbAdapter(getActivity(), new ArrayList<SemanticVideo>(), mQueryType);
            if (mUsesGrid) {
                if (mVideoGrid != null) {
                    mVideoGrid.invalidateViews();
                    mVideoGrid.setAdapter(va);
                }
            } else {
                if (mVideoList != null) {
                    mVideoList.invalidateViews();
                    mVideoList.setAdapter(va);
                }
            }
        } else if (mUsesGrid && mVideoGrid != null) {
                    mVideoGrid.invalidateViews();
            } else if (mVideoList != null) {
                    mVideoList.invalidateViews();
            }
        va.clear();
        va.updateLocalVideos(getLocalVideos());
        va.notifyDataSetChanged();
    }

    public void refreshRemoteVideos() {
        VideoThumbAdapter va = (VideoThumbAdapter) getVideoAdapter();
        if (va == null) {
            va = new VideoThumbAdapter(getActivity(), new ArrayList<SemanticVideo>(), mQueryType);
            if (mUsesGrid) {
                if (mVideoGrid != null) {
                    mVideoGrid.invalidateViews();
                    mVideoGrid.setAdapter(va);
                }
            } else {
                if (mVideoList != null) {
                    mVideoList.invalidateViews();
                    mVideoList.setAdapter(va);
                }
            }
        } else if (mUsesGrid && mVideoGrid != null) {
            mVideoGrid.invalidateViews();
        } else if (mVideoList != null) {
            mVideoList.invalidateViews();
        }
        va.clear();
        va.updateRemoteVideos(getRemoteVideos());
        va.notifyDataSetChanged();
    }

    private List<SemanticVideo> getLocalVideos() {
        switch (mQueryType) {
            case BrowsePagerAdapter.SEARCH:
                if (mQrQuery) {
                    return VideoDBHelper.getVideosByQrCode(mQuery);
                } else {
                    return VideoDBHelper.queryVideoCacheByTitle(mQuery);
                }
            case BrowsePagerAdapter.MY_VIDEOS:
                return VideoDBHelper.getVideoCache();
            case BrowsePagerAdapter.RECOMMENDED:
                return Collections.<SemanticVideo>emptyList();
            case BrowsePagerAdapter.EMPTY:
                return Collections.<SemanticVideo>emptyList();
            case BrowsePagerAdapter.LATEST:
                return Collections.<SemanticVideo>emptyList();
            case BrowsePagerAdapter.BROWSE_BY_GENRE:
                return VideoDBHelper.getVideosByGenre(mQuery);
            default:
                Log.i("BrowseFragment", "Unknown query type");
                return Collections.<SemanticVideo>emptyList();
        }
    }

    private void startRemoteVideoFetch() {
        if (App.hasConnection() && (App.login_state.isIn() || App.login_state.isTrying())) {
            mNoConnectionMessage.setVisibility(LinearLayout.GONE);
            mRemoteProgress.setVisibility(LinearLayout.VISIBLE);

            if (mFetchTask != null) {
                mFetchTask.cancel(true);
            }
            mFetchTask = new RemoteFetchTask(this, mRemoteProgress, mPage);
            mFetchTask.execute(mQuery); // Fetch and populate remote grid
        } else {
            if (!App.isCandybar()) {
                mNoConnectionMessage.setVisibility(LinearLayout.VISIBLE);
            }

            VideoThumbAdapter va = (VideoThumbAdapter) getVideoAdapter();

            va.clearRemoteVideos();
            va.notifyDataSetChanged();
        }
    }
    public void finishRemoteVideoFetch(List<SemanticVideo> result_list) {
        VideoThumbAdapter va = (VideoThumbAdapter) getVideoAdapter();
        va.updateRemoteVideos(result_list);
        va.notifyDataSetChanged();
    }

    private List<SemanticVideo> getRemoteVideos() {
        List<SemanticVideo> list;
        if (mQueryType == BrowsePagerAdapter.EMPTY) {
            return Collections.<SemanticVideo>emptyList();
        }
        if (RemoteResultCache.hasCached(mPage)) {
            list = RemoteResultCache.getCached(mPage);
        } else {
            startRemoteVideoFetch(); // remember to cache the result
            list = Collections.<SemanticVideo>emptyList();
        }
        return list;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mUsesGrid) {
            mContainerState = mVideoGrid.onSaveInstanceState();
        } else {
            mContainerState = mVideoList.onSaveInstanceState();
        }
        outState.putParcelable(CONTAINER_STATE, mContainerState);
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);
        if (state != null) {
            mContainerState = state.getParcelable(CONTAINER_STATE);
        }
    }

    public Pair<ProgressBar, ImageView> getUiElementsFor(SemanticVideo sv) {
        if (!this.isAdded())
            return null;
        ListAdapter la = getVideoAdapter();
        int listitemcount = la.getCount();
        int pos = 0;
        for (int i = 0; i < listitemcount; ++i) {
            if (la.getItem(i) == sv) {
                pos = i;
                break;
            }
        }
        View v;
        if (App.isTablet()) {
            v = mVideoGrid.getChildAt(pos);
        } else {
            v = mVideoList.getChildAt(pos);
        }
        if (v == null)
            return null;
        ProgressBar pb = (ProgressBar) v.findViewById(R.id.upload_progress);
        ImageView uploadIcon = (ImageView) v.findViewById(R.id.upload_icon);

        return new Pair<ProgressBar, ImageView>(pb, uploadIcon);
    }

    public String getQuery() {
        return mQuery;
    }

    public int getQueryType() {
        return mQueryType;
    }

    public interface Callbacks {
        public void onLocalItemSelected(long id);

        public void onRemoteItemSelected(int positionInCache, SemanticVideo sv);

    }

}




/*
    public List<SemanticVideo> query(String mQuery) {
        List<SemanticVideo> result = null;
        if (mQueryType == TITLE_QUERY) {
            result = VideoDBHelper.queryVideoCacheByTitle(mQuery);
        } else if (mQueryType == QR_QUERY) {
            result = VideoDBHelper.getVideosByQrCode(mQuery);
        }
        return result;
    }

    public void refreshLocalVideos() {
        Log.i("SearchGridFragment", "Refreshing search results");

        if (mUsesGrid) {
            if (mVideoGrid != null) {
                mVideoGrid.invalidateViews();
                ImageAdapter local_adapter = new ImageAdapter(getActivity(), query(mQuery));
                mVideoGrid.setAdapter(local_adapter);
                ((ArrayAdapter) mVideoGrid.getAdapter()).notifyDataSetChanged();

                ImageAdapter remote_adapter;
                if (SearchResultCache.getLastSearch() != null) {
                    Log.i("SearchGridFragment", "Getting last results from cache");
                    mRemoteProgress.setVisibility(View.GONE);
                    remote_adapter = new ImageAdapter(getActivity(), SearchResultCache.getLastSearch());
                    mRemoteGrid.setAdapter(remote_adapter);
                    remote_adapter.notifyDataSetChanged();
                } else if (App.hasConnection()) {
                    Log.i("SearchGridFragment", "Has connection, trying remote search");
                    if (mFetchTask != null) {
                        mFetchTask.cancel(true);
                    }
                    mFetchTask = new MultimediaOperationsTask(getActivity(), mRemoteGrid, mRemoteProgress, true);
                    mFetchTask.execute(mQuery); // Fetch and populate remote grid
                } else {
                    Log.i("SearchGridFragment", "No connection, showing empty list");
                    remote_adapter = new ImageAdapter(getActivity(), Collections.<SemanticVideo>emptyList());
                    mRemoteGrid.setAdapter(remote_adapter);
                    remote_adapter.notifyDataSetChanged();
                }
            }
        } else {
            if (mVideoList != null) {
                mVideoList.invalidateViews();
                if (mPage != -1) {
                    mVideoList.setAdapter(new ImageAdapter(getActivity(), VideoDBHelper.getVideoCache(mPage)));
                } else {
                    List<SemanticVideo> videos = query(mQuery);
                    if (SearchResultCache.getLastSearch() == null) {
                        videos.add(null);
                        mSeparatorPosition = videos.size();

                        if (mFetchTask != null) {
                            mFetchTask.cancel(true);
                        }
                        if (App.hasConnection()) {
                            mFetchTask = new MultimediaOperationsTask(getActivity(), mVideoList, mRemoteProgress, false);
                            mFetchTask.execute(mQuery); // Fetch and populate remote grid
                        }
                    } else {
                        mRemoteProgress.setVisibility(View.GONE);
                    }
                    mVideoList.setAdapter(new ImageAdapter(getActivity(), videos));
                }
                ((ArrayAdapter) mVideoList.getAdapter()).notifyDataSetChanged();
            }
        }


        if (mQueryType == QR_QUERY) {
            mUrlArea.setVisibility(LinearLayout.VISIBLE);
            mUrl.setText(mQuery);
            try {
                URL url = new URL(mQuery);
                mUrlLabel.setText(R.string.url_found_label);
            } catch (MalformedURLException e) {
                mUrlLabel.setText(R.string.code_found_label);
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
            video_id = ((SemanticVideo) getLocalAdapter().getItem(position)).getId();
        } else {
            if (position >= mSeparatorPosition) {
                mCallbacks.onRemoteItemSelected(position - 1); // Remove separator from position
                return;
            }
            video_id = VideoDBHelper.getByPosition(position).getId();
        }
        mCallbacks.onLocalItemSelected(video_id);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (mUsesGrid) {
            view = inflater.inflate(R.layout.browse_results_grid, null);
            mRemoteGrid = (ExpandableGridView) view.findViewById(R.id.remote_video_grid);
            mRemoteGrid.setExpanded(true);
            mRemoteGrid.setOnItemClickListener(this);
            ExpandableGridView mVideoGrid = (ExpandableGridView) view.findViewById(R.id.local_video_grid);
            mVideoGrid.setExpanded(true);

            this.mVideoGrid = mVideoGrid;
            this.mVideoGrid.setOnItemClickListener(this);
            this.mVideoGrid.setOnItemLongClickListener(this);
            this.mVideoGrid.setMultiChoiceModeListener(this);
        } else {
            view = super.onCreateView(inflater, container, savedInstanceState);

        }
        if (view != null) {
            mRemoteProgress = (ProgressBar) view.findViewById(R.id.search_progress);
            mUrl = (TextView) view.findViewById(R.id.url_found);
            mUrlArea = (LinearLayout) view.findViewById(R.id.url_found_area);
            mUrlLabel = (TextView) view.findViewById(R.id.url_found_label);
            mUrlArea.setVisibility(LinearLayout.GONE);
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

 */

/*
        ImageAdapter remote_adapter;
        if (SearchResultCache.getLastSearch() != null) {
            Log.i("SearchGridFragment", "Getting last results from cache");
            mRemoteProgress.setVisibility(View.GONE);
            remote_adapter = new ImageAdapter(getActivity(), SearchResultCache.getLastSearch());
            mRemoteGrid.setAdapter(remote_adapter);
            remote_adapter.notifyDataSetChanged();
        } else if (App.hasConnection()) {
            Log.i("SearchGridFragment", "Has connection, trying remote search");
            if (mFetchTask != null) {
                mFetchTask.cancel(true);
            }
            mFetchTask = new MultimediaOperationsTask(getActivity(), mRemoteGrid, mRemoteProgress, true);
            mFetchTask.execute(mQuery); // Fetch and populate remote grid
        } else {
            Log.i("SearchGridFragment", "No connection, showing empty list");
            remote_adapter = new ImageAdapter(getActivity(), Collections.<SemanticVideo>emptyList());
            mRemoteGrid.setAdapter(remote_adapter);
            remote_adapter.notifyDataSetChanged();
        }


    }

        Log.i("SearchGridFragment", "Refreshing search results");

        if (mUsesGrid) {
            if (mVideoGrid != null) {
                mVideoGrid.invalidateViews();
                ImageAdapter local_adapter = new ImageAdapter(getActivity(), query(mQuery));
                mVideoGrid.setAdapter(local_adapter);
                ((ArrayAdapter) mVideoGrid.getAdapter()).notifyDataSetChanged();

            }
        } else {
            if (mVideoList != null) {
                mVideoList.invalidateViews();
                if (mPage != -1) {
                    mVideoList.setAdapter(new ImageAdapter(getActivity(), VideoDBHelper.getVideoCache(mPage)));
                } else {
                    List<SemanticVideo> videos = query(mQuery);
                    if (SearchResultCache.getLastSearch() == null) {
                        videos.add(null);
                        mSeparatorPosition = videos.size();

                        if (mFetchTask != null) {
                            mFetchTask.cancel(true);
                        }
                        if (App.hasConnection()) {
                            mFetchTask = new MultimediaOperationsTask(getActivity(), mVideoList, mRemoteProgress, false);
                            mFetchTask.execute(mQuery); // Fetch and populate remote grid
                        }
                    } else {
                        mRemoteProgress.setVisibility(View.GONE);
                    }
                    mVideoList.setAdapter(new ImageAdapter(getActivity(), videos));
                }
                ((ArrayAdapter) mVideoList.getAdapter()).notifyDataSetChanged();
            }
        }


        if (mQueryType == QR_QUERY) {
            mUrlArea.setVisibility(LinearLayout.VISIBLE);
            mUrl.setText(mQuery);
            try {
                URL url = new URL(mQuery);
                mUrlLabel.setText(R.string.url_found_label);
            } catch (MalformedURLException e) {
                mUrlLabel.setText(R.string.code_found_label);
            }
        }


    }

*/
