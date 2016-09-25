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
        public TextView text;
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
            holder.text = (TextView) convertView.findViewById(R.id.annotationListItemText);
            convertView.setTag(holder);
        } else {
            holder = (AnnotationHolder) convertView.getTag();
        }

        final Annotation annotation = annotations.get(pos);

        holder.text.setText(annotation.getTime() + " : " + annotation.getText());
        holder.text.setOnClickListener(new View.OnClickListener() {
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
