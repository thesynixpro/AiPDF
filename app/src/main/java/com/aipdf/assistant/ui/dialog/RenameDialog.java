package com.aipdf.assistant.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.aipdf.assistant.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public final class RenameDialog {

    public interface OnRename {
        void onRename(String newName);
    }

    private RenameDialog() {
    }

    public static void show(Context context, String currentName, OnRename callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null);
        TextInputEditText input = view.findViewById(R.id.rename_input);
        input.setText(currentName);
        input.setSelection(input.getText() != null ? input.getText().length() : 0);
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.files_rename)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    if (!value.isEmpty()) callback.onRename(value);
                })
                .show();
    }
}
