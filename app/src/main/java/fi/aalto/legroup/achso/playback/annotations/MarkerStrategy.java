package fi.aalto.legroup.achso.playback.annotations;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;

import java.util.List;

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
    private final Drawable markerBackground;

    public MarkerStrategy(MarkerCanvas canvas, AnnotationEditor editor) {
        this.canvas = canvas;
        this.editor = editor;
        this.markerBackground = getDrawable(canvas.getContext(), R.drawable.annotation_marker);

        canvas.setListener(this);
    }

    @Override
    public void render(final List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            PointF markerPosition = annotation.getPosition();
            int color = annotation.calculateColor();

            Marker marker = canvas.addMarker(markerPosition, color, markerBackground);

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
