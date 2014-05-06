package fi.aalto.legroup.achso.pager;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
* Created by purma on 6.5.2014.
*/
public class SwipeDisabledViewPager extends ViewPager {
    private boolean mSwipeEnabled;

    public SwipeDisabledViewPager(Context context) {
        super(context);
    }

    public SwipeDisabledViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void setSwipeEnabled(boolean b) {
        mSwipeEnabled = b;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mSwipeEnabled) {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mSwipeEnabled) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    public boolean getSwipeEnabled() {
        return mSwipeEnabled;
    }
}
