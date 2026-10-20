package com.aipdf.assistant.ui.home;

import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.data.repo.DocumentRepository;
import com.aipdf.assistant.pdf.FileSharing;
import com.aipdf.assistant.ui.dialog.RenameDialog;
import com.aipdf.assistant.ui.dialog.ConfirmDialog;

import java.io.File;

/** Shared context menu for a document row (used by Home and Files). */
public final class DocumentActions {

    private DocumentActions() {
    }

    public interface OnChanged {
        void changed();
    }

    public static void showMenu(Context context, DocumentEntity doc, DocumentRepository repository,
                                View anchor) {
        showMenu(context, doc, repository, anchor, null);
    }

    public static void showMenu(Context context, DocumentEntity doc, DocumentRepository repository,
                                View anchor, OnChanged onChanged) {
        PopupMenu menu = new PopupMenu(context, anchor);
        menu.inflate(R.menu.document_menu);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_rename) {
                RenameDialog.show(context, doc.name, newName ->
                        repository.rename(doc, newName, new DocumentRepository.Callback<DocumentEntity>() {
                            @Override
                            public void onResult(DocumentEntity value) {
                                UiToast.info(context, context.getString(R.string.files_rename));
                                if (onChanged != null) onChanged.changed();
                            }

                            @Override
                            public void onError(String message) {
                                UiToast.error(context, message);
                            }
                        }));
                return true;
            }
            if (id == R.id.menu_share) {
                FileSharing.share(context, new File(doc.path), context.getString(R.string.action_share));
                return true;
            }
            if (id == R.id.menu_export) {
                ExportHelper.exportCopy(context, doc, repository);
                return true;
            }
            if (id == R.id.menu_delete) {
                ConfirmDialog.show(context, context.getString(R.string.files_delete_title),
                        context.getString(R.string.files_delete_message, doc.name),
                        context.getString(R.string.action_delete),
                        () -> repository.delete(doc, new com.aipdf.assistant.data.repo.DocumentRepository.Callback<Boolean>() {
                            @Override
                            public void onResult(Boolean ok) {
                                if (ok) {
                                    UiToast.info(context, context.getString(R.string.action_delete));
                                    if (onChanged != null) onChanged.changed();
                                } else {
                                    UiToast.error(context, context.getString(R.string.files_not_deleted));
                                }
                            }

                            @Override
                            public void onError(String message) {
                                UiToast.error(context, message);
                            }
                        }));
                return true;
            }
            return false;
        });
        menu.show();
    }
}
