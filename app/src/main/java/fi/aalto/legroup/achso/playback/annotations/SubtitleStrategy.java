package fi.aalto.legroup.achso.playback.annotations;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.CharMatcher;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import fi.aalto.legroup.achso.entities.Annotation;

/**
 * Renders subtitles for textual annotations. TextViews are recycled. The number of views kept in
 * memory is equal to the maximum number of annotations with visible text on the screen
 * simultaneously.
 */
public final class SubtitleStrategy implements AnnotationRenderer.Strategy {

    // The optimal initial capacity in terms of memory usage is one, since that is the most likely
    // number of annotations on the screen at once. If this is not specified, ArrayDeque defaults
    // to 16.
    private Deque<View> views = new ArrayDeque<>(1);

    private @LayoutRes int subtitleLayout;
    private ViewGroup container;
    private LayoutInflater inflater;

    public SubtitleStrategy(@LayoutRes int subtitleLayout, ViewGroup container) {
        Context context = container.getContext();

        this.inflater = LayoutInflater.from(context);
        this.subtitleLayout = subtitleLayout;
        this.container = container;
    }

    @Override
    public void render(final List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            String annotationText = annotation.getText();

            // Don't create views for annotations with no visible text
            if (CharMatcher.INVISIBLE.matchesAllOf(annotationText)) {
                continue;
            }

            TextView view;

            // Inflate a new view or recycle a previous one if there are some
            if (views.isEmpty()) {
                view = (TextView) inflater.inflate(subtitleLayout, container, false);
            } else {
                view = (TextView) views.pop();
            }

            view.setText(annotationText);

            container.addView(view);
        }
    }

    @Override
    public void clear() {
        // Recycle the used views
        for (int index = 0; index < container.getChildCount(); index++) {
            views.add(container.getChildAt(index));
        }

        container.removeAllViews();
    }

}
