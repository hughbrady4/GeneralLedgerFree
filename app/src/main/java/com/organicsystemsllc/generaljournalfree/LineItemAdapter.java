package com.organicsystemsllc.generaljournalfree;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

class LineItemAdapter extends ArrayAdapter<DetailLineItem> {

    private Context mContext;
    private int mResource;
    private int mTextViewId;
    private List<DetailLineItem> mObjects;
    private OnClickListener mOnClickListener;

    LineItemAdapter(Context context, int resource,
                    int textViewResourceId, List<DetailLineItem> objects) {
        super(context, resource, textViewResourceId, objects);
        mContext = context;
        mResource = resource;
        mTextViewId = textViewResourceId;
        mObjects = objects;
        mOnClickListener = (OnClickListener) context;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        LineItemHolder holder;

        if (row == null) {
            LayoutInflater inflater =
                    (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(mResource, parent, false);
            holder = new LineItemHolder();
            holder.lineItemView = (TextView) row.findViewById(mTextViewId);
            holder.deleteButton = (ImageButton) row.findViewById(R.id.button_delete_item);
            row.setTag(holder);
        } else {
            holder = (LineItemHolder) row.getTag();
        }

        DetailLineItem item = mObjects.get(position);
        holder.lineItemView.setText(item.toString());
        holder.deleteButton.setImageDrawable(mContext.getResources().getDrawable(android.R.drawable.ic_delete));
        holder.deleteButton.setTag(position);
        holder.deleteButton.setOnClickListener(mOnClickListener);

        return row;
    }

    private static class LineItemHolder {
        TextView lineItemView;
        ImageButton deleteButton;
    }

}
