package fi.aalto.legroup.achso.annotation.renderers;

import java.util.Collection;

import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.annotation.AnnotationRenderService;

/**
 * Responsible for rendering a certain component of an annotation.
 *
 * @author Leo Nikkilä
 */
public abstract class AnnotationRenderer {

    protected AnnotationRenderService service;

    public void initialize(AnnotationRenderService service) {
        this.service = service;
    }

    public abstract void render(Collection<Annotation> annotations);

    public void clear() {
        // Do nothing by default
    }

    public void release() {
        clear();

        // Don't hold onto the render service
        service = null;
    }

}
