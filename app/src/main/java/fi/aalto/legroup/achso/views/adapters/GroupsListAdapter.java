package fi.aalto.legroup.achso.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.entities.Group;

public class GroupsListAdapter extends ArrayAdapter<Group> {
    private ArrayList<Group> groups;
    private Context context;
    private UUID videoId;

    public GroupsListAdapter(Context context, int resource, ArrayList<Group> groups, UUID videoId) {
        super(context, resource, groups);
        this.context = context;
        this.videoId = videoId;
        this.groups = new ArrayList<Group>();
        this.groups.addAll(groups);
    }

    private class GroupHolder {
        CheckBox name;
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

        Group group = groups.get(pos);

        holder.name.setText(group.getName());
        holder.name.setTag(group);
        holder.name.setChecked(group.hasVideo(videoId));

        return convertView;
    }
}
