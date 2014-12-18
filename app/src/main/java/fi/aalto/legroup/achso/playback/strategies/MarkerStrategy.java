package fi.aalto.legroup.achso.playback.strategies;

import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import java.util.Collection;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.playback.AnnotationEditor;
import fi.aalto.legroup.achso.views.Marker;
import fi.aalto.legroup.achso.views.MarkerCanvas;

/**
 * Renders Marker views on a MarkerCanvas for each annotation.
 *
 * @author Leo Nikkilä
 */
public class MarkerStrategy extends AnnotationStrategy implements MarkerCanvas.Listener {

    private static final int MARKER_BACKGROUND_DRAWABLE = R.drawable.marker_square;

    private AnnotationEditor editor;
    private MarkerCanvas canvas;

    private Drawable markerBackground;

    public MarkerStrategy(AnnotationEditor editor, MarkerCanvas canvas) {
        this.editor = editor;
        this.canvas = canvas;

        Resources resources = canvas.getResources();

        markerBackground = resources.getDrawable(MARKER_BACKGROUND_DRAWABLE);

        canvas.setListener(this);
    }

    @Override
    public void execute(Collection<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            PointF position = annotation.getPosition();
            int color = annotation.getAuthor().getColor();

            markerBackground.setColorFilter(color, PorterDuff.Mode.MULTIPLY);

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
    public void onMarkerDragged(Marker marker, PointF newPos) {
        Annotation annotation = (Annotation) marker.getTag();
        editor.moveAnnotation(annotation, newPos);
    }

    @Override
    public void onCanvasTapped(PointF pos) {
        editor.createAnnotation(pos);
    }

}
