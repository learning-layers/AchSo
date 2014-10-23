package fi.aalto.legroup.achso.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.util.PinchToZoomHelper;

/**
 * An area for markers that can be positioned, selected, and dragged around.
 *
 * @author Leo Nikkilä
 */
public class MarkerCanvas extends FrameLayout implements View.OnClickListener,
        PinchToZoomHelper.OnHoldDelegate {

    private Listener listener;
    private PinchToZoomHelper zoomMatrix;

    private List<Marker> markers = new ArrayList<Marker>();
    private LayoutParams wrapLayoutParams;

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

    public void init() {
        wrapLayoutParams = new LayoutParams(Marker.MARKER_SIZE, Marker.MARKER_SIZE);
    }

    public void matrixHasChanged(PinchToZoomHelper helper) {
        float scale = helper.getCurrentZoomLevel();

        for (Marker marker : markers) {
            float orgX = marker.getOriginalX();
            float orgY = marker.getOriginalY();
            float[] point = {orgX, orgY};
            point = helper.mapCoordinates(point);

            marker.setX(point[0]);
            marker.setY(point[1]);

            int size = (int)(Marker.MARKER_SIZE * scale);
            wrapLayoutParams = new LayoutParams(size, size);
            marker.setLayoutParams(wrapLayoutParams);
        }
    }

    public void setZoomMatrix(PinchToZoomHelper matrix) {
        this.zoomMatrix = matrix;
        this.zoomMatrix.setOnHoldDelegate(this);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public Marker addMarker(FloatPosition position, Drawable background) {
        return addMarker(position, background, true);
    }

    public Marker addMarker(FloatPosition position, Drawable background, boolean isDraggable) {
        Marker marker = new Marker(getContext());

        marker.setBackground(background);
        marker.setDraggable(isDraggable);
        marker.setOnClickListener(this);

        addView(marker);

        float posX = getWidth() * position.getX() - marker.getWidth() / 2;
        float posY = getHeight() * position.getY() - marker.getHeight() / 2;

        marker.setX(posX);
        marker.setY(posY);

        float orgX = marker.getOriginalX();
        float orgY = marker.getOriginalY();

        float scale = this.zoomMatrix.getCurrentZoomLevel();
        if(scale != 1) {
            float[] point = {orgX, orgY};
            point = this.zoomMatrix.mapCoordinates(point);

            marker.setX(point[0]);
            marker.setY(point[1]);
            int size = (int)(Marker.MARKER_SIZE * scale);
            wrapLayoutParams = new LayoutParams(size, size);
            marker.setLayoutParams(wrapLayoutParams);
            marker.invalidate();
        } else {
            if(posX != orgX) {
                marker.setX(orgX);
                marker.setY(orgY);
            }

            wrapLayoutParams = new LayoutParams(Marker.MARKER_SIZE, Marker.MARKER_SIZE);
            marker.setLayoutParams(wrapLayoutParams);
            marker.invalidate();
        }

        markers.add(marker);

        return marker;
    }

    public void removeMarker(Marker marker) {
        removeView(marker);
        markers.remove(marker);
    }

    public void clearMarkers() {
        removeAllViews();
        markers.clear();
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

    private void canvasTapped(float positionX, float positionY) {
        if (listener == null) return;

        float posX = positionX / getWidth();
        float posY = positionY / getHeight();

        FloatPosition position = new FloatPosition(posX, posY);

        listener.onCanvasTapped(position);
    }

    /**
     * Called when a marker is tapped.
     */
    @Override
    public void onClick(View view) {
        if (listener != null) listener.onMarkerTapped((Marker) view);
    }

    public void onTouchHold(float positionX, float positionY) {
        canvasTapped(positionX, positionY);
    }

    /**
     * Called when the canvas is touched.
     */
    @Override
    public boolean onTouchEvent(@Nonnull MotionEvent event) {
        super.onTouchEvent(event);
        return zoomMatrix.receiveTouchEvent(event);
    }

    /**
     * Accepts dragged markers that are dropped onto the container and sets their new position.
     */
    @Override
    public boolean onDragEvent(DragEvent event) {
        int action = event.getAction();

        if (action == DragEvent.ACTION_DROP) {
            Marker marker = (Marker) event.getLocalState();

            Float posX = event.getX();
            Float posY = event.getY();

            marker.setX(posX - marker.getWidth() / 2f);
            marker.setY(posY - marker.getHeight() / 2f);

            Float relativeX = posX / getWidth();
            Float relativeY = posY / getHeight();

            FloatPosition pos = new FloatPosition(relativeX, relativeY);

            listener.onMarkerDragged(marker, pos);
        }

        return true;
    }

    public interface Listener {

        public void onMarkerTapped(Marker marker);

        public void onMarkerDragged(Marker marker, FloatPosition newPos);

        public void onCanvasTapped(FloatPosition pos);

    }

}
