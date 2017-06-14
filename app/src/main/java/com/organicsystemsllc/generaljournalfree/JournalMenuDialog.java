package com.organicsystemsllc.generaljournalfree;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class JournalMenuDialog extends DialogFragment implements OnClickListener {

    protected static final String JOURNAL_ID = "journal_id";
    private Uri mAddress;

    public JournalMenuDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Long rowId = getArguments().getLong(JOURNAL_ID);
        mAddress = ContentUris.withAppendedId(GLContentProvider.CONTENT_URI, rowId);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.title_je_context_menu));
        builder.setItems(getResources().getStringArray(R.array.items_je_context_menu), this);
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        switch (which) {
            //0 maps to edit in menu
            case 0:
                Intent i = new Intent(getActivity(), EditJournalEntry.class);
                i.putExtra(EditJournalEntry.JOURNAL_ADDRESS, mAddress);
                i.putExtra(EditJournalEntry.MODE, EditJournalEntry.EDIT_MODE);
                startActivity(i);
                break;
            //1 maps to copy in menu
            case 1:
                Intent j = new Intent(getActivity(), EditJournalEntry.class);
                j.putExtra(EditJournalEntry.JOURNAL_ADDRESS, mAddress);
                j.putExtra(EditJournalEntry.MODE, EditJournalEntry.COPY_MODE);
                startActivity(j);
                break;
            //2 maps to delete in menu
            case 2:
                getActivity().getContentResolver().delete(mAddress, null, null);
                break;
            default:
                break;
        }


    }

}
