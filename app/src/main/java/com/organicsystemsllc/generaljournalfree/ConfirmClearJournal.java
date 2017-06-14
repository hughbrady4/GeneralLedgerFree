package com.organicsystemsllc.generaljournalfree;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

public class ConfirmClearJournal extends DialogFragment {

    public ConfirmClearJournal() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_clear_text);
        builder.setCancelable(true);
        builder.setTitle(R.string.dialog_clear_title);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                ContentResolver res = getActivity().getContentResolver();
                if (res.delete(GLContentProvider.CONTENT_URI, null, null) > 0)
                    Toast.makeText(getActivity(), R.string.toast_cleared,
                            Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getActivity(), R.string.toast_clear_failed,
                            Toast.LENGTH_LONG).show();

            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Toast.makeText(getActivity(), R.string.toast_canceled,
                        Toast.LENGTH_LONG).show();
            }
        });
        return builder.create();
    }


}
