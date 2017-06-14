package com.organicsystemsllc.generaljournalfree;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

public class GeneralJournalFree extends Application {

    private Tracker mTracker;

    public GeneralJournalFree() {
        super();
    }

    synchronized Tracker getTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            mTracker = analytics.newTracker(R.xml.analytics);
            mTracker.enableAdvertisingIdCollection(true);
        }
        return mTracker;
    }

}
