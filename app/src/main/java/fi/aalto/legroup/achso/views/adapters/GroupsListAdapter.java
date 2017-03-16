package fi.aalto.legroup.achso.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Group;

public class GroupsListAdapter extends ArrayAdapter<Group> {
    private ArrayList<Group> groups;
    private Context context;
    private ArrayList<UUID> videoIds;
    private  OnGroupSharedEventListener listener;

    public interface OnGroupSharedEventListener {
        void onClick(Group group, boolean isShared);
    }

    public GroupsListAdapter(Context context, int resource, ArrayList<Group> groups, ArrayList<UUID> videoIds) {
        super(context, resource, groups);
        this.context = context;
        this.videoIds = videoIds;
        this.groups = new ArrayList<Group>();
        this.groups.addAll(groups);
    }

    public class GroupHolder {
        public CheckBox name;
    }

    public void setListener(OnGroupSharedEventListener listener) {
        this.listener = listener;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        GroupHolder holder = null;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.partial_group_list_item, null);

            holder = new GroupHolder();
            holder.name = (CheckBox) convertView.findViewById(R.id.groupCheckbox);
            convertView.setTag(holder);
        } else {
            holder = (GroupHolder) convertView.getTag();
        }

        final Group group = groups.get(pos);

        holder.name.setText(group.getName());
        holder.name.setTag(group);

        for (UUID id: videoIds) {
            if (group.hasVideo(id)) {
                holder.name.setChecked(true);
                break;
            }
        }

        holder.name.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (listener != null) {
                    listener.onClick(group, b);
                }
            }
        });

        return convertView;
    }
}
