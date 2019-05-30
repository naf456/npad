package com.nbennettsoftware.android.npad;

import android.net.Uri;

class NpadDocument {

    static final String TYPE_PLAIN_TEXT = ".txt";
    static final String TYPE_NPAD_ML = ".npml";

    private Uri uri;
    private String type;

    NpadDocument(Uri documentUri, String documentType) {
        uri = documentUri;
        type = (documentType != null)? documentType : NpadDocument.TYPE_NPAD_ML;
    }

    Uri getUri(){
        return uri;
    }

    String getType(){
        return type;
    }
}
