package com.organicsystemsllc.generaljournalfree;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
//import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Calendar;

public class JournalListActivity extends AppCompatActivity
        implements LoaderCallbacks<Cursor>, OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    //private static final String TAG = "General Ledger Free";
    private static final int ACTIVITY_ACCOUNTS = 0;
    private static final int ACTIVITY_JE = 1;
    private static final int ACTIVITY_FILES = 2;
    private static final int ACTIVITY_SETTINGS = 3;
    private static final int DELETE_ID = Menu.FIRST;
    private static final int POST_ID = Menu.FIRST + 1;
    private static final int ACTIVITY_SIGN_IN = 4;
    private ExpandableListView mExpandableList;
    private InterstitialAd mInterstitial;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowCustomEnabled(true);

        //set view and title
        setContentView(R.layout.activity_journal_list);
        setTitle(R.string.title_activity_main);

        //create instruction text
        ImageSpan is2 = new ImageSpan(this, R.drawable.move, ImageSpan.ALIGN_BASELINE);
        SpannableString ss2 = new SpannableString(getString(R.string.empty_list_journal2));
        ss2.setSpan(is2, 8, 10, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        TextView empty = (TextView) findViewById(R.id.emptyList);
        empty.setText(TextUtils.concat(getString(R.string.empty_list_journal), " ", ss2));

        //set empty list view
        mExpandableList = (ExpandableListView) findViewById(R.id.expandableList);
        mExpandableList.setEmptyView(empty);

        //start loader
        getSupportLoaderManager().initLoader(0, null, this);

        registerForContextMenu(mExpandableList);

        //set the preferences
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.journal_preferences, false);

        //build ad requests
        mInterstitial = new InterstitialAd(this);
        mInterstitial.setAdUnitId(getResources().getString(R.string.ad_unit_interstitial1));
        final AdRequest adRequestFull = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(getResources().getString(R.string.test_device_htc_one))
                .addTestDevice(getResources().getString(R.string.test_device_htc_desire))
                .addTestDevice(getResources().getString(R.string.test_device_nexus_7))
                .build();
        AdRequest adRequestBanner = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(getResources().getString(R.string.test_device_htc_one))
                .addTestDevice(getResources().getString(R.string.test_device_htc_desire))
                .addTestDevice(getResources().getString(R.string.test_device_nexus_7))
                .build();

        //load ads
        AdView adBannerView = (AdView) this.findViewById(R.id.adViewMain);
        adBannerView.loadAd(adRequestBanner);

        //load interstitial
        mInterstitial.loadAd(adRequestFull);
        mInterstitial.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                mInterstitial.loadAd(adRequestFull);
            }
        });

        //send hit to analytics
        GeneralJournalFree app = (GeneralJournalFree) getApplication();
        Tracker tracker = app.getTracker();
        tracker.setScreenName(getString(R.string.title_activity_main));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());


        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUser(currentUser);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_je:
                Intent i = new Intent(this, EditJournalEntry.class);
                i.putExtra(EditJournalEntry.MODE, EditJournalEntry.ADD_MODE);
                startActivityForResult(i, ACTIVITY_JE);
                return true;
            case R.id.menu_accounts:
                Intent j = new Intent(this, AccountsList.class);
                startActivityForResult(j, ACTIVITY_ACCOUNTS);
                return true;
            case R.id.menu_export:
                Intent k = new Intent(this, ReportsList.class);
                startActivityForResult(k, ACTIVITY_FILES);
                return true;
            case R.id.menu_sign_in:
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, ACTIVITY_SIGN_IN);
                return true;
            case R.id.menu_settings:
                Intent n = new Intent(this, JournalPreferences.class);
                startActivityForResult(n, ACTIVITY_SETTINGS);
                return true;
            case R.id.menu_clear:
                ConfirmClearJournal dialog = new ConfirmClearJournal();
                dialog.show(getSupportFragmentManager(), "confirm");
                return true;
            case R.id.menu_sign_out:
                signOut();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            return;
        }
        menu.add(0, DELETE_ID, 0, R.string.menu_context_delete);
        //menu.add(0, POST_ID, 0, R.string.post);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        Long id = info.id;
        Uri address = ContentUris.withAppendedId(GLContentProvider.CONTENT_URI, id);
        switch (item.getItemId()) {
            case DELETE_ID:
                getContentResolver().delete(address, null, null);
                return true;
            case POST_ID:
                Calendar c1 = Calendar.getInstance();
                Integer date = c1.get(Calendar.YEAR) * 10000 + c1.get(Calendar.MONTH) * 100 + c1.get(Calendar.DAY_OF_MONTH);
                ContentValues postDate = new ContentValues();
                postDate.put(GlOpenHelper.FLD_POSTED_ON, date);
                getContentResolver().update(address, postDate, null, null);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        CursorLoader loader = new CursorLoader(getApplicationContext());
        loader.setUri(GLContentProvider.CONTENT_URI);
        loader.setProjection(new String[]{
                GlOpenHelper.KEY_ID,
                GlOpenHelper.FLD_DATE,
                GlOpenHelper.FLD_NARRATE,
                GlOpenHelper.FLD_POSTED_ON});
        loader.setSortOrder(GlOpenHelper.FLD_DATE);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        String[] fromChild = new String[]{};
        String[] fromGroup = new String[]{
                GlOpenHelper.FLD_DATE,
                GlOpenHelper.FLD_NARRATE};
        int[] toGroup = new int[]{R.id.dateView, R.id.narrateView};
        int[] toChild = new int[]{};
        JournalListAdapter adapter = new JournalListAdapter(this, cursor, R.layout.journal_list_group, fromGroup, toGroup,
                R.layout.journal_list_child, fromChild, toChild);
        mExpandableList.setAdapter(adapter);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        JournalListAdapter adapter = null;
        mExpandableList.setAdapter(adapter);

    }

    @Override
    public void onClick(View v) {
        Long rowId = (Long) v.getTag();
        Bundle args = new Bundle();
        args.putLong(JournalMenuDialog.JOURNAL_ID, rowId);
        JournalMenuDialog dialog = new JournalMenuDialog();
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "menu");

    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        //showProgressDialog();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            //Log.d(TAG, "signInWithCredential:success");

                            Toast.makeText(JournalListActivity.this, R.string.auth_success,
                                    Toast.LENGTH_SHORT).show();

                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            updateUser(currentUser);

                        } else {
                            // If sign in fails, display a message to the user.
                            //Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(JournalListActivity.this, R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                            updateUser(null);
                        }

                        //hideProgressDialog();

                    }
                });
    }

    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        Toast.makeText(JournalListActivity.this, "User signed out", Toast.LENGTH_SHORT).show();
                        updateUser(null);
                    }
                });
    }

    private void updateUser(FirebaseUser user) {
        TextView userTV = findViewById(R.id.text_user_name);
        if (user != null) {
            userTV.setText(getString(R.string.user_greeting) + user.getDisplayName());
        } else {
            userTV.setText(R.string.not_signed_in);
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == ACTIVITY_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed, update UI appropriately
                updateUser(null);
            }
        }

    }
}

	


