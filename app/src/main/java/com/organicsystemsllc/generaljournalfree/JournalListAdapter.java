package com.organicsystemsllc.generaljournalfree;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

class JournalListAdapter extends SimpleCursorTreeAdapter {

    private ContentResolver mResolver;
    private OnClickListener mMenuListener;
    private DateFormat mDateFormat;


    JournalListAdapter(Context context, Cursor cursor, int groupLayout,
                       String[] groupFrom, int[] groupTo, int childLayout,
                       String[] childFrom, int[] childTo) {
        super(context, cursor, groupLayout, groupFrom, groupTo, childLayout,
                childFrom, childTo);
        mResolver = context.getContentResolver();
        mMenuListener = (OnClickListener) context;
        mDateFormat = DateFormat.getDateInstance();
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        // Given the group, we return a cursor for all the children within that group
        //returns a JE header row id
        Long journo = groupCursor.getLong(0);
        String[] projection = new String[]{GlOpenHelper.KEY_ID, GlOpenHelper.FKEY_ACCT,
                GlOpenHelper.FLD_TYPE, GlOpenHelper.FLD_AMT};
        String where = GlOpenHelper.FKEY_JOURNO + "=?";
        String[] whereArgs = {journo.toString()};

        // Return a cursor to JE details for given header row id
        return mResolver.query(GLContentProvider.CONTENT_URI_DETAIL, projection, where, whereArgs, GlOpenHelper.FLD_TYPE + " desc");
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor,
                                 boolean isLastChild) {
        super.bindChildView(view, context, cursor, isLastChild);
        double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_AMT));
        String account = cursor.getString(cursor.getColumnIndexOrThrow(GlOpenHelper.FKEY_ACCT));
        String type = cursor.getString(cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_TYPE));
        DetailLineItem item = new DetailLineItem(type, amount, account);
        TextView textItem = (TextView) view.findViewById(R.id.text_journal_list_child);
        textItem.setText(item.toString());

    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor,
                                 boolean isExpanded) {
        super.bindGroupView(view, context, cursor, isExpanded);

        ImageButton ib = (ImageButton) view.findViewById(R.id.contextMenu);
        ib.setTag(cursor.getLong(cursor.getColumnIndexOrThrow(GlOpenHelper.KEY_ID)));
        ib.setOnClickListener(mMenuListener);
        ib.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_overflow));

        //get the open date from cursor and convert to Date
        TextView dateView = (TextView) view.findViewById(R.id.dateView);
        Date date = new Date(cursor.getLong((cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_DATE))));
        dateView.setText(mDateFormat.format(date));
    }


}
