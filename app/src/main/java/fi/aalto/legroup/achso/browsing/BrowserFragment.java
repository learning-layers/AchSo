package fi.aalto.legroup.achso.browsing;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.authoring.QRHelper;
import fi.aalto.legroup.achso.authoring.VideoHelper;
import fi.aalto.legroup.achso.playback.VideoPlayerActivity;
import fi.aalto.legroup.achso.storage.local.ExportCreatorTask;
import fi.aalto.legroup.achso.storage.remote.UploadErrorEvent;
import fi.aalto.legroup.achso.storage.remote.UploadService;
import fi.aalto.legroup.achso.storage.remote.UploadStateEvent;
import fi.aalto.legroup.achso.views.RecyclerItemClickListener;
import fi.aalto.legroup.achso.views.adapters.VideoGridAdapter;
import fi.aalto.legroup.achso.views.utilities.DimensionUnits;

public class BrowserFragment extends Fragment implements ActionMode.Callback,
        RecyclerItemClickListener.OnItemClickListener {

    private List<UUID> videos = Collections.emptyList();

    private RecyclerView grid;
    private TextView placeHolder;

    private VideoGridAdapter adapter;
    private GridLayoutManager layoutManager;
    private ActionMode actionMode;

    public static BrowserFragment newInstance(List<UUID> videos) {
        BrowserFragment fragment = new BrowserFragment();

        fragment.setVideos(videos);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FIXME: Retaining the instance just to keep the videos is a bit questionable
        setRetainInstance(true);

        // TODO: Inject instead
        Bus bus = App.bus;
        bus.register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedState) {
        return inflater.inflate(R.layout.fragment_video_browser, parent, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.placeHolder = (TextView) view.findViewById(R.id.place_holder);
        this.grid = (RecyclerView) view.findViewById(R.id.video_list);

        // Default span count is 1, will be recalculated with the layout
        this.layoutManager = new GridLayoutManager(getActivity(), 1);

        this.adapter = new VideoGridAdapter(getActivity(), App.videoInfoRepository);
        this.adapter.registerAdapterDataObserver(new PlaceholderDataObserver());
        this.adapter.setItems(videos);

        this.grid.setHasFixedSize(true);
        this.grid.setAdapter(this.adapter);
        this.grid.setLayoutManager(this.layoutManager);
        this.grid.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), this));
        this.grid.getViewTreeObserver().addOnGlobalLayoutListener(new GridOnLayoutChangeListener());
    }

    @Override
    public void onPause() {
        if (this.actionMode != null) {
            this.actionMode.finish();
        }

        super.onPause();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.video_context_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_qr_to_video:
                QRHelper.readQRCodeForVideos(getActivity(), getSelection(), mode);
                mode.finish();
                return true;

            case R.id.action_share_video:
                List<UUID> list = this.getSelection();
                new ExportCreatorTask(this.getActivity()).execute(list.toArray(new UUID[list.size()]));
                mode.finish();
                return true;

            case R.id.action_delete:
                VideoHelper.deleteVideos(getActivity(), getSelection(), mode);
                mode.finish();
                return true;

            case R.id.action_upload:
                UploadService.upload(getActivity(), getSelection());
                mode.finish();
                return true;

            case R.id.action_view_video_info:
                Intent informationIntent = new Intent(getActivity(), DetailActivity.class);
                informationIntent.putExtra(DetailActivity.ARG_VIDEO_ID, getSelection().get(0));
                startActivity(informationIntent);
                mode.finish();
                return true;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.actionMode = null;
        clearSelection();
    }

    @Override
    public void onItemClick(View view, int position) {
        if (actionMode == null) {
            showVideo(position);
        } else {
            toggleSelection(position);
        }
    }

    @Override
    public void onItemLongPress(View view, int position) {
        if (actionMode == null) {
            startActionMode();
        }

        toggleSelection(position);
    }

    /**
     * FIXME: Juggling the videos like this is a bit icky but better than creating an adapter here.
     */
    public void setVideos(List<UUID> videos) {
        this.videos = videos;

        if (this.adapter != null) {
            this.adapter.setItems(videos);
        }
    }

    @Subscribe
    public void onUploadState(UploadStateEvent event) {
        UUID videoId = event.getVideoId();

        switch (event.getType()) {
            case STARTED:
                this.adapter.showProgress(videoId);
                break;

            case FINISHED:
                this.adapter.hideProgress(videoId);
                break;
        }
    }

    @Subscribe
    public void onUploadError(UploadErrorEvent event) {
        UUID videoId = event.getVideoId();
        String message = event.getErrorMessage();

        this.adapter.hideProgress(videoId);

        if (event.getErrorMessage() == null) {
            message = "Uploading failed, please try again.";
        }

        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private List<UUID> getSelection() {
        List<Integer> positions = this.adapter.getSelectedItems();
        List<UUID> items = new ArrayList<>();

        for (int position : positions) {
            UUID item = this.adapter.getItem(position);
            items.add(item);
        }

        return items;
    }

    private void clearSelection() {
        this.adapter.clearSelectedItems();
    }

    private void toggleSelection(int position) {
        this.adapter.toggleSelection(position);

        int count = this.adapter.getSelectedItemCount();

        if (count == 0) {
            this.actionMode.finish();
        } else {
            String title = getResources().getQuantityString(R.plurals.select_count, count, count);
            this.actionMode.setTitle(title);
        }
    }

    private void showVideo(int position) {
        Intent detailIntent = new Intent(getActivity(), VideoPlayerActivity.class);
        UUID id = this.adapter.getItem(position);

        detailIntent.putExtra(VideoPlayerActivity.ARG_VIDEO_ID, id);

        startActivity(detailIntent);
    }

    private void startActionMode() {
        this.actionMode = getActivity().startActionMode(this);
    }

    /**
     * Shows or hides the placeholder text appropriately when the adapter items change.
     */
    private final class PlaceholderDataObserver extends RecyclerView.AdapterDataObserver {

        @Override
        public void onChanged() {
            super.onChanged();

            int itemCount = BrowserFragment.this.adapter.getItemCount();
            View placeHolder = BrowserFragment.this.placeHolder;

            if (itemCount == 0) {
                placeHolder.setVisibility(View.VISIBLE);
            } else {
                placeHolder.setVisibility(View.GONE);
            }
        }

    }

    /**
     * Calculates the number of columns when the grid's layout bounds change.
     */
    private final class GridOnLayoutChangeListener implements ViewTreeObserver.OnGlobalLayoutListener {

        private static final int MINIMUM_ITEM_WIDTH_DP = 250;

        @Override
        public void onGlobalLayout() {
            int gridWidth = grid.getWidth();
            float itemWidth = DimensionUnits.dpToPx(grid.getContext(), MINIMUM_ITEM_WIDTH_DP);

            int spanCount = (int) Math.floor(gridWidth / itemWidth);

            if (spanCount < 1) {
                spanCount = 1;
            }
            layoutManager.setSpanCount(spanCount);
        }

    }

}