package fi.aalto.legroup.achso.playback.annotations;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.DrawableRes;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.playback.AnnotationEditor;
import fi.aalto.legroup.achso.views.Marker;
import fi.aalto.legroup.achso.views.MarkerCanvas;

/**
 * Draws markers on a canvas for each annotation. Delegates marker actions to an editor class.
 */
public final class MarkerStrategy implements AnnotationRenderer.Strategy, MarkerCanvas.Listener {

    private final MarkerCanvas canvas;
    private final AnnotationEditor editor;
    private final Hashtable<Integer, LayerDrawable> markerCache;

    public MarkerStrategy(MarkerCanvas canvas, AnnotationEditor editor) {
        this.canvas = canvas;
        this.editor = editor;
        this.markerCache = new Hashtable<Integer, LayerDrawable>();

        canvas.setListener(this);
    }

    private LayerDrawable getMarkerForColor(int color) {

        LayerDrawable drawable = markerCache.get(color);

        if (drawable != null) {
            return drawable;
        } else {
            // Replace the placeholder outer ring with a colored one
            Drawable ring = getDrawable(canvas.getContext(), R.drawable.annotation_marker_outer_ring).mutate();
            LayerDrawable bg = (LayerDrawable) getDrawable(canvas.getContext(), R.drawable.annotation_marker).mutate();
            ring.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            bg.setDrawableByLayerId(R.id.outer_ring, ring);
            markerCache.put(color, bg);
            return bg;
        }
    }

    @Override
    public void render(final List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            PointF markerPosition = annotation.getPosition();
            int color = annotation.calculateColor();
            LayerDrawable markerBackground = getMarkerForColor(color);
            Marker marker = canvas.addMarker(markerPosition, markerBackground);
            marker.setTag(annotation);
        }
    }

    @Override
    public void clear() {
        canvas.clearMarkers();
    }

    @Override
    public void onMarkerTapped(Marker marker) {
        Annotation annotation = (Annotation) marker.getTag();
        editor.editAnnotation(annotation);
    }

    @Override
    public void onMarkerDragged(Marker marker, PointF newPosition) {
        Annotation annotation = (Annotation) marker.getTag();
        editor.moveAnnotation(annotation, newPosition);
    }

    @Override
    public void onCanvasTapped(PointF position) {
        editor.createAnnotation(position);
    }

    @SuppressWarnings("deprecation")
    private static Drawable getDrawable(Context context, @DrawableRes int resource) {
        Resources resources = context.getResources();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            Resources.Theme theme = context.getTheme();
            return resources.getDrawable(resource, theme);
        } else {
            return resources.getDrawable(resource);
        }
    }

}
