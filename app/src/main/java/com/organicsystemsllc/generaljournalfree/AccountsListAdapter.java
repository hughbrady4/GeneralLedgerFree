package com.organicsystemsllc.generaljournalfree;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

class AccountsListAdapter extends SimpleCursorTreeAdapter {

    private Context mContext;
    private NumberFormat mNumberFormat;
    private DateFormat mDateFormat;
    private OnClickListener mMenuClickListener;


    AccountsListAdapter(Context context, Cursor cursor, int groupLayout,
                        String[] groupFrom, int[] groupTo, int childLayout,
                        String[] childFrom, int[] childTo) {
        super(context, cursor, groupLayout, groupFrom, groupTo, childLayout,
                childFrom, childTo);

        mMenuClickListener = (OnClickListener) context;
        mContext = context;
        mNumberFormat = NumberFormat.getCurrencyInstance();
        mDateFormat = DateFormat.getDateInstance();

    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor,
                                 boolean isLastChild) {
        super.bindChildView(view, context, cursor, isLastChild);
        TextView item = (TextView) view.findViewById(R.id.text_account_list_child);
        if (isLastChild) {
            item.setText(cursor.getString(1));
        } else {
            String type = cursor.getString(cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_TYPE));
            long amount = cursor.getLong(cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_AMT));
            Date date = new Date(cursor.getLong(cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_DATE)));
            item.setText(type + " " + mNumberFormat.format(amount) + " on " + mDateFormat.format(date));
        }
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor,
                                 boolean isExpanded) {
        super.bindGroupView(view, context, cursor, isExpanded);
        //get journal entry id and set as tag
        int jndex = cursor.getColumnIndexOrThrow(GlOpenHelper.KEY_ID);
        view.setTag(cursor.getLong(jndex));

        //get the open date from cursor and convert to Date
        TextView dateView = (TextView) view.findViewById(R.id.dateView);
        Date openDate = new Date(cursor.getLong((cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_OPEN))));
        dateView.setText(mDateFormat.format(openDate));


        ImageButton button = (ImageButton) view.findViewById(R.id.button_context_menu_account);
        button.setOnClickListener(mMenuClickListener);

    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        //get account title from group cursor
        String title = groupCursor.getString(groupCursor.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_TITLE));

        String[] projection = new String[]{GlOpenHelper.TABLE_JDETAIL + "." + GlOpenHelper.KEY_ID,
                GlOpenHelper.FLD_TYPE,
                GlOpenHelper.FLD_AMT, GlOpenHelper.FLD_DATE};

        String where = GlOpenHelper.FKEY_ACCT + "=?";
        String[] whereArgs = {title};

        Cursor children = mContext.getContentResolver().query(GLContentProvider.URI_DETAIL_WITH_DATE, projection,
                where, whereArgs, GlOpenHelper.FLD_DATE);

        double debitTotal = 0;
        double creditTotal = 0;
        while (children.moveToNext()) {
            String type = children.getString(children.getColumnIndexOrThrow(GlOpenHelper.FLD_TYPE));
            if (type.equals(mContext.getString(R.string.button_toggle_debit)))
                debitTotal += children.getDouble(children.getColumnIndexOrThrow(GlOpenHelper.FLD_AMT));
            if (type.equals(mContext.getString(R.string.button_toggle_credit)))
                creditTotal += children.getDouble(children.getColumnIndexOrThrow(GlOpenHelper.FLD_AMT));
        }

        double difference = debitTotal - creditTotal;
        String balance = "Zero Balance";

        if (difference > 0) {
            balance = "DB Balance: " + mNumberFormat.format(difference);
        } else if (difference < 0) {
            balance = "CR Balance: " + mNumberFormat.format(difference * -1);
        }


        //add dummy row to end of cursor for balance
        MatrixCursor matrix = new MatrixCursor(new String[]{GlOpenHelper.KEY_ID, "AccountBalance"});
        matrix.addRow(new String[]{"-1", balance});
        Cursor[] cursors = {children, matrix};

        return new MergeCursor(cursors);
    }

}
