package com.aipdf.assistant.util;

import android.content.Context;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Centralized location and naming for app-owned documents. */
public final class DocumentStore {

    private DocumentStore() {
    }

    public static File outputFile(Context context, String name) {
        File dir = FileUtils.documentsDir(context);
        return FileUtils.uniqueFile(dir, FileUtils.sanitizeFileName(name));
    }

    public static File generatedFile(Context context, String title) {
        return outputFile(context, title + ".pdf");
    }

    public static File tempFile(Context context, String name) {
        File dir = FileUtils.cacheDir(context);
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(dir, stamp + "_" + FileUtils.sanitizeFileName(name));
    }
}
