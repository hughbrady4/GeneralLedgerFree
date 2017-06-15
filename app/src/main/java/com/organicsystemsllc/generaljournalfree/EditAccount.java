package com.organicsystemsllc.generaljournalfree;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.organicsystemsllc.generaljournalfree.DatePickerFragment.DatePickerListener;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class EditAccount extends AppCompatActivity implements DatePickerListener {

    protected static final String EDIT_MODE = "edit_mode";
    protected static final String CALENDAR = "calendar";
    private static final int ACTIVITY_SETTINGS = 0;
    private Calendar mOpenCalendar = Calendar.getInstance();
    private boolean mDescriptionCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_account);

        //get analytics tracker
        GeneralJournalFree app = (GeneralJournalFree) getApplication();
        Tracker tracker = app.getTracker();

        if (getIntent().getBooleanExtra(EDIT_MODE, false)) {
            getSupportActionBar().setTitle(getString(R.string.title_activity_edit_account));
            tracker.setScreenName(getString(R.string.title_activity_edit_account));
        } else {
            getSupportActionBar().setTitle(getString(R.string.title_activity_create_account));
            tracker.setScreenName(getString(R.string.title_activity_create_account));
        }

        // capture date picker button and handle click
        ImageButton pickDate = findViewById(R.id.button_pick_date);
        pickDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putInt("year", mOpenCalendar.get(Calendar.YEAR));
                bundle.putInt("month", mOpenCalendar.get(Calendar.MONTH));
                bundle.putInt("day", mOpenCalendar.get(Calendar.DAY_OF_MONTH));
                DialogFragment datePicker = new DatePickerFragment();
                datePicker.setArguments(bundle);
                datePicker.show(getSupportFragmentManager(), "date_picker");
            }
        });

        Spinner typeEntry = findViewById(R.id.spinner_account_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.acct_types_array, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        typeEntry.setAdapter(adapter);

        //check shared preferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDescriptionCheck = sharedPrefs.getBoolean(getString(R.string.key_description_check), true);
        OnSharedPreferenceChangeListener prefChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPrefs, String key) {
                if (key.equals(getString(R.string.key_description_check))) {
                    mDescriptionCheck = sharedPrefs.getBoolean(key, true);
                }
            }
        };
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefChanged);

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra(EDIT_MODE, false))
                loadAccount();
        } else {
            mOpenCalendar = (Calendar) savedInstanceState.getSerializable(CALENDAR);
        }

        //set date display text
        updateDateDisplay();

        //load banner ad
        AdRequest adRequestBanner = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(getString(R.string.test_device_htc_one))
                .addTestDevice(getString(R.string.test_device_htc_desire))
                .addTestDevice(getString(R.string.test_device_nexus_7))
                .build();

        AdView adBannerView = this.findViewById(R.id.adViewAccountDetail);
        adBannerView.loadAd(adRequestBanner);

        //send hit to analytics
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

    }

    private void loadAccount() {

        //capture edit text widget for account title text
        EditText title = findViewById(R.id.edit_account_title);
        EditText description = findViewById(R.id.edit_account_desc);

        //get uri to account record in content provider
        long accountId = getIntent().getLongExtra(AccountsList.ACCOUNT_ID, 0);
        Uri accountUri = ContentUris.withAppendedId(GLContentProvider.CONTENT_URI_ACCOUNTS, accountId);

        //get the data
        String[] projection = new String[]{GlOpenHelper.FLD_ACCT_TITLE, GlOpenHelper.FLD_ACCT_DESC, GlOpenHelper.FLD_ACCT_TYPE, GlOpenHelper.FLD_ACCT_OPEN};
        Cursor cursor = getContentResolver().query(accountUri, projection, null, null, null);
        cursor.moveToFirst();

        //set the fields text
        title.setText(cursor.getString(cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_TITLE)));
        description.setText(cursor.getString(cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_DESC)));

        mOpenCalendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_OPEN)));

        //capture spinner widget and set drop down list to array
        String type = cursor.getString(cursor.getColumnIndexOrThrow(GlOpenHelper.FLD_ACCT_TYPE));
        int index = Arrays.asList(getResources().getStringArray(R.array.acct_types_array)).indexOf(type);
        Spinner typeEntry = findViewById(R.id.spinner_account_type);
        typeEntry.setSelection(index, true);

        cursor.close();
    }

    private boolean saveAccount() {

        EditText title = findViewById(R.id.edit_account_title);
        EditText description = findViewById(R.id.edit_account_desc);
        Spinner typeEntry = findViewById(R.id.spinner_account_type);

        if (title.getText().toString().length() == 0) {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_title_required), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (mDescriptionCheck) {
            if (description.getText().toString().length() == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_description_required), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        ContentResolver resolver = getContentResolver();
        ContentValues acctValues = new ContentValues();
        acctValues.put(GlOpenHelper.FLD_ACCT_TITLE, title.getText().toString());
        acctValues.put(GlOpenHelper.FLD_ACCT_DESC, description.getText().toString());
        acctValues.put(GlOpenHelper.FLD_ACCT_TYPE, ((TextView) typeEntry.getSelectedView()).getText().toString());
        acctValues.put(GlOpenHelper.FLD_ACCT_OPEN, mOpenCalendar.getTime().getTime());

        if (getIntent().getBooleanExtra(EDIT_MODE, false)) {
            //get uri to account record in content provider
            long accountId = getIntent().getLongExtra(AccountsList.ACCOUNT_ID, 0);
            Uri accountUri = ContentUris.withAppendedId(GLContentProvider.CONTENT_URI_ACCOUNTS, accountId);
            int count = resolver.update(accountUri, acctValues, null, null);
            if (count > 0)
                Toast.makeText(getApplicationContext(), getString(R.string.toast_account_updated), Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_update_failed), Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Uri uri = resolver.insert(GLContentProvider.CONTENT_URI_ACCOUNTS, acctValues);
            if (uri != null)
                Toast.makeText(getApplicationContext(), getString(R.string.toast_account_created), Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_add_failed), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    // updates the date in the TextView    
    private void updateDateDisplay() {
        DateFormat displayFormat = DateFormat.getDateInstance();
        TextView display = findViewById(R.id.text_acct_date_display);
        display.setText(displayFormat.format(mOpenCalendar.getTime()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_edit_account, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent l = new Intent(this, JournalPreferences.class);
                startActivityForResult(l, ACTIVITY_SETTINGS);
                return true;
            case R.id.menu_save:
                if (saveAccount()) {
                    setResult(RESULT_OK);
                    finish();
                }
                return true;
            case R.id.menu_cancel:
                Toast.makeText(getApplicationContext(), getString(R.string.toast_canceled), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(CALENDAR, mOpenCalendar);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onDateSet(int year, int month, int day) {
        mOpenCalendar.set(year, month, day);
        updateDateDisplay();
    }

}
