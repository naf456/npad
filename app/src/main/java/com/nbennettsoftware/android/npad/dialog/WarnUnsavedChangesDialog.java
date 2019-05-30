package com.nbennettsoftware.android.npad.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import com.nbennettsoftware.android.npad.R;

public class WarnUnsavedChangesDialog extends AppCompatDialogFragment {

    private OnWarningFinished onWarningFinished;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Any unsaved changes to the document will be lost.");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(onWarningFinished !=null) {
                    onWarningFinished._continue();
                }
            }
        });
        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        setCancelable(false);
        return builder.create();
    }

    public interface OnWarningFinished {
        void _continue();
    }

    public void setOnWarningFinished(OnWarningFinished onWarningFinished){
        this.onWarningFinished = onWarningFinished;
    }


}
