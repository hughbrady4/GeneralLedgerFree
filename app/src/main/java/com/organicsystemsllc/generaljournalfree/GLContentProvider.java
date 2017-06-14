package com.organicsystemsllc.generaljournalfree;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class GLContentProvider extends ContentProvider {

    public static final Uri CONTENT_URI =
            Uri.parse("content://com.organicsystemsllc.glproviderfree/generaljournal");
    public static final Uri CONTENT_URI_DETAIL =
            Uri.parse("content://com.organicsystemsllc.glproviderfree/generaljournal/detail");
    public static final Uri CONTENT_URI_ACCOUNTS =
            Uri.parse("content://com.organicsystemsllc.glproviderfree/generaljournal/accounts");
    public static final Uri URI_DETAIL_WITH_DATE =
            Uri.parse("content://com.organicsystemsllc.glproviderfree/generaljournal/detailwithdate");
    private static final int ALL_ROWS = 1;
    private static final int SINGLE_ROW = 2;
    private static final int DETAIL_ROWS = 3;
    private static final int DETAIL_ACCOUNTS = 4;
    private static final int SINGLE_ACCOUNT = 5;
    private static final UriMatcher uriMatcher;
    private static final int DETAIL_ROWS_WITH_DATE = 6;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("com.organicsystemsllc.glproviderfree", "generaljournal", ALL_ROWS);
        uriMatcher.addURI("com.organicsystemsllc.glproviderfree", "generaljournal/#", SINGLE_ROW);
        uriMatcher.addURI("com.organicsystemsllc.glproviderfree", "generaljournal/detail", DETAIL_ROWS);
        uriMatcher.addURI("com.organicsystemsllc.glproviderfree", "generaljournal/accounts", DETAIL_ACCOUNTS);
        uriMatcher.addURI("com.organicsystemsllc.glproviderfree", "generaljournal/accounts/#", SINGLE_ACCOUNT);
        uriMatcher.addURI("com.organicsystemsllc.glproviderfree", "generaljournal/detailwithdate", DETAIL_ROWS_WITH_DATE);
    }

    private GlOpenHelper mGlHelper;

    public GLContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mGlHelper.getWritableDatabase();
        String table;
        if (selection == null) {
            selection = "1";
        }
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW:
                //get header row id for delete
                String rowId = uri.getPathSegments().get(1);
                selectionArgs = new String[]{rowId};
                //first delete details
                selection = GlOpenHelper.FKEY_JOURNO + "=?";
                table = GlOpenHelper.TABLE_JDETAIL;
                db.delete(table, selection, selectionArgs);
                //set up to delete header
                selection = GlOpenHelper.KEY_ID + "=?";
                table = GlOpenHelper.TABLE_JHEADER;
                count = db.delete(table, selection, selectionArgs);
                break;
            case ALL_ROWS:
                //first delete details
                table = GlOpenHelper.TABLE_JDETAIL;
                db.delete(table, selection, selectionArgs);
                //set up to delete header
                table = GlOpenHelper.TABLE_JHEADER;
                count = db.delete(table, selection, selectionArgs);
                break;
            case DETAIL_ROWS:
                table = GlOpenHelper.TABLE_JDETAIL;
                count = db.delete(table, selection, selectionArgs);
                break;
            case DETAIL_ACCOUNTS:
                table = GlOpenHelper.TABLE_ACCOUNTS;
                count = db.delete(table, selection, selectionArgs);
                break;
            default:
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                return "vnd.android.cursor.dir/vnd.organicsystemsllc.generaljournal";
            case SINGLE_ROW:
                return "vnd.android.cursor.item/vnd.organicsystemsllc.generaljournal";
            case DETAIL_ROWS:
                return "vnd.android.cursor.dir/vnd.organicsystemsllc.generaljournal.detail";
            case DETAIL_ACCOUNTS:
                return "vnd.android.cursor.dir/vnd.organicsystemsllc.generaljournal.accounts";
            case SINGLE_ACCOUNT:
                return "vnd.android.cursor.item/vnd.organicsystemsllc.generaljournal.accounts";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mGlHelper.getWritableDatabase();
        String nullColumnHack = null;
        String table = null;

        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                table = GlOpenHelper.TABLE_JHEADER;
                break;
            case DETAIL_ROWS:
                table = GlOpenHelper.TABLE_JDETAIL;
                break;
            case DETAIL_ACCOUNTS:
                table = GlOpenHelper.TABLE_ACCOUNTS;
                break;
            case SINGLE_ACCOUNT:
                table = GlOpenHelper.TABLE_ACCOUNTS;
            default:
                break;
        }


        long id = db.insert(table, nullColumnHack, values);

        if (id > -1) {
            Uri insertedId = ContentUris.withAppendedId(uri, id);
            //getContext().getContentResolver().notifyChange(insertedId, null);
            getContext().getContentResolver().notifyChange(uri, null);
            return insertedId;
        } else {
            return null;
        }
    }

    @Override
    public boolean onCreate() {
        mGlHelper = new GlOpenHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mGlHelper.getWritableDatabase();
        String groupBy = null;
        String having = null;
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case ALL_ROWS:
                queryBuilder.setTables(GlOpenHelper.TABLE_JHEADER);
                break;
            case SINGLE_ROW:
                String rowId = uri.getPathSegments().get(1);
                queryBuilder.setTables(GlOpenHelper.TABLE_JHEADER);
                queryBuilder.appendWhere(GlOpenHelper.KEY_ID + "=" + rowId);
                break;
            case DETAIL_ROWS:
                queryBuilder.setTables(GlOpenHelper.TABLE_JDETAIL);
                break;
            case DETAIL_ACCOUNTS:
                queryBuilder.setTables(GlOpenHelper.TABLE_ACCOUNTS);
                break;
            case SINGLE_ACCOUNT:
                String acctId = uri.getPathSegments().get(2);
                queryBuilder.setTables(GlOpenHelper.TABLE_ACCOUNTS);
                queryBuilder.appendWhere(GlOpenHelper.KEY_ID + "=" + acctId);
                break;
            case DETAIL_ROWS_WITH_DATE:
                queryBuilder.setTables(GlOpenHelper.TABLE_JDETAIL + " JOIN " +
                        GlOpenHelper.TABLE_JHEADER + " ON " +
                        GlOpenHelper.TABLE_JDETAIL + "." + GlOpenHelper.FKEY_JOURNO
                        + " = " + GlOpenHelper.TABLE_JHEADER + "." + GlOpenHelper.KEY_ID);
                break;
            default:
                break;
        }

        Cursor cursor =
                queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = mGlHelper.getWritableDatabase();
        String table = null;

        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW:
                table = GlOpenHelper.TABLE_JHEADER;
                String rowId = uri.getPathSegments().get(1);
                selection = GlOpenHelper.KEY_ID + "=?";
                selectionArgs = new String[]{rowId};
                break;
            case SINGLE_ACCOUNT:
                table = GlOpenHelper.TABLE_ACCOUNTS;
                String accountId = uri.getPathSegments().get(2);
                selection = GlOpenHelper.KEY_ID + "=?";
                selectionArgs = new String[]{accountId};
                break;
            default:
                break;
        }

        int count = db.update(table, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
