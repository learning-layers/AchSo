package fi.aalto.legroup.achso.views.adapters;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.sql.Time;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.Group;

public class AnnotationsListAdapter extends ArrayAdapter<Annotation> {
    private List<Annotation> annotations;
    private Context context;
    private OnAnnotationItemClickedListener listener;

    public interface OnAnnotationItemClickedListener {
        void onClick(Annotation annotation);
    }

    public AnnotationsListAdapter(Context context, int resource, List<Annotation> annotations) {
        super(context, resource, annotations);
        this.context = context;
        this.annotations = annotations;
    }

    public class AnnotationHolder {
        public LinearLayout wrapper;
        public TextView author;
        public TextView timestamp;
        public TextView content;
    }

    public void setListener(OnAnnotationItemClickedListener listener) {
        this.listener = listener;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        AnnotationHolder holder = null;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.partial_annotation_list_item, null);

            holder = new AnnotationHolder();


            holder.wrapper = (LinearLayout) convertView.findViewById(R.id.annotationListWrapper);
            holder.author = (TextView) convertView.findViewById(R.id.annotationListAuthor);
            holder.timestamp = (TextView) convertView.findViewById(R.id.annotationListTime);
            holder.content = (TextView) convertView.findViewById(R.id.annotationListContent);

            convertView.setTag(holder);
        } else {
            holder = (AnnotationHolder) convertView.getTag();
        }

        final Annotation annotation = annotations.get(pos);

        long time = annotation.getTime();
        String timestamp = String.format(Locale.US, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(time),
                TimeUnit.MILLISECONDS.toSeconds(time) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)));

        holder.author.setText(annotation.getAuthor().getName());
        holder.timestamp.setText(timestamp);
        holder.content.setText(annotation.getText());

        holder.wrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onClick(annotation);
                }
            }
        });

        return convertView;
    }
}
