package com.aipdf.assistant.ui.dialog;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.aipdf.assistant.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static void show(Context context, String title, String message, String positive,
                            Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(positive, (d, w) -> onConfirm.run())
                .show();
    }
}
