package com.aipdf.assistant.ui.home;

import android.content.Context;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.data.repo.DocumentRepository;
import com.aipdf.assistant.pdf.FileSharing;
import com.aipdf.assistant.util.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/** Copies a document to a user-chosen location via the Storage Access Framework. */
public final class ExportHelper {

    private ExportHelper() {
    }

    public static void exportCopy(Context context, DocumentEntity doc, DocumentRepository repository) {
        if (!(context instanceof AppCompatActivity)) {
            copyToCacheAndShare(context, doc);
            return;
        }
        AppCompatActivity activity = (AppCompatActivity) context;
        ActivityResultLauncher<String> launcher = activity.registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/pdf"), uri -> {
                    if (uri == null) return;
                    try (InputStream in = new FileInputStreamSafe(new File(doc.path)).open();
                         OutputStream out = activity.getContentResolver().openOutputStream(uri)) {
                        if (out == null) throw new Exception("No output stream");
                        byte[] buffer = new byte[32 * 1024];
                        int n;
                        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                        UiToast.info(activity, activity.getString(R.string.files_export));
                    } catch (Exception e) {
                        UiToast.error(activity, activity.getString(R.string.error_io));
                    }
                });
        launcher.launch(FileUtils.sanitizeFileName(doc.name));
    }

    private static void copyToCacheAndShare(Context context, DocumentEntity doc) {
        FileSharing.share(context, new File(doc.path), context.getString(R.string.action_share));
    }

    /** Small indirection so we can centralize file-open failures. */
    private static class FileInputStreamSafe {
        private final File file;

        FileInputStreamSafe(File file) {
            this.file = file;
        }

        InputStream open() throws Exception {
            return new java.io.FileInputStream(file);
        }
    }
}
