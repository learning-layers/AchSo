package fi.aalto.legroup.achso.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import javax.annotation.Nonnull;

/**
 * A ViewGroup containing a child view that can be zoomed and panned by pinching and dragging.
 *
 * TODO: Zoom into the pinching focus point instead of the centre
 */
public class ZoomView extends FrameLayout {

    protected GestureDetector gestureDetector;
    protected ScaleGestureDetector scaleGestureDetector;

    protected View child;

    public ZoomView(Context context) {
        super(context);
        init();
    }

    public ZoomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ZoomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), new TranslationListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScalingListener());
    }

    protected void translateChild(float translationX, float translationY) {
        // Translation allowed in the positive direction. The scaled dimensions must be taken into
        // consideration. These will be non-positive if the child fits, i.e. it isn't scaled out of
        // its bounds.
        float maxTranslationX = (child.getWidth() * child.getScaleX() - getWidth()) / 2;
        float maxTranslationY = (child.getHeight() * child.getScaleY() - getHeight()) / 2;

        // If the view is completely inside its bounds, don't allow translating it.
        if (maxTranslationX < 0) maxTranslationX = 0;
        if (maxTranslationY < 0) maxTranslationY = 0;

        // Translation allowed into the negative direction
        float minTranslationX = -maxTranslationX;
        float minTranslationY = -maxTranslationY;

        translationX = limitToRange(translationX, minTranslationX, maxTranslationX);
        translationY = limitToRange(translationY, minTranslationY, maxTranslationY);

        child.setTranslationX(translationX);
        child.setTranslationY(translationY);
    }

    /**
     * Ensures that a value stays within a specified range.
     */
    private float limitToRange(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    @Override
    protected void onFinishInflate() {
        if (getChildCount() > 1) {
            throw new IllegalStateException("Only 1 child allowed in ZoomView.");
        }

        super.onFinishInflate();
    }

    @Override
    public void addView(@Nonnull View child, int index, ViewGroup.LayoutParams params) {
        this.child = child;
        super.addView(child, index, params);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean hasChild = (getChildCount() > 0);

        if (hasChild) {
            gestureDetector.onTouchEvent(event);
            scaleGestureDetector.onTouchEvent(event);
        }

        return false;
    }

    /**
     * Detects translation gestures.
     */
    private class TranslationListener extends GestureDetector.SimpleOnGestureListener {

        float initialTranslationX;
        float initialTranslationY;



        @Override
        public boolean onDown(MotionEvent event) {
            // Store the initial translation values when translation starts
            initialTranslationX = child.getTranslationX();
            initialTranslationY = child.getTranslationY();

            // Best practice to always return true here.
            // http://developer.android.com/training/gestures/detector.html#detect
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent downEvent, MotionEvent scrollEvent, float distanceX,
                                float distanceY) {

            // Disregard the scroll event when there are multiple pointers to avoid conflicts with
            // the pinch to zoom gesture.
            if (downEvent.getPointerCount() > 1) return false;
            if (scrollEvent.getPointerCount() > 1) return false;

            // Use raw coordinates to determine the scrolling distance since relative coordinates
            // (also distanceX and distanceY) are affected by the translation.
            float deltaX = scrollEvent.getRawX() - downEvent.getRawX();
            float deltaY = scrollEvent.getRawY() - downEvent.getRawY();

            float translationX = initialTranslationX + deltaX;
            float translationY = initialTranslationY + deltaY;

            translateChild(translationX, translationY);

            return true;
        }

    }

    /**
     * Detects scaling gestures.
     */
    private class ScalingListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private static final float MINIMUM_SCALE = 1;
        private static final float MAXIMUM_SCALE = 3;
        private static final float SCALING_THRESHOLD = 0.01f;

        private float scale = 1;
        private int focusX = 0;
        private int focusY = 0;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // Only scale if the factor is above the threshold
            return detector.getScaleFactor() > SCALING_THRESHOLD;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor();
            focusX = (int)detector.getFocusX();
            focusY = (int)detector.getFocusY();

            scale = limitToRange(scale, MINIMUM_SCALE, MAXIMUM_SCALE);

            child.setPivotX(focusX);
            child.setPivotY(focusY);
            child.setScaleX(scale);
            child.setScaleY(scale);


            // Recalculate the translation to keep the child inside the bounds
            translateChild(child.getTranslationX(), child.getTranslationY());

            return true;
        }

    }

}
