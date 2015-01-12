package fi.aalto.legroup.achso.playback.strategies;

import java.util.Collection;

import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.playback.AnnotationRenderService;

/**
 * Responsible for rendering a certain component of an annotation.
 *
 * @author Leo Nikkilä
 */
public abstract class AnnotationStrategy {

    protected AnnotationRenderService service;

    public void initialize(AnnotationRenderService service) {
        this.service = service;
    }

    public abstract void execute(Collection<Annotation> annotations);

    public void clear() {
        // Do nothing by default
    }

    public void release() {
        clear();

        // Don't hold onto the render service
        service = null;
    }

}
