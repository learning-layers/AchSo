package fi.aalto.legroup.achso.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;

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

            holder.author = (TextView) convertView.findViewById(R.id.annotationListAuthor);
            holder.timestamp = (TextView) convertView.findViewById(R.id.annotationListTime);
            holder.content = (TextView) convertView.findViewById(R.id.annotationListContent);

            convertView.setTag(holder);
        } else {
            holder = (AnnotationHolder) convertView.getTag();
        }

        final Annotation annotation = annotations.get(pos);

        holder.author.setText(annotation.getAuthor().getName());
        holder.timestamp.setText(String.valueOf(annotation.getTime()));
        holder.content.setText(annotation.getText());

        holder.content.setOnClickListener(new View.OnClickListener() {
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
