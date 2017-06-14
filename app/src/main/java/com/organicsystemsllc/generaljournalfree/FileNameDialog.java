package com.organicsystemsllc.generaljournalfree;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

public class FileNameDialog extends DialogFragment {

    private File mFile;
    private EditText mInput;
    private File mDir;

    public FileNameDialog() {
        super();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();


        mFile = (File) getArguments().getSerializable("file");
        mDir = mFile.getParentFile();
        mInput = (EditText) inflater.inflate(R.layout.dialog_filename, null);
        mInput.setText(mFile.getName());

        builder.setTitle(R.string.dialog_enter_filename);
        builder.setView(mInput);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strNewName = mInput.getText().toString().trim();
                File newFile = new File(mDir, strNewName);
                if (newFile.exists())
                    Toast.makeText(getActivity(), getString(R.string.toast_filename_exists), Toast.LENGTH_SHORT).show();
                else {
                    if (mFile.renameTo(newFile))
                        Toast.makeText(getActivity(), getString(R.string.toast_filename_success), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), getString(R.string.toast_filename_failure), Toast.LENGTH_SHORT).show();
                }
                return;
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), getString(R.string.toast_canceled), Toast.LENGTH_SHORT).show();
                return;
            }
        });

        return builder.create();
    }
}


