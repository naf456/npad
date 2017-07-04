package com.nbennettsoftware.android.npad.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

public class NpadEditText extends android.support.v7.widget.AppCompatEditText {

    private boolean needsSaving;

    public NpadEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupOnChangeListener();
    }

    public NpadEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupOnChangeListener();

    }

    public NpadEditText(Context context) {
        super(context);
        setupOnChangeListener();
    }

    private void setupOnChangeListener() {
        addTextChangedListener(new OnTextModifiedNeedsSavingSetter());
    }

    public void notifySave(){
        this.needsSaving=false;
        addTextChangedListener(new OnTextModifiedNeedsSavingSetter());
    }

    public boolean needsSaving(){
        return needsSaving;
    }

    private class OnTextModifiedNeedsSavingSetter implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            needsSaving = true;
            removeTextChangedListener(this);
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void afterTextChanged(Editable s) {}
    }


}
