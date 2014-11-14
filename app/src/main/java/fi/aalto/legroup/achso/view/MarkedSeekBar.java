package fi.aalto.legroup.achso.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import fi.aalto.legroup.achso.util.DimensionUnits;

public class MarkedSeekBar extends SeekBar {

    protected static final int MARKER_RADIUS_DP = 5;

    protected List<Integer> markers = new ArrayList<Integer>();
    protected Paint markerPaint = new Paint();

    protected SnappingOnSeekBarChangeListener listener;

    public MarkedSeekBar(Context context) {
        super(context);
        init();
    }

    public MarkedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarkedSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        markerPaint.setAntiAlias(true);
        markerPaint.setColor(getResources().getColor(android.R.color.holo_blue_light));

        listener = new SnappingOnSeekBarChangeListener(markers);
        super.setOnSeekBarChangeListener(listener);
    }

    /**
     * Sets the positions that should be drawn on the seek bar. Marker positions should be integers
     * that are greater than or equal to 0 and less than or equal to getMax().
     *
     * @param markers List of marker positions.
     */
    public void setMarkers(List<Integer> markers) {
        this.markers = markers;

        listener.setPositions(markers);

        // Force the view to redraw
        invalidate();
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener decoratedListener) {
        listener = new SnappingOnSeekBarChangeListener(markers, decoratedListener);
        super.setOnSeekBarChangeListener(listener);
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        super.onDraw(canvas);

        if (markers.isEmpty()) return;

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());

        int max = getMax();
        int markerAreaWidth = getWidth() - getThumbOffset() - getPaddingRight();

        int posY = getThumbOffset();

        float markerRadius = DimensionUnits.dpToPx(getContext(), MARKER_RADIUS_DP);

        for (int marker : markers) {
            float posX = (float) marker / max * markerAreaWidth;
            canvas.drawCircle((int) posX, posY, markerRadius, markerPaint);
        }

        canvas.restore();
    }

    /**
     * An implementation of an OnSeekBarChangeListener that snaps to positions on the bar. Can also
     * decorate other listeners to provide the same functionality.
     */
    private static final class SnappingOnSeekBarChangeListener implements OnSeekBarChangeListener {

        // How close a position should be before we snap to it. Fraction of the bar length.
        private final static float snappingDistance = 0.02f;

        private OnSeekBarChangeListener listener;
        private List<Integer> positions;

        /**
         * Constructs a new listener.
         *
         * @param positions Positions to snap to.
         */
        public SnappingOnSeekBarChangeListener(List<Integer> positions) {
            this.positions = positions;
        }

        /**
         * Decorates a previous listener with snapping functionality.
         *
         * @param positions Positions to snap to.
         * @param listener  The original listener to decorate.
         */
        public SnappingOnSeekBarChangeListener(List<Integer> positions,
                                               OnSeekBarChangeListener listener) {
            this.listener = listener;
            this.positions = positions;
        }

        public void setPositions(List<Integer> positions) {
            this.positions = positions;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (listener != null) listener.onProgressChanged(seekBar, progress, fromUser);

            if ( ! fromUser || positions.isEmpty()) return;

            // Take one marker that should be compared
            int closestMarkerPosition = positions.get(0);
            int closestMarkerDistance = Math.abs(closestMarkerPosition - progress);

            // See if we can find a closer marker
            for (int marker : positions) {
                int distance = Math.abs(marker - progress);

                if (distance < closestMarkerDistance) {
                    closestMarkerPosition = marker;
                    closestMarkerDistance = distance;
                }
            }

            // If the closest marker is within snapping distance, snap to it
            if (closestMarkerDistance <= seekBar.getMax() * snappingDistance) {
                seekBar.setProgress(closestMarkerPosition);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (listener != null) listener.onStartTrackingTouch(seekBar);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (listener != null) listener.onStopTrackingTouch(seekBar);
        }

    }

}
