package com.aipdf.assistant.pdf;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.aipdf.assistant.util.FileUtils;

import java.io.File;

/** Shares / opens generated PDF files via FileProvider. */
public final class FileSharing {

    private FileSharing() {
    }

    public static Uri uriFor(Context context, File file) {
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
    }

    public static void share(Context context, File file, String chooserTitle) {
        Uri uri = uriFor(context, file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(FileUtils.mimeOf(file));
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, chooserTitle));
    }

    public static void open(Context context, File file) {
        Uri uri = uriFor(context, file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, FileUtils.mimeOf(file));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            share(context, file, "Open with");
        }
    }
}
