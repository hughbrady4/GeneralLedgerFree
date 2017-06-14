package com.organicsystemsllc.generaljournalfree;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.DigitsKeyListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.HitBuilders.EventBuilder;
import com.google.android.gms.analytics.Tracker;
import com.organicsystemsllc.generaljournalfree.DatePickerFragment.DatePickerListener;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;


public class EditJournalEntry extends AppCompatActivity implements DatePickerListener, OnClickListener {

    protected static final String JOURNAL_ADDRESS = "journal_address";
    protected static final String MODE = "mode";
    protected static final int EDIT_MODE = 0;
    protected static final int ADD_MODE = 1;
    protected static final int COPY_MODE = 2;
    private static final int ACTIVITY_SETTINGS = 0;
    private static final String DETAIL_LINES = "detail_lines";
    private static final String GL_DATE = "gl_date";
    private Calendar mGLDate = Calendar.getInstance();
    private ArrayList<DetailLineItem> mDetailLineItems = new ArrayList<>();
    private boolean mBalanceCheck;
    private boolean mAllowZero;
    private boolean mNarrateCheck;
    private boolean mDetailCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_edit);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //get analytics tracker
        GeneralJournalFree app = (GeneralJournalFree) getApplication();
        Tracker tracker = app.getTracker();

        //test mode - entry, edit or copy
        int mode = getIntent().getIntExtra(MODE, 1);
        switch (mode) {
            case EDIT_MODE:
                actionBar.setTitle(getString(R.string.title_activity_edit_entry));
                tracker.setScreenName(getString(R.string.title_activity_edit_entry));
                break;
            case ADD_MODE:
                actionBar.setTitle(getString(R.string.title_activity_journal_entry));
                tracker.setScreenName(getString(R.string.title_activity_journal_entry));
                break;
            case COPY_MODE:
                actionBar.setTitle(getString(R.string.title_activity_copy_journal_entry));
                tracker.setScreenName(getString(R.string.title_activity_copy_journal_entry));
                break;
            default:
                finish();
        }


        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mBalanceCheck = sharedPrefs.getBoolean(getString(R.string.key_balance), true);
        mAllowZero = sharedPrefs.getBoolean(getString(R.string.key_zeros), false);
        mNarrateCheck = sharedPrefs.getBoolean(getString(R.string.key_narrate_check), true);
        mDetailCheck = sharedPrefs.getBoolean(getString(R.string.key_detail_check), true);
        OnSharedPreferenceChangeListener spChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {


            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPrefs, String key) {
                if (key.equals(getString(R.string.key_balance))) {
                    mBalanceCheck = sharedPrefs.getBoolean(key, true);
                } else if (key.equals(getString(R.string.key_zeros))) {
                    mAllowZero = sharedPrefs.getBoolean(key, false);
                } else if (key.equals(getString(R.string.key_narrate_check))) {
                    mNarrateCheck = sharedPrefs.getBoolean(key, true);
                } else if (key.equals(getString(R.string.key_detail_check))) {
                    mDetailCheck = sharedPrefs.getBoolean(key, true);
                }
            }
        };
        sharedPrefs.registerOnSharedPreferenceChangeListener(spChanged);

        if (savedInstanceState == null) {
            if (mode == EDIT_MODE || mode == COPY_MODE)
                loadJournalEntry();
        } else {
            mDetailLineItems = savedInstanceState.getParcelableArrayList(DETAIL_LINES);
            mGLDate = (Calendar) savedInstanceState.getSerializable(GL_DATE);

        }

        setDateDisplay();
        setTotalDisplay();

        //get GL accounts cursor for account spinner
        Cursor accounts = getContentResolver().query(GLContentProvider.CONTENT_URI_ACCOUNTS,
                new String[]{GlOpenHelper.KEY_ID, GlOpenHelper.FLD_ACCT_TITLE},
                null, null,
                GlOpenHelper.FLD_ACCT_TITLE);

        // set up adapter
        String[] from = new String[]{GlOpenHelper.FLD_ACCT_TITLE};
        int[] to = new int[]{android.R.id.text1};
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.support_simple_spinner_dropdown_item, accounts, from, to, 0);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        // set the accounts list
        Spinner account = (Spinner) findViewById(R.id.spinner_account_select);
        account.setAdapter(adapter);

        // set the detail line adapter
        LineItemAdapter detailItemAdapter = new LineItemAdapter(this, R.layout.detail_line_item, R.id.text_account_title, mDetailLineItems);
        ListView detailItemsList = (ListView) findViewById(R.id.list_detail_line_items);
        detailItemsList.setAdapter(detailItemAdapter);
        detailItemsList.setEmptyView(findViewById(R.id.empty_detail_line_items));

        //set key currency keys for amount
        EditText amount = (EditText) findViewById(R.id.edit_amount);
        amount.setKeyListener(DigitsKeyListener.getInstance(true, true));

        //set add detail button click
        ImageButton addDetailLine = (ImageButton) findViewById(R.id.button_add_item);
        addDetailLine.setOnClickListener(this);

        AdRequest adRequestBanner = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(getString(R.string.test_device_htc_one))
                .addTestDevice(getString(R.string.test_device_htc_desire))
                .addTestDevice(getString(R.string.test_device_nexus_7))
                .build();

        //load ads
        AdView adBannerView = (AdView) this.findViewById(R.id.adViewEditEntry);
        adBannerView.loadAd(adRequestBanner);

        //send hit to analytics
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

    }

    private void loadJournalEntry() {
        //get the row address from intent and then query database
        Uri address = getIntent().getParcelableExtra(JOURNAL_ADDRESS);
        String[] projection = new String[]{
                GlOpenHelper.FLD_DATE, GlOpenHelper.FLD_NARRATE};
        Cursor header = getContentResolver().query(address, projection, null, null, null);
        header.moveToFirst();

        //set the explanation
        EditText narrate = (EditText) findViewById(R.id.edit_narration);
        narrate.setText(header.getString(header.getColumnIndexOrThrow(GlOpenHelper.FLD_NARRATE)));

        //set the GL date
        mGLDate.setTimeInMillis(header.getLong(header.getColumnIndexOrThrow(GlOpenHelper.FLD_DATE)));

        String lastPath = address.getLastPathSegment();

        String[] detailColumns = new String[]{GlOpenHelper.FKEY_ACCT,
                GlOpenHelper.FLD_TYPE, GlOpenHelper.FLD_AMT};
        String where = GlOpenHelper.FKEY_JOURNO + "=?";
        String[] whereArgs = {lastPath};

        Cursor details = getContentResolver()
                .query(GLContentProvider.CONTENT_URI_DETAIL, detailColumns, where, whereArgs, GlOpenHelper.FLD_TYPE + " desc");

        while (details.moveToNext()) {
            String strType = details.getString(details.getColumnIndexOrThrow(GlOpenHelper.FLD_TYPE));
            String strAcct = details.getString(details.getColumnIndexOrThrow(GlOpenHelper.FKEY_ACCT));
            double amount = details.getDouble(details.getColumnIndexOrThrow(GlOpenHelper.FLD_AMT));
            DetailLineItem lineItem = new DetailLineItem(strType, amount, strAcct);
            mDetailLineItems.add(lineItem);
        }

        header.close();
        details.close();

    }

    private boolean saveEntry() {
        if (!validateJournalEntry()) {
            return false;
        }

        //get activity mode
        int mode = getIntent().getIntExtra(MODE, 1);

        //get analytics tracker
        GeneralJournalFree app = (GeneralJournalFree) getApplication();
        Tracker tracker = app.getTracker();

        //create tracking event
        EventBuilder event = new HitBuilders.EventBuilder();
        switch (mode) {
            case EDIT_MODE:
                event.setCategory("Edit Entry");
                break;
            case ADD_MODE:
                event.setCategory("Add Entry");
                break;
            case COPY_MODE:
                event.setCategory("Copy Entry");
                break;
            default:
                event.setCategory("Invalid Mode");
                break;
        }
        event.setAction("Save");
        tracker.send(event.build());

        EditText narrate = (EditText) findViewById(R.id.edit_narration);
        ContentValues glHeader = new ContentValues();
        glHeader.put(GlOpenHelper.FLD_DATE, mGLDate.getTimeInMillis());
        glHeader.put(GlOpenHelper.FLD_NARRATE, narrate.getText().toString());
        glHeader.put(GlOpenHelper.FLD_YEAR, mGLDate.get(Calendar.YEAR));
        glHeader.put(GlOpenHelper.FLD_MONTH, mGLDate.get(Calendar.MONTH));
        glHeader.put(GlOpenHelper.FLD_DAY, mGLDate.get(Calendar.DAY_OF_MONTH));
        glHeader.put(GlOpenHelper.FLD_DETAIL_ITEM_COUNT, mDetailLineItems.size());

        //insert or update header, depending on mode
        Uri address;
        String row;
        switch (mode) {
            case EDIT_MODE:
                address = getIntent().getParcelableExtra(JOURNAL_ADDRESS);
                int count = getContentResolver().update(address, glHeader, null, null);
                if (count > 0)
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_journal_updated), Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_update_failed), Toast.LENGTH_SHORT).show();
                    return false;
                }
                //delete existing details
                row = address.getPathSegments().get(1);
                getContentResolver().delete(GLContentProvider.CONTENT_URI_DETAIL,
                        GlOpenHelper.FKEY_JOURNO + "=?", new String[]{row});
                break;
            case ADD_MODE:
                address = getContentResolver().insert(GLContentProvider.CONTENT_URI, glHeader);
                if (address != null)
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_entry_created), Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_add_failed), Toast.LENGTH_SHORT).show();
                    return false;
                }
                row = address.getPathSegments().get(1);
                break;
            case COPY_MODE:
                address = getContentResolver().insert(GLContentProvider.CONTENT_URI, glHeader);
                if (address != null)
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_journal_copied), Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_copy_failed), Toast.LENGTH_SHORT).show();
                    return false;
                }
                row = address.getPathSegments().get(1);
                break;
            default:
                return false;
        }


        //insert journal entry detail records
        ContentValues glDetail = new ContentValues();
        for (DetailLineItem item : mDetailLineItems) {
            glDetail.put(GlOpenHelper.FKEY_ACCT, item.getAccount());
            glDetail.put(GlOpenHelper.FLD_AMT, item.getAmount());
            glDetail.put(GlOpenHelper.FLD_TYPE, item.getType());
            glDetail.put(GlOpenHelper.FKEY_JOURNO, row);
            getContentResolver().insert(GLContentProvider.CONTENT_URI_DETAIL, glDetail);
            glDetail.clear();
        }


        return true;
    }

    private boolean validateJournalEntry() {

        if (mDetailCheck) {
            if (mDetailLineItems.size() == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_no_details), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        double creditTotal = 0;
        double debitTotal = 0;

        for (DetailLineItem item : mDetailLineItems) {
            if (item.getType().equalsIgnoreCase("debit")) {
                debitTotal += item.getAmount();
            } else if (item.getType().equalsIgnoreCase("credit")) {
                creditTotal += item.getAmount();
            }
        }

        if (mBalanceCheck) {
            if (debitTotal != creditTotal) {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_out_of_balance), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (mNarrateCheck) {
            EditText narrate = (EditText) findViewById(R.id.edit_narration);
            if (narrate.getText().toString().length() == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_narration_required), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void setTotalDisplay() {
        double creditTotal = 0;
        double debitTotal = 0;

        for (DetailLineItem item : mDetailLineItems) {
            if (item.getType().equalsIgnoreCase("debit")) {
                debitTotal += item.getAmount();
            } else if (item.getType().equalsIgnoreCase("credit")) {
                creditTotal += item.getAmount();
            }
        }

        String debits = NumberFormat.getCurrencyInstance().format(debitTotal);
        String credits = NumberFormat.getCurrencyInstance().format(creditTotal);

        TextView text = (TextView) findViewById(R.id.text_total_display);
        text.setText(getString(R.string.text_debit_total) + debits + " " +
                getString(R.string.text_credit_total) + credits);
    }

    private void setDateDisplay() {
        DateFormat displayFormat = DateFormat.getDateInstance();
        TextView text = (TextView) findViewById(R.id.text_gldate_display);
        text.setText(displayFormat.format(mGLDate.getTime()));
    }

    public void onPickGLDate(View view) {
        Bundle args = new Bundle();
        args.putInt("year", mGLDate.get(Calendar.YEAR));
        args.putInt("month", mGLDate.get(Calendar.MONTH));
        args.putInt("day", mGLDate.get(Calendar.DAY_OF_MONTH));
        DialogFragment pick = new DatePickerFragment();
        pick.setArguments(args);
        pick.show(getSupportFragmentManager(), "datePicker");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_journal_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_save:
                if (saveEntry())
                    finish();
                return true;
            case R.id.menu_cancel:
                Toast.makeText(getApplicationContext(), getString(R.string.toast_canceled), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
                return true;
            case R.id.menu_settings:
                Intent l = new Intent(this, JournalPreferences.class);
                startActivityForResult(l, ACTIVITY_SETTINGS);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDateSet(int year, int month, int day) {
        mGLDate.set(year, month, day);
        setDateDisplay();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_delete_item:
                Integer position = (Integer) v.getTag();
                mDetailLineItems.remove(position.intValue());
                break;
            case R.id.button_add_item:
                Button buttonType = (Button) findViewById(R.id.button_transaction_type);
                String type = buttonType.getText().toString();
                Spinner spinnerAccount = (Spinner) findViewById(R.id.spinner_account_select);
                TextView selected = (TextView) spinnerAccount.getSelectedView();
                String account = selected.getText().toString();
                double amount;
                try {
                    EditText editAmount = (EditText) findViewById(R.id.edit_amount);
                    amount = Double.parseDouble(editAmount.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_invalid_amt), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mAllowZero && amount == 0) {
                    Toast.makeText(getApplicationContext(), R.string.toast_zero_dollar, Toast.LENGTH_SHORT).show();
                    return;
                }

                DetailLineItem lineItem = new DetailLineItem(type, amount, account);
                mDetailLineItems.add(lineItem);
                break;
        }
        LineItemAdapter detailItemAdapter = new LineItemAdapter(this, R.layout.detail_line_item, R.id.text_account_title, mDetailLineItems);
        ListView detailItemsList = (ListView) findViewById(R.id.list_detail_line_items);
        detailItemsList.setAdapter(detailItemAdapter);
        setTotalDisplay();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(DETAIL_LINES, mDetailLineItems);
        outState.putSerializable(GL_DATE, mGLDate);
        super.onSaveInstanceState(outState);
    }

}
