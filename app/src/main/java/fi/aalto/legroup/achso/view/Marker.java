package fi.aalto.legroup.achso.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import javax.annotation.Nonnull;

/**
 * A marker that can be clicked and dragged.
 *
 * It's up to the parent view to set an OnClickedListener and to react to dragged and dropped
 * markers via the onDragEvent method.
 *
 * @author Leo Nikkilä
 */
public class Marker extends View {

    private GestureDetector gestureDetector;

    private boolean isDraggable = true;

    public Marker(Context context) {
        super(context);
        init();
    }

    public Marker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Marker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), new OnGestureListener());
    }

    public void setDraggable(boolean isDraggable) {
        this.isDraggable = isDraggable;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;

        Drawable background = getBackground();

        if (background == null) {
            width = MeasureSpec.getSize(widthMeasureSpec);
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            width = background.getIntrinsicWidth();
            height = background.getIntrinsicHeight();
        }

        setMeasuredDimension(width, height);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBackground(Drawable background) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            super.setBackgroundDrawable(background);
        } else {
            super.setBackground(background);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        // Centre marker to its position
        setTranslationX(getTranslationX() - width / 2f);
        setTranslationY(getTranslationY() - height / 2f);
    }

    @Override
    public boolean onTouchEvent(@Nonnull MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    /**
     * Hide the actual marker during dragging since there is a shadow.
     */
    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                setVisibility(View.INVISIBLE);
                break;

            case DragEvent.ACTION_DRAG_ENDED:
                setVisibility(View.VISIBLE);
                break;
        }

        return super.onDragEvent(event);
    }



    /**
     * A gesture listener that starts dragging if the user presses their finger down onto the
     * view and then drags to exit the view bounds.
     */
    protected class OnGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent downEvent, MotionEvent scrollEvent, float distanceX,
                                float distanceY) {

            if (isDraggable) startDrag(null, new DragShadowBuilder(Marker.this), Marker.this, 0);

            return false;
        }

        /**
         * Restores clicking functionality by delegating to OnClickListener.
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            performClick();
            return super.onSingleTapUp(e);
        }

    }

}
