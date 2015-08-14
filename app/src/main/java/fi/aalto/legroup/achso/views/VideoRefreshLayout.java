package fi.aalto.legroup.achso.views;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

public class VideoRefreshLayout extends SwipeRefreshLayout {

    public VideoRefreshLayout(Context context) {
        super(context);
    }
    public VideoRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canChildScrollUp() {
        // Disable pull to refresh here.
        //
        // Pull to refresh doesn't feel right here because of two-axis scrolling of the videos.
        // Adding can be reconsidered, but it would require potentially lots of tweaking, since
        // the default behavior feels very broken. Currently sync happens every time the browsing
        // view is resumed so the content gets updated quite frequently anyway.

        // If the child can scroll up the pull to refresh behavior is not used, so just pretend
        // that the child can always scroll up even when that's not the case.
        return true;
    }
}
