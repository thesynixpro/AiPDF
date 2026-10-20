package com.aipdf.assistant.ui.reader;

import android.content.Context;

import com.aipdf.assistant.data.AppDatabase;
import com.aipdf.assistant.data.entity.DocumentEntity;

/** Resolves a document id to its on-disk path for cross-screen navigation. */
public final class FileDocProvider {

    private FileDocProvider() {
    }

    public static String pathOf(Context context, long documentId) {
        DocumentEntity doc = AppDatabase.get().documentDao().findById(documentId);
        return doc == null ? null : doc.path;
    }
}
