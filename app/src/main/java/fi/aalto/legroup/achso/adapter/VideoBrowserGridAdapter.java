package fi.aalto.legroup.achso.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.view.VideoGridItemView;

/**
 * Created by lassi on 31.10.14.
 */
public class VideoBrowserGridAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private List<SemanticVideo> videos;

    public VideoBrowserGridAdapter(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setVideos(List<SemanticVideo> videos) {
        this.videos = videos;
    }

    public int getCount() {
        if (this.videos == null) {
            return 0;
        }
        return this.videos.size();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View cell;

        if (convertView == null) {
            cell = inflater.inflate(R.layout.item_video_browser_grid, null);
        } else {
            cell = convertView;
        }

        this.populateCell((VideoGridItemView) cell, this.videos.get(position));
        return cell;
    }

    private void populateCell(VideoGridItemView cell, SemanticVideo video) {
        Resources res = cell.getResources();

        TextView field = (TextView) cell.findViewById(R.id.item_video_title);
        field.setText(video.getTitle());

        field = (TextView) cell.findViewById(R.id.item_video_genre);
        field.setText(video.getGenreText());
        int color = res.getColor(video.getGenreColor());
        cell.setColor(color);

        field = (TextView) cell.findViewById(R.id.item_video_user);
        field.setText(video.getCreator());

        ImageView image = (ImageView) cell.findViewById(R.id.item_video_thumbnail);
        image.setImageBitmap(video.getThumbnail(MediaStore.Images.Thumbnails.MINI_KIND));

        String author = video.getCreator();
        if (author == null || author.isEmpty()) {
            if (video.inLocalDB()) {
                author = this.context.getString(R.string.author_is_me);
            } else {
                author = this.context.getString(R.string.author_is_unknown);
            }
        }
        field = (TextView) cell.findViewById(R.id.item_video_user);
        field.setText(author);

        image = (ImageView) cell.findViewById(R.id.item_video_cloud);
        if (video.inCloud()) {
            image.setVisibility(View.VISIBLE);
        } else {
            image.setVisibility(View.GONE);
        }

        image = (ImageView) cell.findViewById(R.id.item_video_sd);
        if (video.inLocalDB()) {
            image.setVisibility(View.VISIBLE);
        } else {
            image.setVisibility(View.GONE);
        }
    }

    public long getItemId(int position) {
        return 0;
    }

    public Object getItem(int position) {
        return null;
    }
}
