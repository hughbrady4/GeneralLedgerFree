package com.organicsystemsllc.generaljournalfree;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class JournalPreferences extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.journal_preferences);

        //send hit to analytics
        GeneralJournalFree app = (GeneralJournalFree) getApplication();
        Tracker tracker = app.getTracker();
        tracker.setScreenName(getString(R.string.title_activity_preferences));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

}
