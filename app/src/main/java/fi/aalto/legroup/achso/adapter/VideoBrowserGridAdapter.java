package fi.aalto.legroup.achso.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.SemanticVideo;

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
        if(this.videos == null) {
            return  0;
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

        this.populateCell(cell, this.videos.get(position));
        return cell;
    }

    private void populateCell(View cell, SemanticVideo video) {
        TextView title = (TextView)cell.findViewById(R.id.item_video_title);
        title.setText(video.getTitle());
    }

    public long getItemId(int position) {
        return 0;
    }

    public Object getItem(int position) {
        return null;
    }
}
