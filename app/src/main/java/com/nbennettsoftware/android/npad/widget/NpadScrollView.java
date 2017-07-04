package com.nbennettsoftware.android.npad.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class NpadScrollView extends ScrollView {

    View focusableView;

    public NpadScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupClickListener();
    }

    public NpadScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupClickListener();
    }

    public NpadScrollView(Context context) {
        super(context);
        setupClickListener();
    }

    private void setupClickListener(){
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(focusableView != null) {
                    focusableView.requestFocus();
                }
            }
        });
    }

    public void setFocusedViewOnClick(View view){
        this.focusableView = view;
    }
}
