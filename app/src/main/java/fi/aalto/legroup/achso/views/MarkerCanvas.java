package fi.aalto.legroup.achso.views;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import javax.annotation.Nonnull;

/**
 * An area for markers that can be positioned, selected, and dragged around.
 */
public class MarkerCanvas extends FrameLayout implements View.OnClickListener {

    private Listener listener;

    private GestureDetector gestureDetector;

    public MarkerCanvas(Context context) {
        super(context);
        init();
    }

    public MarkerCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarkerCanvas(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public Marker addMarker(PointF position, Drawable background) {
        return addMarker(position, background, true);
    }

    public Marker addMarker(PointF position, Drawable background, boolean isDraggable) {
        Marker marker = new Marker(getContext());

        marker.setImageDrawable(background);
        marker.setDraggable(isDraggable);
        marker.setOnClickListener(this);

        addView(marker);

        float posX = getWidth() * position.x - marker.getWidth() / 2;
        float posY = getHeight() * position.y - marker.getHeight() / 2;

        marker.setX(posX);
        marker.setY(posY);

        return marker;
    }

    public void removeMarker(Marker marker) {
        removeView(marker);
    }

    public void clearMarkers() {
        removeAllViews();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        // Compute new marker positions
        for (int i = 0; i < getChildCount(); i++) {
            Marker marker = (Marker) getChildAt(i);

            float newPosX = marker.getX() / oldWidth * width;
            float newPosY = marker.getY() / oldHeight * height;

            marker.setX(newPosX);
            marker.setY(newPosY);
        }
    }

    private void canvasTapped(@Nonnull MotionEvent event) {
        if (listener == null) {
            return;
        }

        float posX = event.getX() / getWidth();
        float posY = event.getY() / getHeight();

        PointF position = new PointF(posX, posY);

        listener.onCanvasTapped(position);
    }

    @Override
    public boolean onTouchEvent(@Nonnull MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    /**
     * Called when a marker is tapped.
     */
    @Override
    public void onClick(View view) {
        if (listener != null) {
            listener.onMarkerTapped((Marker) view);
        }
    }

    /**
     * Accepts dragged markers that are dropped onto the container and sets their new position.
     */
    @Override
    public boolean onDragEvent(DragEvent event) {
        int action = event.getAction();

        if (action == DragEvent.ACTION_DROP) {
            Marker marker = (Marker) event.getLocalState();

            float posX = event.getX();
            float posY = event.getY();

            marker.setX(posX - marker.getWidth() / 2f);
            marker.setY(posY - marker.getHeight() / 2f);

            float relativeX = posX / getWidth();
            float relativeY = posY / getHeight();

            PointF pos = new PointF(relativeX, relativeY);

            listener.onMarkerDragged(marker, pos);
        }

        return true;
    }

    public interface Listener {

        public void onMarkerTapped(Marker marker);

        public void onMarkerDragged(Marker marker, PointF newPos);

        public void onCanvasTapped(PointF pos);

    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        /**
         * Called when the canvas is tapped once.
         */
        @Override
        public boolean onDown(MotionEvent event) {
            canvasTapped(event);
            // Best practice to always return true here.
            // http://developer.android.com/training/gestures/detector.html#detect
            return true;
        }

    }

}
