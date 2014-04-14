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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.HashMap;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.LoginActivity;
import fi.aalto.legroup.achso.activity.VideoBrowserActivity;
import fi.aalto.legroup.achso.adapter.ImageAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.state.IntentDataHolder;
import fi.aalto.legroup.achso.upload.UploaderService;
import fi.aalto.legroup.achso.util.App;
import fi.google.zxing.integration.android.IntentIntegrator;

public class BrowseFragment extends Fragment implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, ActionMode.Callback, AbsListView.MultiChoiceModeListener {

    protected static final String CONTAINER_STATE = "containerState";
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id) {
        }

        public void onRemoteItemSelected(int positionInCache) {
        }
    };
    Callbacks mCallbacks = sDummyCallbacks;
    protected ProgressBar mSearchProgress;
    protected Parcelable mContainerState;
    protected String mSortBy;
    protected GridView mGrid;
    protected ListView mList;
    protected boolean mUsesGrid;
    int mPage = -1;
    String mQuery = "";
    int mQueryType = 0;
    private HashMap<Integer, SemanticVideo> mSelectedVideos;
    private ActionMode mActionMode = null;

    public BrowseFragment() {
        mSortBy = VideoDBHelper.KEY_CREATED_AT;
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
                                refresh();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
                return true;
            case R.id.action_upload:
                if (App.login_state.isIn()) {
                    final Context ctx = this.getActivity();
                    new AlertDialog.Builder(this.getActivity()).setTitle(R.string.not_loggedin_nag_title)
                            .setMessage(R.string.not_loggedin_nag_text)
                            .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(ctx, LoginActivity.class));
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
                        sv.setUploadPending(true);
                        Intent uploadIntent = new Intent(getActivity(), UploaderService.class);
                        uploadIntent.putExtra(UploaderService.PARAM_IN, sv.getId());
                        getActivity().startService(uploadIntent);
                    }
                    refresh();
                    mSelectedVideos.clear();
                    mode.finish();
                }
                return true;
            case R.id.action_qr_to_video:
                ((VideoBrowserActivity) getActivity()).setSelectedVideosForQrCode(mSelectedVideos);
                IntentIntegrator integrator = new IntentIntegrator(getActivity());
                IntentDataHolder.From = SemanticVideoPlayerFragment.class;
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
        SemanticVideo sv = (SemanticVideo) getListAdapter().getItem(position);
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
        mCallbacks.onItemSelected(VideoDBHelper.getByPosition(position).getId());
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
            mPage = args.getInt("page");
            mQuery = args.getString("query");
            mQueryType = args.getInt("query_type");
            mUsesGrid = args.getBoolean("usesGrid");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (mUsesGrid) {
            view = inflater.inflate(R.layout.browse_results_grid, null);
            assert (view != null);
            mGrid = (GridView) view.findViewById(R.id.main_menu_grid);
            if (mGrid != null) {
                mGrid.setOnItemClickListener(this);
                mGrid.setOnItemLongClickListener(this);
                mGrid.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
                mGrid.setMultiChoiceModeListener(this);
            }
        } else {
            view = inflater.inflate(R.layout.browse_results_list, null);
            assert (view != null);
            mList = (ListView) view.findViewById(R.id.browse_list);
            if (mList != null) {
                mList.setOnItemClickListener(this);
                mList.setOnItemLongClickListener(this);
                mList.setMultiChoiceModeListener(this);
            }
        }
        if (view != null) {
            mSearchProgress = (ProgressBar) view.findViewById(R.id.search_progress);
            LinearLayout urlArea = (LinearLayout) view.findViewById(R.id.url_found_area);
            urlArea.setVisibility(LinearLayout.GONE);
        }

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
        refresh();
        if (mContainerState != null) {
            if (mUsesGrid) {
                mGrid.onRestoreInstanceState(mContainerState);
            } else {
                mList.onRestoreInstanceState(mContainerState);
            }
        }
        mContainerState = null;
    }

    public ListAdapter getListAdapter() {
        if (mUsesGrid) {
            return mGrid.getAdapter();
        } else {
            return mList.getAdapter();
        }
    }

    public void refresh() {
        if (mUsesGrid) {
            if (mGrid != null) {
                mGrid.invalidateViews();
                ImageAdapter adapter = new ImageAdapter(getActivity(), VideoDBHelper.getVideoCache(mPage));
                mGrid.setAdapter(adapter);
                ((ImageAdapter) mGrid.getAdapter()).notifyDataSetChanged();
                adapter.notifyDataSetChanged();
            }
        } else {
            if (mList != null) {
                mList.invalidateViews();
                mList.setAdapter(new ImageAdapter(getActivity(), VideoDBHelper.getVideoCache(mPage)));
                ((ArrayAdapter) mList.getAdapter()).notifyDataSetChanged();

            }
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mUsesGrid) {
            mContainerState = mGrid.onSaveInstanceState();
        } else {
            mContainerState = mList.onSaveInstanceState();
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

    public interface Callbacks {
        public void onItemSelected(long id);

        public void onRemoteItemSelected(int positionInCache);
    }

}
