package fi.aalto.legroup.achso.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
    private OnItemClickListener mListener;
    private boolean isLong = false;

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
        public void onItemLongPress(View view, int position);
    }

    GestureDetector mGestureDetector;

    public RecyclerItemClickListener(Context context, OnItemClickListener listener) {
        mListener = listener;
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                RecyclerItemClickListener.this.isLong = true;
                super.onLongPress(e);
            }


        });
    }

    @Override public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        View childView = view.findChildViewUnder(e.getX(), e.getY());

        if (childView != null && mListener != null && this.isLong) {
            mListener.onItemLongPress(childView, view.getChildPosition(childView));
            this.isLong = false;
        }

        if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e)) {
            mListener.onItemClick(childView, view.getChildPosition(childView));
            this.isLong = false;
        }


        return false;
    }

    @Override public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) { }
}
