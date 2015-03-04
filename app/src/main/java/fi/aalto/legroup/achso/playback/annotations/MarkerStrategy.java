package fi.aalto.legroup.achso.playback.annotations;

import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

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

    private static final int MARKER_BACKGROUND_DRAWABLE = R.drawable.marker_square;

    private MarkerCanvas canvas;
    private AnnotationEditor editor;
    private Drawable markerBackground;

    public MarkerStrategy(MarkerCanvas canvas, AnnotationEditor editor) {
        this.canvas = canvas;
        this.editor = editor;
        this.markerBackground = canvas.getResources().getDrawable(MARKER_BACKGROUND_DRAWABLE);

        canvas.setListener(this);
    }

    @Override
    public void render(final List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            PointF markerPosition = annotation.getPosition();
            int color = annotation.getAuthor().getColor();

            markerBackground.setColorFilter(color, PorterDuff.Mode.MULTIPLY);

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

}
