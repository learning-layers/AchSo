package fi.aalto.legroup.achso.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import fi.aalto.legroup.achso.R;

/**
 * Created by lassi on 6.11.14.
 */
public class VideoGridItemView extends LinearLayout implements Checkable {
    public static final int ALPHA = 90;
    public static final int UNSELECTED_ALPHA = 160;
    public static final int SELECTED_ALPHA = 255;

    private ProgressBar progress;
    private boolean checked = false;

    private int color;

    public VideoGridItemView(Context context) {
        super(context);
    }

    public VideoGridItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoGridItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setColor(int color) {
        this.color = color;
        this.findViewById(R.id.item_video_genre_border).setBackgroundColor(this.color);
        this.findViewById(R.id.item_video_genre_color).setBackgroundColor(this.color);
    }

    public void setProgress(int progress) {
        if(this.progress == null) {
            this.progress = (ProgressBar)this.findViewById(R.id.progress_bar);
            this.findViewById(R.id.progress_container).setVisibility(View.VISIBLE);
        }

        this.progress.setProgress(progress);

        if(progress >= 100) {
            this.findViewById(R.id.progress_container).setVisibility(View.GONE);
            this.findViewById(R.id.item_video_cloud).setVisibility(View.VISIBLE);
            this.progress = null;
        }
        this.invalidate();
    }

    public boolean isChecked() {
        return this.checked;
    }

    public void setChecked(boolean b) {
        this.checked = b;
        if (b) {
            this.findViewById(R.id.item_video_genre_color).getBackground().setAlpha(SELECTED_ALPHA);
            this.findViewById(R.id.item_video_genre_border).getBackground().setAlpha(SELECTED_ALPHA);
            View background = this.findViewById(R.id.item_video_background);
            background.setBackgroundColor(this.color);
            background.getBackground().setAlpha(ALPHA);
        } else {
            this.findViewById(R.id.item_video_genre_color).getBackground().setAlpha(UNSELECTED_ALPHA);
            this.findViewById(R.id.item_video_genre_border).getBackground().setAlpha(UNSELECTED_ALPHA);
            View background = this.findViewById(R.id.item_video_background);
            background.setBackgroundColor(Color.parseColor("#FFFFFF"));
            background.getBackground().setAlpha(255);

        }
        this.invalidate();
    }

    public void toggle() {
        this.setChecked(!this.checked);
    }
}
