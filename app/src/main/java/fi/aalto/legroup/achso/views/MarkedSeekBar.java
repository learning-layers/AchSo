package fi.aalto.legroup.achso.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;

import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.views.utilities.DimensionUnits;
import fi.aalto.legroup.achso.views.utilities.ThemeColors;

public class MarkedSeekBar extends SeekBar {

    protected static final int MARKER_RADIUS_DP = 4;
    protected static final int MARKER_STROKE_WIDTH_DP = 2;

    protected SnappingOnSeekBarChangeListener listener;
    protected Paint trimIndicatorPaint;
    protected Paint markerPaint;
    protected Paint markerSecondaryPaint;
    protected Paint markerDisabledPaint;

    protected float markerRadiusPx;
    protected float markerStrokeWidthPx;

    private Hashtable<Integer, Paint> markerCache;

    protected List<Integer> markers = new ArrayList<>();
    protected List<Annotation> annotations = new ArrayList<>();

    protected int trimEndTime = 0;
    protected int trimStartTime = Integer.MAX_VALUE;

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
        this.markerCache = new Hashtable<Integer, Paint>();

        int accentColor = ThemeColors.getAccentColor(getContext());

        markerStrokeWidthPx = DimensionUnits.dpToPx(getContext(), MARKER_STROKE_WIDTH_DP);

        markerRadiusPx = DimensionUnits.dpToPx(getContext(), MARKER_RADIUS_DP);

        markerPaint = new Paint();
        markerPaint.setAntiAlias(true);
        markerPaint.setColor(accentColor);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(markerStrokeWidthPx);

        markerSecondaryPaint = new Paint(markerPaint);
        markerSecondaryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        // TODO: Make this styling more flexible
        markerDisabledPaint = new Paint(markerSecondaryPaint);
        markerDisabledPaint.setColor(Color.BLACK);
        markerDisabledPaint.setAlpha(0x20);

        trimIndicatorPaint = new Paint();
        trimIndicatorPaint.setAntiAlias(true);
        trimIndicatorPaint.setColor(Color.parseColor("#33b5e5"));
        trimIndicatorPaint.setStyle(Paint.Style.STROKE);
        trimIndicatorPaint.setStrokeWidth(markerStrokeWidthPx);

        listener = new SnappingOnSeekBarChangeListener(markers);

        super.setOnSeekBarChangeListener(listener);
    }


    /**
     * Sets the positions that should be drawn on the seek bar. Marker positions should be integers
     * that are greater than or equal to 0 and less than or equal to getMax().
     *
     * @param markers List of marker positions.
     */
    public void setMarkersAndAnnotations(List<Integer> markers, List<Annotation> annotations) {
        this.markers = markers;
        this.annotations = annotations;

        listener.setPositions(markers);

        // Force the view to redraw
        invalidate();
    }

    public void setTrim(int start, int end) {
        this.trimStartTime = start;
        this.trimEndTime = end;

        invalidate();
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener decoratedListener) {
        listener.setDelegate(decoratedListener);
        super.setOnSeekBarChangeListener(listener);
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());

        int markerAreaWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int posY = (getHeight() - getPaddingTop() - getPaddingBottom()) / 2;
        int max = getMax();

        if (trimEndTime != 0 || trimStartTime != Integer.MAX_VALUE) {
            float startX = (float) trimStartTime / max * markerAreaWidth;
            float endX = (float) trimEndTime / max * markerAreaWidth;

            canvas.drawCircle((int) startX, posY, markerRadiusPx, trimIndicatorPaint);
            canvas.drawCircle((int) endX, posY, markerRadiusPx, trimIndicatorPaint);
        }

        for (int i = 0; i < markers.size(); i++){
            int marker = markers.get(i);
            Annotation annotation = annotations.get(i);

            Paint paint = getMarkerPaint(marker, annotation);

            float posX = (float) marker / max * markerAreaWidth;

            canvas.drawCircle((int) posX, posY, markerRadiusPx, paint);
        }

        canvas.restore();
    }

    /**
     * Returns an appropriate paint for the specified marker.
     */
    private Paint getMarkerPaint(int marker, Annotation annotation) {
        if (!isEnabled()) {
            return markerDisabledPaint;
        }

        int color = annotation.calculateColor();

        Paint paint;

        if (this.markerCache.contains(color)) {
            paint = this.markerCache.get(color);
        } else {
            paint = new Paint();
            paint.setColor(color);
            markerCache.put(color, paint);
        }

        if (marker > getProgress()) {
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(markerStrokeWidthPx);
        }

        return paint;
    }

    /**
     * An implementation of an OnSeekBarChangeListener that snaps to positions on the bar. Can also
     * decorate other listeners to provide the same functionality.
     */
    private static final class SnappingOnSeekBarChangeListener implements OnSeekBarChangeListener {

        // How close a position should be before we snap to it. Fraction of the bar length.
        private final static float snappingDistance = 0.02f;

        private OnSeekBarChangeListener delegate;
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
         * Decorates a listener with snapping functionality.
         *
         * @param positions Positions to snap to.
         * @param delegate  Listener to decorate.
         */
        public SnappingOnSeekBarChangeListener(List<Integer> positions,
                                               OnSeekBarChangeListener delegate) {
            this.delegate = delegate;
            this.positions = positions;
        }

        public void setPositions(List<Integer> positions) {
            this.positions = positions;
        }

        public void setDelegate(OnSeekBarChangeListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && !positions.isEmpty()) {
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
                    progress = closestMarkerPosition;
                }
            }

            if (delegate != null) {
                delegate.onProgressChanged(seekBar, progress, fromUser);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (delegate != null) {
                delegate.onStartTrackingTouch(seekBar);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (delegate != null) {
                delegate.onStopTrackingTouch(seekBar);
            }
        }

    }

}
