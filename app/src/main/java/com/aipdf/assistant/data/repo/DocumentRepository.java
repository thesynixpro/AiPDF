package com.aipdf.assistant.data.repo;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.aipdf.assistant.data.AppDatabase;
import com.aipdf.assistant.data.dao.DocumentDao;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.pdf.PdfAnalyzer;
import com.aipdf.assistant.util.AppExecutors;
import com.aipdf.assistant.util.FileUtils;

import java.io.File;
import java.util.List;

public class DocumentRepository {

    public interface Callback<T> {
        void onResult(T value);

        void onError(String message);
    }

    private final Context context;
    private final DocumentDao dao;

    public DocumentRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dao = AppDatabase.get().documentDao();
    }

    public LiveData<List<DocumentEntity>> observeAll() {
        return dao.observeAll();
    }

    public LiveData<List<DocumentEntity>> observeRecent(int limit) {
        return dao.observeRecent(limit);
    }

    public DocumentDao dao() {
        return dao;
    }

    /** Copies a picked PDF into app storage and records metadata. */
    public void importDocument(Uri uri, Callback<DocumentEntity> callback) {
        AppExecutors.disk(() -> {
            try {
                String name = FileUtils.displayName(context, uri);
                File dir = FileUtils.documentsDir(context);
                File dest = FileUtils.uniqueFile(dir, name);
                FileUtils.importUriToDocuments(context, uri, dest);
                int pageCount = PdfAnalyzer.countPages(dest);
                if (pageCount <= 0) {
                    //noinspection ResultOfMethodCallIgnored
                    dest.delete();
                    AppExecutors.main(() -> callback.onError("Unable to open this document."));
                    return;
                }
                DocumentEntity existing = dao.findByPath(dest.getAbsolutePath());
                if (existing != null) {
                    AppExecutors.main(() -> callback.onResult(existing));
                    return;
                }
                DocumentEntity doc = new DocumentEntity();
                doc.name = dest.getName();
                doc.path = dest.getAbsolutePath();
                doc.sizeBytes = dest.length();
                doc.pageCount = pageCount;
                doc.addedAt = System.currentTimeMillis();
                doc.lastOpenedAt = System.currentTimeMillis();
                long id = dao.insert(doc);
                doc.id = id;
                AppExecutors.main(() -> callback.onResult(doc));
            } catch (Exception e) {
                AppExecutors.main(() -> callback.onError(e.getMessage() == null ? "Import failed" : e.getMessage()));
            }
        });
    }

    /** Registers an already-created file (e.g. AI-generated or tool output). */
    public void registerGenerated(File file, boolean generated, Callback<DocumentEntity> callback) {
        AppExecutors.disk(() -> {
            try {
                DocumentEntity existing = dao.findByPath(file.getAbsolutePath());
                if (existing != null) {
                    existing.sizeBytes = file.length();
                    existing.pageCount = PdfAnalyzer.countPages(file);
                    existing.lastOpenedAt = System.currentTimeMillis();
                    dao.update(existing);
                    AppExecutors.main(() -> callback.onResult(existing));
                    return;
                }
                DocumentEntity doc = new DocumentEntity();
                doc.name = file.getName();
                doc.path = file.getAbsolutePath();
                doc.sizeBytes = file.length();
                doc.pageCount = PdfAnalyzer.countPages(file);
                doc.generated = generated;
                doc.addedAt = System.currentTimeMillis();
                doc.lastOpenedAt = System.currentTimeMillis();
                long id = dao.insert(doc);
                doc.id = id;
                AppExecutors.main(() -> callback.onResult(doc));
            } catch (Exception e) {
                AppExecutors.main(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void rename(DocumentEntity doc, String newName, Callback<DocumentEntity> callback) {
        AppExecutors.disk(() -> {
            try {
                String safe = FileUtils.sanitizeFileName(newName);
                File old = new File(doc.path);
                File renamed = FileUtils.uniqueFile(old.getParentFile(), safe);
                if (!old.renameTo(renamed)) {
                    AppExecutors.main(() -> callback.onError("Rename failed"));
                    return;
                }
                doc.name = renamed.getName();
                doc.path = renamed.getAbsolutePath();
                dao.update(doc);
                AppExecutors.main(() -> callback.onResult(doc));
            } catch (Exception e) {
                AppExecutors.main(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void delete(DocumentEntity doc, Callback<Boolean> callback) {
        AppExecutors.disk(() -> {
            boolean ok = true;
            File file = new File(doc.path);
            if (file.exists() && !file.delete()) ok = false;
            dao.delete(doc);
            boolean finalOk = ok;
            AppExecutors.main(() -> callback.onResult(finalOk));
        });
    }

    public void markOpened(DocumentEntity doc, int page) {
        AppExecutors.disk(() -> {
            doc.lastOpenedAt = System.currentTimeMillis();
            doc.lastPage = page;
            dao.update(doc);
        });
    }

    public DocumentEntity findById(long id) {
        return dao.findById(id);
    }

    public DocumentEntity findByPath(String path) {
        return dao.findByPath(path);
    }
}
