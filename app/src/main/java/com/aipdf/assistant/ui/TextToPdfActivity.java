package com.aipdf.assistant.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.repo.DocumentRepository;
import com.aipdf.assistant.databinding.ActivityTextToPdfBinding;
import com.aipdf.assistant.pdf.PdfGenerator;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.util.AppExecutors;
import com.aipdf.assistant.util.DocumentStore;

import java.io.File;

public class TextToPdfActivity extends AppCompatActivity {

    private ActivityTextToPdfBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTextToPdfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnCreate.setOnClickListener(v -> create());
    }

    private void create() {
        String title = binding.inputTitle.getText() == null ? "" : binding.inputTitle.getText().toString().trim();
        String text = binding.inputText.getText() == null ? "" : binding.inputText.getText().toString();
        if (TextUtils.isEmpty(text.trim())) {
            Toast.makeText(this, R.string.text_to_pdf_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(title)) title = "Document";

        binding.btnCreate.setEnabled(false);
        final String finalTitle = title;
        final String finalText = text;
        AppExecutors.disk(() -> {
            try {
                File out = DocumentStore.generatedFile(this, finalTitle);
                PdfGenerator generator = new PdfGenerator(this);
                PdfGenerator.Result result = generator.textToPdf(finalTitle, finalText,
                        AppSettings.getPageSize().name(),
                        AppSettings.getOrientation().name().toLowerCase(java.util.Locale.US), out);
                AppExecutors.main(() -> {
                    binding.btnCreate.setEnabled(true);
                    new DocumentRepository(this).registerGenerated(result.file, true,
                            new DocumentRepository.Callback<com.aipdf.assistant.data.entity.DocumentEntity>() {
                                @Override
                                public void onResult(com.aipdf.assistant.data.entity.DocumentEntity value) {
                                }

                                @Override
                                public void onError(String message) {
                                }
                            });
                    Toast.makeText(this, getString(R.string.preview_saved, result.file.getName()),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                AppExecutors.main(() -> {
                    binding.btnCreate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.preview_failed,
                            e.getMessage() == null ? "" : e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
