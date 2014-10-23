package fi.aalto.legroup.achso.annotation.renderers;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import java.util.Collection;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.annotation.AnnotationEditor;
import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.view.Marker;
import fi.aalto.legroup.achso.view.MarkerCanvas;

/**
 * Renders Marker views on a MarkerCanvas for each annotation.
 *
 * @author Leo Nikkilä
 */
public class MarkerRenderer extends AnnotationRenderer implements MarkerCanvas.Listener {

    private static final int MARKER_BACKGROUND_DRAWABLE = R.drawable.marker_square;

    private AnnotationEditor editor;
    private MarkerCanvas canvas;

    private Drawable markerBackground;

    public MarkerRenderer(AnnotationEditor editor, MarkerCanvas canvas) {
        this.editor = editor;
        this.canvas = canvas;

        Resources resources = canvas.getResources();

        markerBackground = resources.getDrawable(MARKER_BACKGROUND_DRAWABLE);

        canvas.setListener(this);
    }

    @Override
    public void render(Collection<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            FloatPosition position = annotation.getPosition();

            markerBackground.setColorFilter(annotation.getColor(), PorterDuff.Mode.MULTIPLY);

            Marker marker = canvas.addMarker(position, markerBackground);
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
    public void onMarkerDragged(Marker marker, FloatPosition newPos) {
        Annotation annotation = (Annotation) marker.getTag();
        editor.moveAnnotation(annotation, newPos);
    }

    @Override
    public void onCanvasTapped(FloatPosition pos) {
        editor.createAnnotation(pos);
    }

}
