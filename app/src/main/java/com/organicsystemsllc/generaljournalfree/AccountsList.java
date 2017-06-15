package com.organicsystemsllc.generaljournalfree;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class AccountsList extends AppCompatActivity implements OnClickListener {

    public static final String ACCOUNT_ID = "account_id";
    private static final int DELETE_ID = Menu.FIRST;
    private static final int EDIT_ID = Menu.FIRST + 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_main);

        filldata();

        ExpandableListView listView1 = (ExpandableListView) findViewById(R.id.list_accounts);
        registerForContextMenu(listView1);

        TextView empty = (TextView) findViewById(R.id.list_accounts_empty);
        listView1.setEmptyView(empty);


        //load the banner ad
        AdRequest adRequestBanner = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(getString(R.string.test_device_htc_one))
                .addTestDevice(getString(R.string.test_device_htc_desire))
                .addTestDevice(getString(R.string.test_device_nexus_7))
                .build();

        AdView adBannerView = (AdView) this.findViewById(R.id.adViewAccounts);
        adBannerView.loadAd(adRequestBanner);

        //send hit to analytics
        GeneralJournalFree app = (GeneralJournalFree) getApplication();
        Tracker tracker = app.getTracker();
        tracker.setScreenName(getString(R.string.title_activity_gl_acct_list));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        Long id = (Long) info.targetView.getTag();
        switch (item.getItemId()) {
            case DELETE_ID:
                String where = GlOpenHelper.KEY_ID + "=?";
                String[] whereArgs = new String[]{id.toString()};
                getContentResolver().delete(GLContentProvider.CONTENT_URI_ACCOUNTS, where, whereArgs);
                return true;
            case EDIT_ID:
                Intent i = new Intent(getApplicationContext(), EditAccount.class);
                i.putExtra(ACCOUNT_ID, id);
                i.putExtra(EditAccount.EDIT_MODE, true);
                startActivity(i);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            return;
        }
        menu.add(0, EDIT_ID, 0, R.string.menu_context_edit);
        menu.add(0, DELETE_ID, 0, R.string.menu_context_delete);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_accounts_list, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_crt_acct: {
                Intent j = new Intent(this, EditAccount.class);
                j.putExtra(EditAccount.EDIT_MODE, false);
                startActivity(j);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void filldata() {
        Cursor cursor = getContentResolver().query(GLContentProvider.CONTENT_URI_ACCOUNTS,
                new String[]{GlOpenHelper.KEY_ID, GlOpenHelper.FLD_ACCT_TYPE,
                        GlOpenHelper.FLD_ACCT_TITLE, GlOpenHelper.FLD_ACCT_OPEN, GlOpenHelper.FLD_ACCT_DESC},
                null, null, GlOpenHelper.FLD_ACCT_TITLE);
        String[] groupFrom = new String[]{GlOpenHelper.FLD_ACCT_TITLE, GlOpenHelper.FLD_ACCT_TYPE,
                GlOpenHelper.FLD_ACCT_OPEN, GlOpenHelper.FLD_ACCT_DESC};
        int[] groupTo = new int[]{R.id.titleView, R.id.typeView, R.id.dateView, R.id.descView};
        String[] childFrom = new String[]{};
        int[] childTo = new int[]{};
        AccountsListAdapter account =
                new AccountsListAdapter(this, cursor, R.layout.list_group_accounts, groupFrom,
                        groupTo, R.layout.list_child_accounts, childFrom, childTo);
        ExpandableListView listView1 = (ExpandableListView) findViewById(R.id.list_accounts);
        listView1.setAdapter(account);
    }

    @Override
    public void onClick(View v) {
        openContextMenu(v);

    }

}
