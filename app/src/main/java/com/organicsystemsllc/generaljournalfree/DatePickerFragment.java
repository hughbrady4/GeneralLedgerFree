package com.organicsystemsllc.generaljournalfree;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

public class DatePickerFragment extends DialogFragment implements OnDateSetListener {

    private DatePickerListener mPickerListener;

    public DatePickerFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker

        Bundle args = getArguments();
        int year = args.getInt("year");
        int month = args.getInt("month");
        int day = args.getInt("day");

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPickerListener = (DatePickerListener) activity;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear,
                          int dayOfMonth) {
        mPickerListener.onDateSet(year, monthOfYear, dayOfMonth);
    }

    interface DatePickerListener {
        void onDateSet(int year, int month, int day);
    }

}
