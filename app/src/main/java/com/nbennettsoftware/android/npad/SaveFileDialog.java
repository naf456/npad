package com.nbennettsoftware.android.npad;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

public class SaveFileDialog extends AppCompatDialogFragment {

    private OnSaveDialogFinished onSaveDialogFinished;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.save_msg);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(onSaveDialogFinished!=null) {
                    onSaveDialogFinished.doSave();
                }
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(onSaveDialogFinished!=null) {
                    onSaveDialogFinished.doContinue();
                }
            }
        });
        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        setCancelable(false);
        return builder.create();
    }

    interface OnSaveDialogFinished{
        void doSave();
        void doContinue();
    }

    void setOnSaveDialogFinished(OnSaveDialogFinished onSaveDialogFinished){
        this.onSaveDialogFinished = onSaveDialogFinished;
    }


}
