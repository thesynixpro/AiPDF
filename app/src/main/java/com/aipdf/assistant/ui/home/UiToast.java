package com.aipdf.assistant.ui.home;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public final class UiToast {

    private UiToast() {
    }

    public static void info(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void error(Context context, String message) {
        if (message == null || message.isEmpty()) {
            message = context.getString(com.aipdf.assistant.R.string.error_generic);
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
