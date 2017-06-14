package com.organicsystemsllc.generaljournalfree;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

class GlOpenHelper extends SQLiteOpenHelper {

    public static final String FLD_GL_SOURCE = "gl_source";
    //Fields used in multiple tables
    static final String KEY_ID = "_id";
    static final String FLD_NARRATE = "narration";
    static final String FKEY_ACCT = "account";
    static final String FLD_AMT = "amount";
    static final String FLD_TYPE = "type";
    //Journal entry header table fields
    static final String TABLE_JHEADER = "jheader";
    static final String FLD_DATE = "trn_date";
    static final String FLD_YEAR = "year";
    static final String FLD_MONTH = "month";
    static final String FLD_DAY = "day";
    static final String FLD_POSTED_ON = "posted_on_date";
    static final String FLD_DETAIL_ITEM_COUNT = "detail_item_count";
    //Journal entry detail items table fields
    static final String TABLE_JDETAIL = "jdetail";
    static final String FKEY_JOURNO = "journo";
    //Ledger accounts table fields
    static final String TABLE_ACCOUNTS = "gl_accounts";
    static final String FLD_ACCT_TYPE = "type";
    static final String FLD_ACCT_TITLE = "title";
    static final String FLD_ACCT_DESC = "description";
    static final String FLD_ACCT_OPEN = "open_date";
    private static final String DATABASE_NAME = "gldbfree";
    private static final int DATABASE_VERSION = 2;
    private static final String FLD_UPDATE = "updated";
    private static final String FLD_ACCT_BAL = "balance";
    //Create journal entry header table SQL
    private static final String CRT_JHEADER =
            "create table " + TABLE_JHEADER + " (" +
                    KEY_ID + " integer primary key autoincrement, " +
                    FLD_DATE + " integer not null, " +
                    FLD_YEAR + " integer not null, " +
                    FLD_MONTH + " integer not null, " +
                    FLD_DAY + " integer not null, " +
                    FLD_NARRATE + " text not null, " +
                    FLD_DETAIL_ITEM_COUNT + " integer not null, " +
                    FLD_POSTED_ON + " date default null, " +
                    FLD_UPDATE + " updated timestamp not null default current_timestamp);";
    //Create journal entry detail table SQL
    private static final String CRT_JDETAIL =
            "create table " + TABLE_JDETAIL + " (" +
                    KEY_ID + " integer primary key autoincrement, " +
                    FKEY_ACCT + " text not null, " +
                    FLD_AMT + " double not null, " +
                    FLD_TYPE + " text not null, " +
                    FKEY_JOURNO + " integer not null, " +
                    FLD_UPDATE + " updated timestamp not null default current_timestamp);";
    //create accounts table SQL
    private static final String CRT_ACCOUNT =
            "create table " + TABLE_ACCOUNTS + " (" +
                    KEY_ID + " integer primary key autoincrement, " +
                    FLD_ACCT_TITLE + " text not null, " +
                    FLD_ACCT_TYPE + " text not null, " +
                    FLD_ACCT_DESC + " text, " +
                    FLD_ACCT_BAL + " double not null default 0, " +
                    FLD_ACCT_OPEN + " integer not null, " +
                    FLD_UPDATE + " updated timestamp not null default current_timestamp);";
    private Context mContext;


    GlOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CRT_JHEADER);
        db.execSQL(CRT_JDETAIL);
        db.execSQL(CRT_ACCOUNT);


        //create inital accounts
        Date open = new Date();
        String[] accounts = mContext.getResources().getStringArray(R.array.array_initial_accounts);
        ContentValues values = new ContentValues();
        int i = 0;
        while (i < accounts.length) {
            values.put(FLD_ACCT_TITLE, accounts[i++]);
            values.put(FLD_ACCT_TYPE, accounts[i++]);
            values.put(FLD_ACCT_DESC, accounts[i++]);
            values.put(FLD_ACCT_OPEN, open.getTime());
            db.insert(TABLE_ACCOUNTS, null, values);
        }


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        switch (oldVersion) {
            case 1:
                db.execSQL("CREATE TABLE jheaderv1 AS SELECT * FROM " + TABLE_JHEADER);
                db.execSQL("CREATE TABLE jdetailv1 AS SELECT * FROM " + TABLE_JDETAIL);
                db.execSQL("CREATE TABLE accountv1 AS SELECT * FROM " + TABLE_ACCOUNTS);
                db.execSQL("DROP TABLE " + TABLE_JHEADER);
                db.execSQL("DROP TABLE " + TABLE_JDETAIL);
                db.execSQL("DROP TABLE " + TABLE_ACCOUNTS);
                db.execSQL(CRT_JHEADER);
                db.execSQL(CRT_JDETAIL);
                db.execSQL(CRT_ACCOUNT);
                upgradeV1(db);
                db.execSQL("DROP TABLE jheaderv1");
                db.execSQL("DROP TABLE jdetailv1");
                db.execSQL("DROP TABLE accountv1");
            case 2:

        }

    }

    private void upgradeV1(SQLiteDatabase db) {
        //create date format for old date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setLenient(true);

        //query old headers
        Cursor headers = db.rawQuery("select * from jheaderv1", null);
        ContentValues headerValues = new ContentValues();
        ContentValues detailValues = new ContentValues();
        while (headers.moveToNext()) {
            headerValues.clear();
            //query old details
            long oldId = headers.getLong(headers.getColumnIndexOrThrow(KEY_ID));
            Cursor details = db.rawQuery("select * from jdetailv1 where journo=?", new String[]{Long.toString(oldId)});
            //populate new headers
            headerValues.put(FLD_NARRATE, headers.getString(headers.getColumnIndexOrThrow(FLD_NARRATE)));
            Date date = new Date();
            try {
                date = sdf.parse(headers.getString(headers.getColumnIndexOrThrow(FLD_DATE)));
            } catch (IllegalArgumentException e) {
                //continue;
            } catch (ParseException e) {
                //continue;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            headerValues.put(FLD_DATE, cal.getTimeInMillis());
            headerValues.put(FLD_YEAR, cal.get(Calendar.YEAR));
            headerValues.put(FLD_MONTH, cal.get(Calendar.MONTH));
            headerValues.put(FLD_DAY, cal.get(Calendar.DAY_OF_MONTH));
            headerValues.put(FLD_DETAIL_ITEM_COUNT, details.getCount());
            long newId = db.insert(TABLE_JHEADER, null, headerValues);
            while (details.moveToNext()) {
                detailValues.clear();
                detailValues.put(FKEY_JOURNO, newId);
                detailValues.put(FKEY_ACCT, details.getString(details.getColumnIndexOrThrow(FKEY_ACCT)));
                detailValues.put(FLD_AMT, details.getDouble(details.getColumnIndexOrThrow(FLD_AMT)));
                String type = details.getString(details.getColumnIndexOrThrow(FLD_TYPE));
                if (type.equalsIgnoreCase("dr"))
                    detailValues.put(FLD_TYPE, "Debit");
                else if (type.equalsIgnoreCase("cr"))
                    detailValues.put(FLD_TYPE, "Credit");
                else {
                    continue;
                }
                db.insert(TABLE_JDETAIL, null, detailValues);
            }
            details.close();

        }

        headers.close();

        Cursor accounts = db.rawQuery("select * from accountv1", null);
        ContentValues acctValues = new ContentValues();
        while (accounts.moveToNext()) {
            acctValues.clear();
            Date date = new Date();
            try {
                date = sdf.parse(accounts.getString(headers.getColumnIndexOrThrow(FLD_ACCT_OPEN)));
            } catch (IllegalArgumentException e) {
                //continue;
            } catch (ParseException e) {
                //continue;
            }

            acctValues.put(FLD_ACCT_TITLE, accounts.getString(accounts.getColumnIndexOrThrow(FLD_ACCT_TITLE)));
            acctValues.put(FLD_ACCT_DESC, accounts.getString(accounts.getColumnIndexOrThrow(FLD_ACCT_DESC)));
            acctValues.put(FLD_ACCT_TYPE, accounts.getString(accounts.getColumnIndexOrThrow(FLD_ACCT_TYPE)));
            acctValues.put(FLD_ACCT_OPEN, date.getTime());
            db.insert(TABLE_ACCOUNTS, null, acctValues);

        }
        accounts.close();

    }

}
