package com.aipdf.assistant.ui.reader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.AppDatabase;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.databinding.ActivityPdfReaderBinding;
import com.aipdf.assistant.pdf.PdfPageRenderer;
import com.aipdf.assistant.ui.chat.AssistantFragment;
import com.aipdf.assistant.ui.home.UiToast;
import com.aipdf.assistant.util.AppExecutors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class PdfReaderActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "doc_id";
    private static final String EXTRA_PATH = "doc_path";
    private static final String EXTRA_NAME = "doc_name";

    private ActivityPdfReaderBinding binding;
    private PdfPageRenderer renderer;
    private PdfPagerAdapter adapter;
    private long documentId;
    private String documentName;
    private boolean darkMode = false;
    private boolean fitWidth = true;

    public static Intent intentFor(Context context, long id, String path, String name) {
        Intent intent = new Intent(context, PdfReaderActivity.class);
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_NAME, name);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        documentId = getIntent().getLongExtra(EXTRA_ID, -1);
        documentName = getIntent().getStringExtra(EXTRA_NAME);
        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null) {
            finish();
            return;
        }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(documentName == null ? "" : documentName);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        File file = new File(path);
        if (!file.exists()) {
            UiToast.error(this, getString(R.string.files_import_error));
            finish();
            return;
        }

        AppExecutors.disk(() -> {
            try {
                PdfPageRenderer r = new PdfPageRenderer(file);
                AppExecutors.main(() -> setupPager(r));
            } catch (final Exception e) {
                AppExecutors.main(() -> {
                    String message = (e instanceof com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
                            || isPasswordError(e))
                            ? getString(R.string.reader_locked) : getString(R.string.reader_damaged);
                    UiToast.error(PdfReaderActivity.this, message);
                    finish();
                });
            }
        });
    }

    private boolean isPasswordError(Exception e) {
        String m = e.getMessage();
        return m != null && m.toLowerCase().contains("password");
    }

    private void setupPager(PdfPageRenderer r) {
        this.renderer = r;
        int width = getResources().getDisplayMetrics().widthPixels;
        adapter = new PdfPagerAdapter(renderer, width);
        binding.pager.setAdapter(adapter);
        binding.pager.setOffscreenPageLimit(1);
        updateIndicator(0);

        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
                if (documentId > 0) {
                    AppExecutors.disk(() -> {
                        DocumentEntity doc = AppDatabase.get().documentDao().findById(documentId);
                        if (doc != null) {
                            doc.lastPage = position;
                            doc.lastOpenedAt = System.currentTimeMillis();
                            AppDatabase.get().documentDao().update(doc);
                        }
                    });
                }
            }
        });

        binding.btnPrev.setOnClickListener(v ->
                binding.pager.setCurrentItem(Math.max(0, binding.pager.getCurrentItem() - 1), true));
        binding.btnNext.setOnClickListener(v ->
                binding.pager.setCurrentItem(Math.min(renderer.pageCount() - 1,
                        binding.pager.getCurrentItem() + 1), true));

        binding.btnFit.setOnClickListener(v -> {
            fitWidth = !fitWidth;
            UiToast.info(this, getString(fitWidth ? R.string.reader_fit_width : R.string.reader_fit_page));
        });

        binding.btnBookmark.setOnClickListener(v -> toggleBookmark(binding.pager.getCurrentItem() + 1));
        binding.btnAi.setOnClickListener(v -> {
            AssistantFragment.startWithDocument(documentName, FileDocProvider.pathOf(this, documentId));
            finish();
        });
    }

    private void updateIndicator(int position) {
        binding.pageIndicator.setText(getString(R.string.reader_page_of, position + 1, renderer.pageCount()));
    }

    private void toggleBookmark(int page) {
        if (documentId <= 0) {
            UiToast.error(this, getString(R.string.reader_bookmark));
            return;
        }
        AppExecutors.disk(() -> {
            AppDatabase db = AppDatabase.get();
            com.aipdf.assistant.data.entity.BookmarkEntity existing = db.bookmarkDao().find(documentId, page);
            if (existing != null) {
                db.bookmarkDao().deletePage(documentId, page);
                AppExecutors.main(() -> UiToast.info(this, "Bookmark removed"));
            } else {
                com.aipdf.assistant.data.entity.BookmarkEntity b =
                        new com.aipdf.assistant.data.entity.BookmarkEntity();
                b.documentId = documentId;
                b.page = page;
                b.label = "Page " + page;
                b.createdAt = System.currentTimeMillis();
                db.bookmarkDao().insert(b);
                AppExecutors.main(() -> UiToast.info(this, "Page bookmarked"));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.setDarkMode(false);
        if (renderer != null) renderer.close();
    }
}
