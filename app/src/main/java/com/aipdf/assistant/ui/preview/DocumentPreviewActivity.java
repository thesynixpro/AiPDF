package com.aipdf.assistant.ui.preview;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ai.DocumentGenerator;
import com.aipdf.assistant.ai.models.DocumentSpec;
import com.aipdf.assistant.ai.models.DocumentSpecParser;
import com.aipdf.assistant.data.repo.DocumentRepository;
import com.aipdf.assistant.databinding.ActivityDocumentPreviewBinding;
import com.aipdf.assistant.pdf.FileSharing;
import com.aipdf.assistant.pdf.PdfGenerator;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.ui.reader.PdfReaderActivity;
import com.aipdf.assistant.util.AppExecutors;
import com.aipdf.assistant.util.DocumentStore;

import java.io.File;

public class DocumentPreviewActivity extends AppCompatActivity {

    private static final String EXTRA_SPEC = "spec_json";

    private ActivityDocumentPreviewBinding binding;
    private DocumentSpec spec;
    private File lastOutput;

    public static Intent intentFor(Context context, String specJson) {
        Intent intent = new Intent(context, DocumentPreviewActivity.class);
        intent.putExtra(EXTRA_SPEC, specJson);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String json = getIntent().getStringExtra(EXTRA_SPEC);
        spec = DocumentSpecParser.fromJsonString(json);
        if (spec == null) {
            Toast.makeText(this, R.string.error_bad_response, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        applyDefaults(spec);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(spec.title == null ? getString(R.string.preview_title) : spec.title);

        SpecPreviewAdapter adapter = new SpecPreviewAdapter();
        binding.specList.setLayoutManager(new LinearLayoutManager(this));
        binding.specList.setAdapter(adapter);
        adapter.submit(spec);

        binding.fabExport.setOnClickListener(v -> exportPdf());
        binding.btnRegenerate.setOnClickListener(v -> regenerate());
        binding.btnEdit.setOnClickListener(v -> editTitle());
        binding.btnPreview.setOnClickListener(v -> exportPdf());
    }

    private void applyDefaults(DocumentSpec spec) {
        if (spec.pageSize == null || spec.pageSize.isEmpty()) {
            spec.pageSize = AppSettings.getPageSize().name();
        }
        if (spec.orientation == null || spec.orientation.isEmpty()) {
            spec.orientation = AppSettings.getOrientation().name().toLowerCase(java.util.Locale.US);
        }
    }

    private void exportPdf() {
        if (spec == null) return;
        binding.progress.setVisibility(View.VISIBLE);
        binding.fabExport.setEnabled(false);
        final DocumentSpec toRender = spec;
        AppExecutors.disk(() -> {
            try {
                File out = DocumentStore.generatedFile(this, toRender.title);
                PdfGenerator generator = new PdfGenerator(this);
                PdfGenerator.Result result = generator.renderToFile(toRender, out);
                AppExecutors.main(() -> {
                    binding.progress.setVisibility(View.GONE);
                    binding.fabExport.setEnabled(true);
                    lastOutput = result.file;
                    new DocumentRepository(this).registerGenerated(result.file, true,
                            new DocumentRepository.Callback<com.aipdf.assistant.data.entity.DocumentEntity>() {
                                @Override
                                public void onResult(com.aipdf.assistant.data.entity.DocumentEntity value) {
                                }

                                @Override
                                public void onError(String message) {
                                }
                            });
                    showSavedDialog(result.file, result.pageCount);
                });
            } catch (Exception e) {
                AppExecutors.main(() -> {
                    binding.progress.setVisibility(View.GONE);
                    binding.fabExport.setEnabled(true);
                    String detail = e.getMessage() == null ? "" : e.getMessage();
                    Toast.makeText(this, getString(R.string.preview_failed, detail), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showSavedDialog(File file, int pages) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.preview_export)
                .setMessage(getString(R.string.preview_saved, file.getName())
                        + "\n" + getString(R.string.files_pages, pages))
                .setPositiveButton(R.string.preview_open, (d, w) ->
                        startActivity(PdfReaderActivity.intentFor(this, -1, file.getAbsolutePath(), file.getName())))
                .setNeutralButton(R.string.preview_share, (d, w) ->
                        FileSharing.share(this, file, getString(R.string.action_share)))
                .setNegativeButton(R.string.action_close, null)
                .show();
    }

    private void regenerate() {
        if (spec == null) return;
        binding.progress.setVisibility(View.VISIBLE);
        new DocumentGenerator(this).regenerateFromEdited(spec, new DocumentGenerator.Callback() {
            @Override
            public void onClarification(java.util.List<String> questions) {
                binding.progress.setVisibility(View.GONE);
            }

            @Override
            public void onDocument(DocumentSpec updated, String rawText) {
                binding.progress.setVisibility(View.GONE);
                spec = updated;
                applyDefaults(spec);
                SpecPreviewAdapter adapter = new SpecPreviewAdapter();
                binding.specList.setAdapter(adapter);
                adapter.submit(spec);
                binding.toolbar.setTitle(spec.title);
            }

            @Override
            public void onRawText(String rawText) {
                binding.progress.setVisibility(View.GONE);
            }

            @Override
            public void onError(com.aipdf.assistant.ai.AiError error) {
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(DocumentPreviewActivity.this, error.userMessage(DocumentPreviewActivity.this),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void editTitle() {
        if (spec == null) return;
        com.aipdf.assistant.ui.dialog.RenameDialog.show(this, spec.title, newName -> {
            spec.title = newName;
            binding.toolbar.setTitle(newName);
        });
    }
}
