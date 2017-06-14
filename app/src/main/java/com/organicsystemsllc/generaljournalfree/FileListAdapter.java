package com.organicsystemsllc.generaljournalfree;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.util.Date;

class FileListAdapter extends ArrayAdapter<File> {

    private Context context;
    private int resource;
    private int textViewResourceId;
    private File[] files;
    private OnClickListener mMenuClickListener;

    FileListAdapter(Context context, int resource,
                    int textViewResourceId, File[] objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
        this.resource = resource;
        this.textViewResourceId = textViewResourceId;
        this.files = objects;
        this.mMenuClickListener = (OnClickListener) context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        FileViewHolder holder;

        if (row == null) {
            LayoutInflater inflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(resource, parent, false);
            holder = new FileViewHolder();
            holder.txtView1 = (TextView) row.findViewById(textViewResourceId);
            holder.txtView2 = (TextView) row.findViewById(R.id.fileUpdated);
            holder.txtView3 = (TextView) row.findViewById(R.id.fileSize);
            holder.button1 = (ImageButton) row.findViewById(R.id.button_file_menu);
            row.setTag(R.id.TAG_VIEW_HOLDER, holder);
        } else {
            holder = (FileViewHolder) row.getTag(R.id.TAG_VIEW_HOLDER);
        }

        File file = files[position];
        String fileName = file.getName();
        Long lastUpdate = file.lastModified();
        holder.txtView1.setText(fileName);

        holder.txtView2.setText(DateFormat.getDateFormat(context).format(new Date(lastUpdate)));

        float kiloBytes = file.length();
        kiloBytes = kiloBytes / 1024;
        holder.txtView3.setText(String.format("%.02f", kiloBytes) + "KB");


        row.setTag(R.id.TAG_FILE, files[position]);
        holder.button1.setOnClickListener(mMenuClickListener);

        return row;
    }

    private static class FileViewHolder {
        TextView txtView1;
        TextView txtView2;
        TextView txtView3;
        ImageButton button1;
    }

}
