package com.organicsystemsllc.generaljournalfree;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.ImageSpan;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import au.com.bytecode.opencsv.CSVReader;

public class ReportsList extends ActionBarActivity implements OnItemClickListener, OnClickListener {

    private static final String QUOTE = "\"";
    private static final char COMMA = ',';
    private static final int DELETE_ID = Menu.FIRST;
    private static final int SHARE_ID = Menu.FIRST + 1;
    private static final int IMPORT_ACCTS_ID = Menu.FIRST + 2;
    private static final int IMPORT_JOURN_ID = Menu.FIRST + 3;
    private static final int OPEN_ID = Menu.FIRST + 4;
    private static final int RENAME_ID = Menu.FIRST + 5;
    private File mPath;
    private BroadcastReceiver mExternalStorageReceiver;
    private boolean mExternalStorageWritable = false;
    private boolean mExternalStorageAvailable = false;
    private SpannableString ss;
    private CSVReader reader;
    //private File[] mFiles;

    private void exportAccounts() {

        if (!mExternalStorageWritable) {
            if (!mExternalStorageAvailable) {
                Toast.makeText(this, "External Storage Unavailable", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "External Storage Read-Only", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (!mPath.exists()) {
            mPath.mkdirs();
        }

        Calendar cal = Calendar.getInstance();
        String ts = Long.toString(cal.getTimeInMillis());

        try {
            File file = new File(mPath, "Accounts_" + ts + ".csv");
            file.createNewFile();
            file.setReadable(true, false);
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            //Write csv file column headers
            osw.append(QUOTE);
            osw.append(getString(R.string.acct_csv_column_1));
            osw.append(QUOTE + COMMA + QUOTE);
            osw.append(getString(R.string.acct_csv_column_2));
            osw.append(QUOTE + COMMA + QUOTE);
            osw.append(getString(R.string.acct_csv_column_3));
            osw.append(QUOTE + COMMA + QUOTE);
            osw.append(getString(R.string.acct_csv_column_4));
            osw.append(QUOTE);
            osw.append("\n");


            Cursor accounts = getContentResolver()
                    .query(GLContentProvider.CONTENT_URI_ACCOUNTS, null, null, null, GlOpenHelper.FLD_ACCT_TITLE);

            if (accounts != null) {
                if (accounts.moveToFirst()) {
                    do {
                        osw.append(QUOTE);
                        osw.append(accounts.getString(accounts.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_TITLE)));
                        osw.append(QUOTE + COMMA + QUOTE);
                        osw.append(accounts.getString(accounts.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_DESC)));
                        osw.append(QUOTE + COMMA + QUOTE);
                        osw.append(accounts.getString(accounts.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_TYPE)));
                        osw.append(QUOTE + COMMA + QUOTE);
                        Date date = new Date(accounts.getLong(accounts.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_OPEN)));
                        osw.append(DateFormat.getDateFormat(getApplicationContext()).format(date));
                        osw.append(QUOTE);
                        osw.append("\n");
                    } while (accounts.moveToNext());
                }
            }
            accounts.close();
            osw.close();
            fos.close();
            fillData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportJournal() {

        if (!mExternalStorageWritable) {
            if (!mExternalStorageAvailable) {
                Toast.makeText(this, "External Storage Unavailable", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "External Storage Read-Only", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (!mPath.exists()) {
            mPath.mkdirs();
        }

        Calendar cal = Calendar.getInstance();
        String ts = Long.toString(cal.getTimeInMillis());

        try {
            File file = new File(mPath, "Journal_" + ts + ".csv");
            file.createNewFile();
            file.setReadable(true, false);
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            Cursor headers = getContentResolver().query(GLContentProvider.CONTENT_URI, null, null, null, GlOpenHelper.FLD_DATE);

            //Write csv file column headers
            osw.append(QUOTE);
            osw.append(getString(R.string.csv_column_1));
            osw.append(QUOTE + COMMA + QUOTE);
            osw.append(getString(R.string.csv_column_2));
            osw.append(QUOTE + COMMA + QUOTE);
            osw.append(getString(R.string.csv_column_3));
            osw.append(QUOTE);
            osw.append("\n");

            if (headers != null) {
                if (headers.moveToFirst()) {
                    do {

                        String[] whereArgs = {headers.getString(headers.getColumnIndexOrThrow(GlOpenHelper.KEY_ID))};

                        // Return a cursor to JE details for given header row id
                        Cursor details = getContentResolver().query(GLContentProvider.CONTENT_URI_DETAIL, null,
                                GlOpenHelper.FKEY_JOURNO + "=?", whereArgs, GlOpenHelper.FLD_TYPE + " desc");

                        Date date = new Date(headers.getLong(headers.getColumnIndexOrThrow(GlOpenHelper.FLD_DATE)));


                        osw.append(QUOTE);
                        osw.append(DateFormat.getDateFormat(getApplicationContext()).format(date));
                        osw.append(QUOTE + COMMA + QUOTE);
                        osw.append(headers.getString(headers.getColumnIndexOrThrow(GlOpenHelper.FLD_NARRATE)));
                        osw.append(QUOTE + COMMA + QUOTE);
                        osw.append(Integer.toString(details.getCount()));
                        osw.append(QUOTE);
                        osw.append("\n");

                        if (details != null) {
                            if (details.moveToFirst()) {
                                do {
                                    osw.append(QUOTE);
                                    osw.append(details.getString(details.getColumnIndexOrThrow(GlOpenHelper.FKEY_ACCT)));
                                    osw.append(QUOTE + COMMA + QUOTE);
                                    osw.append(details.getString(details.getColumnIndexOrThrow(GlOpenHelper.FLD_AMT)));
                                    osw.append(QUOTE + COMMA + QUOTE);
                                    osw.append(details.getString(details.getColumnIndexOrThrow(GlOpenHelper.FLD_TYPE)));
                                    osw.append(QUOTE);
                                    osw.append("\n");

                                } while (details.moveToNext());
                            }
                        }
                        details.close();


                    }
                    while (headers.moveToNext());
                }
            }
            headers.close();
            osw.close();
            fos.close();
            fillData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillData() {
        FileListAdapter filesAdapter = null;
        TextView empty = (TextView) findViewById(R.id.empty_list_files);
        if (mExternalStorageAvailable) {
            if (mPath.exists()) {
                File[] files = mPath.listFiles();
                filesAdapter = new FileListAdapter(this, R.layout.list_item_file, R.id.fileName, files);
            }
        } else {
            empty.setText(TextUtils.concat(getString(R.string.sd_unavailable1), " ", ss));
        }
        ListView view = (ListView) findViewById(R.id.list_files);
        view.setAdapter(filesAdapter);
        view.setEmptyView(empty);
    }

    private void importAccounts(File file) {
        try {
            reader = new CSVReader(new FileReader(file));
            String[] nextLine;
            ContentValues glAccount = new ContentValues();

            //First read should just be CSV column headers
            reader.readNext();

            //Next line should be first account row
            while ((nextLine = reader.readNext()) != null) {
                //insert account record
                glAccount.put(GlOpenHelper.FLD_ACCT_TITLE, nextLine[0]);
                glAccount.put(GlOpenHelper.FLD_ACCT_DESC, nextLine[1]);
                glAccount.put(GlOpenHelper.FLD_ACCT_TYPE, nextLine[2]);
                Calendar cal = parseDate(nextLine[3]);
                glAccount.put(GlOpenHelper.FLD_ACCT_OPEN, cal.getTimeInMillis());
                getContentResolver().insert(GLContentProvider.CONTENT_URI_ACCOUNTS, glAccount);
            }
            Toast.makeText(this, "File import complete.", Toast.LENGTH_SHORT).show();
            Intent j = new Intent(this, AccountsList.class);
            startActivity(j);
        } catch (Exception e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Import Failed");
            builder.setMessage("File import failed, please check .csv file format. Export the accounts for a sample.");
            builder.setCancelable(true);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void importJournal(File file) {
        try {
            reader = new CSVReader(new FileReader(file));
            String[] nextLine;
            ContentValues glHeader = new ContentValues();
            ContentValues glDetail = new ContentValues();

            //First read should just be CSV column headers
            reader.readNext();

            //Next line should be first GL header row
            while ((nextLine = reader.readNext()) != null) {
                //insert journal entry header record
                Calendar cal = parseDate(nextLine[0]);
                glHeader.put(GlOpenHelper.FLD_DATE, cal.getTimeInMillis());
                glHeader.put(GlOpenHelper.FLD_YEAR, cal.get(Calendar.YEAR));
                glHeader.put(GlOpenHelper.FLD_MONTH, cal.get(Calendar.MONTH));
                glHeader.put(GlOpenHelper.FLD_DAY, cal.get(Calendar.DAY_OF_MONTH));
                glHeader.put(GlOpenHelper.FLD_NARRATE, nextLine[1]);
                int detailCount = Integer.parseInt(nextLine[2]);
                glHeader.put(GlOpenHelper.FLD_DETAIL_ITEM_COUNT, detailCount);
                Uri uri = getContentResolver().insert(GLContentProvider.CONTENT_URI, glHeader);
                String row = uri.getPathSegments().get(1);
                //insert journal entry detail records
                for (int i = 0; i < detailCount; i++) {
                    nextLine = reader.readNext();
                    double formatAmt = Double.parseDouble(nextLine[1]);
                    glDetail.put(GlOpenHelper.FKEY_ACCT, nextLine[0]);
                    glDetail.put(GlOpenHelper.FLD_AMT, formatAmt);
                    glDetail.put(GlOpenHelper.FLD_TYPE, nextLine[2]);
                    glDetail.put(GlOpenHelper.FKEY_JOURNO, row);
                    getContentResolver().insert(GLContentProvider.CONTENT_URI_DETAIL, glDetail);
                    glDetail.clear();
                }
                glHeader.clear();
            }
            Toast.makeText(this, "File import complete.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Import Failed");
            builder.setMessage("File import failed, please check .csv file format. Export the current journal for sample.");
            builder.setCancelable(true);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public void onClick(View v) {
        openContextMenu(v);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!mExternalStorageAvailable) {
            Toast.makeText(this, "External Storage Unavailable", Toast.LENGTH_SHORT).show();
            return false;
        }

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        File path = (File) info.targetView.getTag(R.id.TAG_FILE);
        Uri uri = Uri.fromFile(path);

        Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.organicsystemsllc.generaljournalfree.fileprovider", path);
        String contentUriType = getContentResolver().getType(contentUri);


        switch (item.getItemId()) {
            case DELETE_ID:
                if (!mExternalStorageWritable) {
                    Toast.makeText(this, "External Storage Read-Only", Toast.LENGTH_SHORT).show();
                    return false;
                }
                boolean rVal = path.delete();
                fillData();
                return rVal;
            case RENAME_ID:
                Bundle args = new Bundle();
                args.putSerializable("file", path);
                FileNameDialog rename = new FileNameDialog();
                rename.setArguments(args);
                rename.show(getSupportFragmentManager(), "rename");
                fillData();
                return true;
            case SHARE_ID:
                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType(contentUriType);
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.send_email_subject));
                shareIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share via..."));
                return true;
            case IMPORT_JOURN_ID:
                importJournal(path);
                return true;
            case IMPORT_ACCTS_ID:
                importAccounts(path);
                return true;
            case OPEN_ID:
                Intent openIntent = new Intent(android.content.Intent.ACTION_VIEW);
                openIntent.setDataAndType(uri, "text/rtf");
                try {
                    startActivity(openIntent);
                } catch (ActivityNotFoundException e) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Open Failed");
                    builder.setMessage("Could not locate an app to open text file.");
                    builder.setCancelable(true);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                return true;

        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Create string with embedded image for empty list message
        ImageSpan is = new ImageSpan(this, R.drawable.move, ImageSpan.ALIGN_BASELINE);
        ss = new SpannableString(getString(R.string.sd_unavailable2));
        ss.setSpan(is, 8, 10, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

        startWatchingExternalStorage();
        updateExternalStorageState();

        File root = Environment.getExternalStorageDirectory();
        mPath = new File(root, "GeneralLedgerReports");
        fillData();

        ListView fileList = (ListView) findViewById(R.id.list_files);
        registerForContextMenu(fileList);
        fileList.setOnItemClickListener(this);


        //load banner ad
        AdRequest adRequestBanner = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(getString(R.string.test_device_htc_one))
                .addTestDevice(getString(R.string.test_device_htc_desire))
                .addTestDevice(getString(R.string.test_device_nexus_7))
                .build();

        AdView adBannerView = (AdView) this.findViewById(R.id.adViewExport);
        adBannerView.loadAd(adRequestBanner);

        //send hit to analytics
        GeneralJournalFree app = (GeneralJournalFree) getApplication();
        Tracker tracker = app.getTracker();
        tracker.setScreenName(getString(R.string.title_activity_files));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, SHARE_ID, 0, R.string.menu_share);
        menu.add(0, IMPORT_JOURN_ID, 0, R.string.menu_import_journ);
        menu.add(0, IMPORT_ACCTS_ID, 0, R.string.menu_import_accts);
        menu.add(0, RENAME_ID, 0, R.string.menu_report_rename);
        menu.add(0, DELETE_ID, 0, R.string.menu_context_delete);
        menu.add(0, OPEN_ID, 0, R.string.menu_open_as_text);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_export_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopWatchingExternalStorage();
    }

    @Override
    public void onItemClick(AdapterView<?> list, View view, int position, long id) {
        File file = (File) view.getTag(R.id.TAG_FILE);
        Uri contentUri = FileProvider.getUriForFile(getApplicationContext(),
                "com.organicsystemsllc.generaljournalfree.fileprovider", file);
        Uri fileUri = Uri.fromFile(file.getAbsoluteFile());
        String mimeType = getContentResolver().getType(contentUri);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(intent, "Open with"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_report_journal:
                exportJournal();
                return true;
            case R.id.menu_add_report_accounts:
                exportAccounts();
                return true;
            case R.id.menu_export_refresh:
                fillData();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Calendar parseDate(String dateToValidate) {

        java.text.DateFormat format = DateFormat.getDateFormat(getApplicationContext());
        format.setLenient(false);
        Date date = new Date();

        try {
            //if not valid, it will throw ParseException
            date = format.parse(dateToValidate);
        } catch (ParseException e) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    private void startWatchingExternalStorage() {
        mExternalStorageReceiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        filter.addAction(Intent.ACTION_MEDIA_CHECKING);
        filter.addDataScheme("file");
        registerReceiver(mExternalStorageReceiver, filter);
    }

    private void stopWatchingExternalStorage() {
        unregisterReceiver(mExternalStorageReceiver);
    }

    private void updateExternalStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageWritable = true;
            mExternalStorageAvailable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageWritable = false;
            mExternalStorageAvailable = true;
        } else {
            mExternalStorageWritable = false;
            mExternalStorageAvailable = false;
        }
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateExternalStorageState();
        }
    }

}


