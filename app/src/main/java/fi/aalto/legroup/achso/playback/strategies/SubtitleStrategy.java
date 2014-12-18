package fi.aalto.legroup.achso.playback.strategies;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Annotation;

/**
 * Renders subtitles for annotations.
 */
public class SubtitleStrategy extends AnnotationStrategy {

    private LayoutInflater inflater;
    private ViewGroup container;

    private List<TextView> views = new ArrayList<>();

    public SubtitleStrategy(ViewGroup container) {
        this.inflater = LayoutInflater.from(container.getContext());
        this.container = container;
    }

    @Override
    public void execute(Collection<Annotation> annotations) {
        Iterator<TextView> viewIterator = views.iterator();

        for (Annotation annotation : annotations) {
            TextView view;

            // Recycle a previous view or create a new one if none are left
            if (viewIterator.hasNext()) {
                view = viewIterator.next();
            } else {
                view = (TextView) inflater.inflate(R.layout.subtitle, container, false);
                views.add(view);
            }

            view.setText(annotation.getText());
            view.setTextColor(annotation.getAuthor().getColor());

            container.addView(view);
        }
    }

    @Override
    public void clear() {
        container.removeAllViews();
    }

}
