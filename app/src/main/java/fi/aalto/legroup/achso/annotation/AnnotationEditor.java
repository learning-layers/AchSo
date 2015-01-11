package fi.aalto.legroup.achso.annotation;

import android.graphics.PointF;

import fi.aalto.legroup.achso.entities.Annotation;

/**
 * @author Leo Nikkilä
 */
public interface AnnotationEditor {

    /**
     * Prompt the user to create a new annotation at the specified position.
     *
     * @param position Position for the annotation
     */
    public void createAnnotation(PointF position);

    /**
     * Prompt the user to edit an annotation.
     *
     * @param annotation Annotation to edit
     */
    public void editAnnotation(Annotation annotation);

    /**
     * Move an annotation to a new position.
     *
     * @param annotation Annotation in question
     * @param position   New position for the annotation
     */
    public void moveAnnotation(Annotation annotation, PointF position);

}
