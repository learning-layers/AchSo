package fi.aalto.legroup.achso.views.adapters;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.OptimizedVideo;
import fi.aalto.legroup.achso.storage.VideoInfoRepository;

public final class VideoGridAdapter extends RecyclerView.Adapter<VideoGridAdapter.ViewHolder> {

    private Context context;
    private LayoutInflater inflater;
    private VideoInfoRepository repository;

    private List<UUID> videoIds = Collections.emptyList();

    private List<Integer> selectedItems = new ArrayList<>();
    private List<Integer> itemsInProgress = new ArrayList<>();

    public VideoGridAdapter(Context context, VideoInfoRepository repository) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.repository = repository;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        View view = this.inflater.inflate(R.layout.item_browser_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        OptimizedVideo video;

        ProgressBar progressBar = holder.getProgressBar();
        View selectionOverlay = holder.getSelectionOverlay();

        if (isInProgress(position)) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }

        if (isSelected(position)) {
            selectionOverlay.setVisibility(View.VISIBLE);
        } else {
            selectionOverlay.setVisibility(View.GONE);
        }

        try {
            UUID id = this.videoIds.get(position);
            video = this.repository.getVideo(id);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        holder.getTitleText().setText(video.getTitle());

        Uri thumbUri;

        if (video.hasCachedFiles()) {
            thumbUri = video.getThumbUri();
        } else {
            thumbUri = video.getThumbUri();
        }

        ImageView thumbImage = holder.getThumbImage();

        Picasso.with(this.context).load(thumbUri).into(thumbImage);

        if (video.isLocal()) {
            holder.getUploadIndicator().setImageAlpha(0x80);
            holder.getUploadIndicator().setImageResource(R.drawable.ic_cloud_off_white_24dp);
        } else {
            holder.getUploadIndicator().setImageAlpha(0xFF);
            holder.getUploadIndicator().setImageResource(R.drawable.ic_cloud_done_white_24dp);
        }
    }

    @Override
    public int getItemCount() {
        return this.videoIds.size();
    }

    public UUID getItem(int position) {
        try {
            return this.videoIds.get(position);
        } catch (IndexOutOfBoundsException ex ) {
            return null;
        }
    }

    public void setItems(List<UUID> videoIds) {
        this.videoIds = videoIds;
        notifyDataSetChanged();
    }

    public List<Integer> getSelectedItems() {
        return this.selectedItems;
    }

    public int getSelectedItemCount() {
        return this.selectedItems.size();
    }

    public boolean isSelected(Integer position) {
        return this.selectedItems.contains(position);
    }

    public void clearSelectedItems() {
        this.selectedItems.clear();
        notifyDataSetChanged();
    }

    public void toggleSelection(Integer position) {
        if (isSelected(position)) {
            // Note: the method parameter needs to be an Integer instead of the primitive int,
            // otherwise the list will treat the position here as an index and sometimes throw an
            // IndexOutOfBoundsException. What fun!
            this.selectedItems.remove(position);
        } else {
            this.selectedItems.add(position);
        }

        notifyItemChanged(position);
    }

    private boolean isInProgress(Integer position) {
        return this.itemsInProgress.contains(position);
    }

    public void showProgress(UUID videoId) {
        Integer position = this.videoIds.indexOf(videoId);

        this.itemsInProgress.add(position);

        notifyItemChanged(position);
    }

    public void hideProgress(UUID videoId) {
        Integer position = this.videoIds.indexOf(videoId);

        this.itemsInProgress.remove(position);

        notifyItemChanged(position);
    }

    protected static final class ViewHolder extends RecyclerView.ViewHolder {

        private View view;
        private TextView titleText;
        private ImageView thumbImage;
        private ImageView uploadIndicator;
        private ProgressBar progressBar;
        private View selectionOverlay;

        public ViewHolder(View view) {
            super(view);

            this.view = view;
            this.titleText = (TextView) view.findViewById(R.id.titleText);
            this.thumbImage = (ImageView) view.findViewById(R.id.thumbImage);
            this.uploadIndicator = (ImageView) view.findViewById(R.id.uploadButton);
            this.progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            this.selectionOverlay = view.findViewById(R.id.selectionOverlay);
        }

        public View getView() {
            return this.view;
        }

        public TextView getTitleText() {
            return this.titleText;
        }

        public ImageView getThumbImage() {
            return this.thumbImage;
        }

        public ImageView getUploadIndicator() {
            return this.uploadIndicator;
        }

        public ProgressBar getProgressBar() {
            return this.progressBar;
        }

        public View getSelectionOverlay() {
            return this.selectionOverlay;
        }

    }

}
