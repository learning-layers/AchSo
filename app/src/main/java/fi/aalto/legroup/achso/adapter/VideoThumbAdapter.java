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

package fi.aalto.legroup.achso.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.util.App;

/**
 * An adapter that builds the bridge between an array of {@link fi.aalto.legroup.achso.database.SemanticVideo}s
 * and the View.
 *
 */
public class VideoThumbAdapter extends ArrayAdapter<SemanticVideo> {

    private static final int ITEM_TYPE_VIDEO = 0;
    private static final int ITEM_TYPE_SEPARATOR = 1;
    public static final int ITEM_TYPE_RECORD_BUTTON = 2;
    private static final int ITEM_TYPE_COUNT = 3;
    private final int mQueryType;

    private List<SemanticVideo> mLocalVideos;
    private List<SemanticVideo> mRemoteVideos;

    PrettyTime mPrettyTime;
    Context mContext;

    public VideoThumbAdapter(Context c, List<SemanticVideo> videos, int query_type) {
        super(c, 0, videos);
        mContext = c;
        mQueryType = query_type;
        mPrettyTime = new PrettyTime();
        mLocalVideos = new ArrayList<SemanticVideo>();
        mRemoteVideos = new ArrayList<SemanticVideo>();
    }

    /**
     * Decides which layout to show for a specific array entry. Basically calls {@link #getItemViewType(int)}
     * to determine the type of the data and then inflates the according layout.
     *
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        LayoutInflater i = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        boolean isTablet = App.isTablet();
        int view_type = getItemViewType(position);
        switch(view_type) {
            case ITEM_TYPE_RECORD_BUTTON:
                convertView = i.inflate(R.layout.large_record_item, null);
                return convertView;
            case ITEM_TYPE_SEPARATOR:
                convertView = i.inflate(R.layout.list_separator, null);
                return convertView;
            case ITEM_TYPE_VIDEO:
                break;
        }
        Resources res = mContext.getResources();

        // prepare convertView -object if not old version available
        if (convertView == null) {
            vh = new ViewHolder();
            if (isTablet) {
                convertView = i.inflate(R.layout.video_thumb_grid_item, null);
                assert (convertView != null);
                // Borders exist only in tablet-mode
                vh.border = (LinearLayout) convertView.findViewById(R.id.grid_item_border);
                vh.title = (TextView) convertView.findViewById(R.id.grid_item_title);
                vh.genre = (TextView) convertView.findViewById(R.id.grid_item_genre);
                vh.author = (TextView) convertView.findViewById(R.id.grid_item_author);
                vh.duration_land = (TextView) convertView.findViewById(R.id.grid_item_duration);
                vh.duration_port = (TextView) convertView
                        .findViewById(R.id.grid_item_duration_port);
                vh.timestamp = (TextView) convertView.findViewById(R.id.grid_item_timestamp);
            } else {
                convertView = i.inflate(R.layout.video_thumb_list_item, null);
                assert (convertView != null);
                vh.title = (TextView) convertView.findViewById(R.id.video_thumb_title);
                vh.genre = (TextView) convertView.findViewById(R.id.video_thumb_genre);
                vh.author = (TextView) convertView.findViewById(R.id.video_thumb_author);
                vh.duration_land = (TextView) convertView.findViewById(R.id.landscape_thumb_duration_text);
                vh.duration_port = (TextView) convertView
                        .findViewById(R.id.portrait_thumb_duration_text);
                vh.timestamp = (TextView) convertView.findViewById(R.id.video_thumb_timestamp);
            }
            vh.thumbnail_land = (RelativeLayout) convertView.findViewById(R.id.landscape_thumb_layout);
            vh.thumbnail_port = (RelativeLayout) convertView.findViewById(R.id.portrait_thumb_layout);

            vh.progress = (ProgressBar) convertView.findViewById(R.id.upload_progress);
            vh.uploadIcon = (ImageView) convertView.findViewById(R.id.upload_icon);
            vh.localIcon = (ImageView) convertView.findViewById(R.id.local_icon);
            vh.cloudIcon = (ImageView) convertView.findViewById(R.id.cloud_icon);
            // store viewholder, save pointers to different fields in this thumb
            convertView.setTag(vh);
        } else {
            // restore viewholder, get pointers to fields
            vh = (ViewHolder) convertView.getTag();
        }
        // fill convertView's fields with video data
        SemanticVideo v = getItem(position);
        vh.title.setText(v.getTitle());
        if (v.getCreationTime() != null) {
            vh.timestamp.setText(mPrettyTime.format(v.getCreationTime()));
        }
        vh.genre.setText(v.getGenreText());
        String author = v.getCreator();
        if (author == null || author.isEmpty()) {
            if (v.inLocalDB()) {
                author = mContext.getString(R.string.author_is_me);
            } else {
                author = mContext.getString(R.string.author_is_unknown);
            }
        }
        vh.author.setText(author);
        long min = TimeUnit.MILLISECONDS.toMinutes(v.getDuration(getContext()));
        long sec = TimeUnit.MILLISECONDS.toSeconds(v.getDuration(getContext())) % 60;
        if (vh.duration_land != null) {
            vh.duration_land.setText(Long.toString(min) + ":" + String.format("%02d", sec));
        }
        if (vh.duration_port != null) {
            vh.duration_port.setText(Long.toString(min) + ":" + String.format("%02d", sec));
        }
        switch (v.getGenre()) {
            case Problem:
                vh.genre.setTextColor(res.getColor(R.color.achso_red));
                if (isTablet) {
                    setBorderBackground(vh.border, res.getDrawable(R.drawable.grid_item_border_problem));
                }
                break;
            case GoodWork:
                vh.genre.setTextColor(res.getColor(R.color.achso_green));
                if (isTablet) {
                    setBorderBackground(vh.border,
                            res.getDrawable(R.drawable.grid_item_border_problemsolved));
                }
                break;
            case TrickOfTrade:
                vh.genre.setTextColor(res.getColor(R.color.achso_yellow));
                if (isTablet) {
                    setBorderBackground(vh.border, res.getDrawable(R.drawable.grid_item_border_howto));
                }
                break;
            case SiteOverview:
                vh.genre.setTextColor(res.getColor(R.color.achso_blue));
                if (isTablet) {
                    setBorderBackground(vh.border,
                            res.getDrawable(R.drawable.grid_item_border_dontdothis));
                }
                break;
        }
        if (v.inLocalDB()) {
            BitmapDrawable thumb = new BitmapDrawable(mContext.getResources(), v.getThumbnail(MediaStore.Images.Thumbnails.MINI_KIND));
            if (thumb.getIntrinsicWidth() > thumb.getIntrinsicHeight()) {
                setThumbBackground(vh.thumbnail_land, thumb);
                vh.thumbnail_land.setVisibility(View.VISIBLE);
                vh.thumbnail_port.setVisibility(View.GONE);
            } else {
                setThumbBackground(vh.thumbnail_port, thumb);
                vh.thumbnail_port.setVisibility(View.VISIBLE);
                vh.thumbnail_land.setVisibility(View.GONE);
            }
        } else {
            BitmapDrawable thumb = (BitmapDrawable) res.getDrawable(R.drawable.circle);
            setThumbBackground(vh.thumbnail_land, thumb);
            vh.thumbnail_land.setVisibility(View.VISIBLE);
            vh.thumbnail_port.setVisibility(View.GONE);
            v.putThumbnailTo(vh);
        }
        if (v.isNeverUploaded()) {
            vh.uploadIcon.setColorFilter(null);
            vh.uploadIcon.setVisibility(View.GONE);
        } else if (v.isUploading()) {
            vh.progress.setVisibility(View.VISIBLE);
            vh.uploadIcon.setVisibility(View.VISIBLE);
            vh.uploadIcon.setColorFilter(res.getColor(R.color.upload_icon_uploading));
        } else if (v.isUploadPending()) {
            vh.progress.setVisibility(View.VISIBLE);
            vh.uploadIcon.setVisibility(View.VISIBLE);
            vh.uploadIcon.setColorFilter(res.getColor(R.color.upload_icon_pending));
        } else if (v.isUploaded()) {
            vh.progress.setVisibility(View.GONE);
            vh.uploadIcon.setColorFilter(null);
            vh.uploadIcon.setVisibility(View.GONE);
            vh.cloudIcon.setColorFilter(null);
        } else if (v.isProcessing()) {
            vh.progress.setVisibility(View.GONE);
            vh.uploadIcon.setVisibility(View.GONE);
            vh.cloudIcon.setVisibility(View.VISIBLE);
        }
        if (v.inCloud()) {
            vh.cloudIcon.setVisibility(View.VISIBLE);
        } else {
            vh.cloudIcon.setVisibility(View.GONE);
        }
        if (v.inLocalDB()) {
            vh.localIcon.setVisibility(View.VISIBLE);
        } else {
            vh.localIcon.setVisibility(View.GONE);
        }

        return convertView;
    }

    @TargetApi(16)
    public static void setBorderBackground(LinearLayout layout, Drawable drawable) {
        if (Build.VERSION.SDK_INT < 16) {
            layout.setBackgroundDrawable(drawable);
        } else {
            layout.setBackground(drawable);
        }
    }

    @TargetApi(16)
    public static void setThumbBackground(RelativeLayout layout, BitmapDrawable thumb) {
        if (Build.VERSION.SDK_INT < 16) {
            layout.setBackgroundDrawable(thumb);
        } else {
            layout.setBackground(thumb);
        }
    }

    @Override
    public int getViewTypeCount() {
        return ITEM_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) == null ? ITEM_TYPE_RECORD_BUTTON : ITEM_TYPE_VIDEO;
    }

    public void updateLocalVideos(List<SemanticVideo> localVideos) {
        mLocalVideos = localVideos;
        this.clear();
        if (mQueryType == BrowsePagerAdapter.MY_VIDEOS && !App.isHorizontalCandybar()) {
            this.add(null);
        }
        this.addAll(mLocalVideos);
        this.addAll(mRemoteVideos);
    }

    public void updateRemoteVideos(List<SemanticVideo> remoteVideos) {
        mRemoteVideos = remoteVideos;
        this.clear();
        if (mQueryType == BrowsePagerAdapter.MY_VIDEOS && !App.isHorizontalCandybar()) {
            this.add(null);
        }
        this.addAll(mLocalVideos);
        this.addAll(mRemoteVideos);
    }

    public void clearRemoteVideos() {
        mRemoteVideos.clear();
        this.clear();
        this.addAll(mLocalVideos);
    }

    /**
     * Holds references to all views in a list or grid item.
     */
    public class ViewHolder implements Target {
        public LinearLayout border;
        public TextView title;
        public TextView genre;
        public TextView author;
        public TextView timestamp;
        public TextView duration_land;
        public TextView duration_port;
        public RelativeLayout thumbnail_land;
        public RelativeLayout thumbnail_port;
        public ProgressBar progress;
        public ImageView uploadIcon;
        public ImageView localIcon;
        public ImageView cloudIcon;

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            BitmapDrawable thumb = new BitmapDrawable(mContext.getResources(), bitmap);
            if (thumb.getIntrinsicWidth() > thumb.getIntrinsicHeight()) {
                setThumbBackground(this.thumbnail_land, thumb);
                this.thumbnail_land.setVisibility(View.VISIBLE);
                this.thumbnail_port.setVisibility(View.GONE);
            } else {
                setThumbBackground(this.thumbnail_port, thumb);
                this.thumbnail_port.setVisibility(View.VISIBLE);
                this.thumbnail_land.setVisibility(View.GONE);
            }

        }

        @Override
        public void onBitmapFailed(Drawable drawable) {

        }

        @Override
        public void onPrepareLoad(Drawable drawable) {

        }
    }
}
