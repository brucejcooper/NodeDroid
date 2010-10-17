package com.eightbitcloud.internode;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;

public class UserAndPassDialog extends Dialog {

    public UserAndPassDialog(Context context) {
        super(context);
    }

    public UserAndPassDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public UserAndPassDialog(Context context, int theme) {
        super(context, theme);
    }

}
