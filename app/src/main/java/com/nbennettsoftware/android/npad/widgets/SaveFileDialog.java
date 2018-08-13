package com.nbennettsoftware.android.npad.widgets;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import com.nbennettsoftware.android.npad.R;

public class SaveFileDialog extends AppCompatDialogFragment {

    private OnSaveDialogFinished onSaveDialogFinished;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.save_dialog_msg);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(onSaveDialogFinished!=null) {
                    onSaveDialogFinished.Save();
                }
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(onSaveDialogFinished!=null) {
                    onSaveDialogFinished.doNotSave();
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

    public interface OnSaveDialogFinished{
        void Save();
        void doNotSave();
    }

    public void setOnSaveDialogFinished(OnSaveDialogFinished onSaveDialogFinished){
        this.onSaveDialogFinished = onSaveDialogFinished;
    }


}
