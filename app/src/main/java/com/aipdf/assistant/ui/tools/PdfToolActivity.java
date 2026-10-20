package com.aipdf.assistant.ui.tools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.data.repo.DocumentRepository;
import com.aipdf.assistant.databinding.ActivityPdfToolBinding;
import com.aipdf.assistant.pdf.FileSharing;
import com.aipdf.assistant.pdf.PageRangeParser;
import com.aipdf.assistant.pdf.PdfAnalyzer;
import com.aipdf.assistant.pdf.PdfOperations;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.ui.home.ToolNavigator;
import com.aipdf.assistant.ui.reader.PdfReaderActivity;
import com.aipdf.assistant.util.AppExecutors;
import com.aipdf.assistant.util.DocumentStore;
import com.aipdf.assistant.util.FileUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Dynamic tool form: builds the required inputs and runs a real PDF operation. */
public class PdfToolActivity extends AppCompatActivity {

    private static final String EXTRA_TOOL = "tool_id";

    private ActivityPdfToolBinding binding;
    private String toolId;
    private File sourcePdf;
    private final List<File> sourceList = new ArrayList<>();
    private final List<Uri> imageList = new ArrayList<>();

    private TextInputEditText pagesInput;
    private TextInputEditText passwordInput;
    private TextInputEditText watermarkInput;
    private TextInputEditText outputNameInput;

    private final ActivityResultLauncher<String[]> pickPdf =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) handlePickedPdf(uri);
            });

    private final ActivityResultLauncher<String[]> pickMultiplePdf =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris != null && !uris.isEmpty()) handlePickedMultiple(uris);
            });

    private final ActivityResultLauncher<String[]> pickImages =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris != null) handlePickedImages(uris);
            });

    public static Intent intentFor(Context context, String toolId) {
        Intent intent = new Intent(context, PdfToolActivity.class);
        intent.putExtra(EXTRA_TOOL, toolId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfToolBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolId = getIntent().getStringExtra(EXTRA_TOOL);
        if (toolId == null) {
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(toolTitle(toolId));
        buildForm();
        binding.btnRun.setOnClickListener(v -> run());
    }

    private String toolTitle(String id) {
        switch (id) {
            case ToolNavigator.TOOL_MERGE:
                return getString(R.string.tool_merge);
            case ToolNavigator.TOOL_SPLIT:
                return getString(R.string.tool_split);
            case ToolNavigator.TOOL_EXTRACT:
                return getString(R.string.tool_extract);
            case ToolNavigator.TOOL_REORDER:
                return getString(R.string.tool_reorder);
            case ToolNavigator.TOOL_ROTATE:
                return getString(R.string.tool_rotate);
            case ToolNavigator.TOOL_DELETE:
                return getString(R.string.tool_delete_pages);
            case ToolNavigator.TOOL_COMPRESS:
                return getString(R.string.tool_compress);
            case ToolNavigator.TOOL_WATERMARK:
                return getString(R.string.tool_watermark);
            case ToolNavigator.TOOL_PROTECT:
                return getString(R.string.tool_protect);
            case ToolNavigator.TOOL_UNPROTECT:
                return getString(R.string.tool_unprotect);
            case ToolNavigator.TOOL_EXTRACT_TEXT:
                return getString(R.string.tool_extract_text);
            case ToolNavigator.TOOL_IMAGES_TO_PDF:
                return getString(R.string.tool_images_to_pdf);
            default:
                return getString(R.string.tools_title);
        }
    }

    private void buildForm() {
        LinearLayout form = binding.form;
        form.removeAllViews();

        if (ToolNavigator.TOOL_IMAGES_TO_PDF.equals(toolId)) {
            addPickButton(form, getString(R.string.tool_select_images), v -> pickImages.launch(new String[]{"image/*"}));
            return;
        }

        if (ToolNavigator.TOOL_MERGE.equals(toolId)) {
            addPickButton(form, getString(R.string.tool_select_multiple), v ->
                    pickMultiplePdf.launch(new String[]{"application/pdf"}));
        } else {
            addPickButton(form, getString(R.string.tool_select_source), v ->
                    pickPdf.launch(new String[]{"application/pdf"}));
        }

        switch (toolId) {
            case ToolNavigator.TOOL_EXTRACT:
            case ToolNavigator.TOOL_DELETE:
                pagesInput = addInput(form, getString(R.string.tool_pages_hint), "1-3, 5");
                break;
            case ToolNavigator.TOOL_ROTATE:
                pagesInput = addInput(form, getString(R.string.tool_pages_hint), "1-3");
                break;
            case ToolNavigator.TOOL_REORDER:
                pagesInput = addInput(form, getString(R.string.tool_pages_hint), "3,1,2");
                break;
            case ToolNavigator.TOOL_SPLIT:
                pagesInput = addInput(form, "Pages per file", "1");
                break;
            case ToolNavigator.TOOL_WATERMARK:
                watermarkInput = addInput(form, getString(R.string.tool_watermark_text), "CONFIDENTIAL");
                break;
            case ToolNavigator.TOOL_PROTECT:
            case ToolNavigator.TOOL_UNPROTECT:
                passwordInput = addPasswordInput(form);
                break;
            default:
                break;
        }

        if (!ToolNavigator.TOOL_COMPRESS.equals(toolId)
                && !ToolNavigator.TOOL_EXTRACT_TEXT.equals(toolId)
                && !ToolNavigator.TOOL_PROTECT.equals(toolId)
                && !ToolNavigator.TOOL_UNPROTECT.equals(toolId)) {
            outputNameInput = addInput(form, getString(R.string.tool_output_name), defaultOutputName());
        }
    }

    private String defaultOutputName() {
        String base = "output";
        if (sourcePdf != null) base = sourcePdf.getName().replace(".pdf", "");
        return base + "_" + toolId + ".pdf";
    }

    private void addPickButton(LinearLayout form, String label, View.OnClickListener listener) {
        com.google.android.material.button.MaterialButton button =
                new com.google.android.material.button.MaterialButton(this);
        button.setText(label);
        button.setIconResource(R.drawable.ic_import);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        button.setLayoutParams(params);
        button.setOnClickListener(listener);
        form.addView(button);
    }

    private TextInputEditText addInput(LinearLayout form, String hint, String value) {
        TextInputLayout layout = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layout.setHint(hint);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        layout.setLayoutParams(params);
        TextInputEditText input = new TextInputEditText(this);
        input.setText(value);
        input.setMaxLines(1);
        layout.addView(input);
        form.addView(layout);
        return input;
    }

    private TextInputEditText addPasswordInput(LinearLayout form) {
        TextInputLayout layout = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layout.setHint(getString(R.string.tool_password_hint));
        layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        layout.setLayoutParams(params);
        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setMaxLines(1);
        layout.addView(input);
        form.addView(layout);
        return input;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void handlePickedPdf(Uri uri) {
        AppExecutors.disk(() -> {
            try {
                File local = copyToTemp(uri);
                AppExecutors.main(() -> {
                    sourcePdf = local;
                    toast(getString(R.string.preview_open) + ": " + local.getName());
                });
            } catch (Exception e) {
                AppExecutors.main(() -> toast(getString(R.string.files_import_error)));
            }
        });
    }

    private void handlePickedMultiple(List<Uri> uris) {
        AppExecutors.disk(() -> {
            List<File> files = new ArrayList<>();
            for (Uri uri : uris) {
                try {
                    files.add(copyToTemp(uri));
                } catch (Exception ignored) {
                }
            }
            AppExecutors.main(() -> {
                sourceList.clear();
                sourceList.addAll(files);
                toast(files.size() + " PDFs selected");
            });
        });
    }

    private void handlePickedImages(List<Uri> uris) {
        imageList.clear();
        imageList.addAll(uris);
        toast(uris.size() + " images selected");
    }

    private File copyToTemp(Uri uri) throws Exception {
        String name = FileUtils.displayName(this, uri);
        File dest = FileUtils.uniqueFile(FileUtils.cacheDir(this), name);
        FileUtils.importUriToDocuments(this, uri, dest);
        return dest;
    }

    private void run() {
        binding.btnRun.setEnabled(false);
        AppExecutors.disk(() -> {
            try {
                PdfOperations.OpResult result = execute();
                AppExecutors.main(() -> {
                    binding.btnRun.setEnabled(true);
                    onSuccess(result);
                });
            } catch (Exception e) {
                AppExecutors.main(() -> {
                    binding.btnRun.setEnabled(true);
                    toast(getString(R.string.tool_failed, e.getMessage() == null ? "" : e.getMessage()));
                });
            }
        });
    }

    private PdfOperations.OpResult execute() throws Exception {
        if (ToolNavigator.TOOL_IMAGES_TO_PDF.equals(toolId)) {
            if (imageList.isEmpty()) throw new Exception(getString(R.string.tool_select_images));
            File out = outputFile("images", ".pdf");
            com.aipdf.assistant.pdf.PdfGenerator.Result r = new com.aipdf.assistant.pdf.PdfGenerator(this).imagesToPdf(imageList,
                    AppSettings.getPageSize().name(),
                    AppSettings.getOrientation().name().toLowerCase(java.util.Locale.US), out);
            return new PdfOperations.OpResult(r.file, r.pageCount);
        }

        if (ToolNavigator.TOOL_MERGE.equals(toolId)) {
            if (sourceList.size() < 2) throw new Exception(getString(R.string.tool_pick_first));
            File out = outputFile("merged", ".pdf");
            return PdfOperations.merge(sourceList, out);
        }

        if (sourcePdf == null) throw new Exception(getString(R.string.tool_select_source));
        int total = PdfAnalyzer.countPages(sourcePdf);

        switch (toolId) {
            case ToolNavigator.TOOL_SPLIT: {
                int perFile = parseInt(text(pagesInput), 1);
                File dir = FileUtils.documentsDir(this);
                return PdfOperations.split(sourcePdf, perFile, dir);
            }
            case ToolNavigator.TOOL_EXTRACT: {
                List<Integer> pages = PageRangeParser.parse(text(pagesInput), total);
                return PdfOperations.extractPages(sourcePdf, pages, outputFile("extracted", ".pdf"));
            }
            case ToolNavigator.TOOL_DELETE: {
                List<Integer> pages = PageRangeParser.parse(text(pagesInput), total);
                return PdfOperations.deletePages(sourcePdf, pages, outputFile("deleted", ".pdf"));
            }
            case ToolNavigator.TOOL_ROTATE: {
                List<Integer> pages = PageRangeParser.parse(text(pagesInput), total);
                return PdfOperations.rotate(sourcePdf, pages, 90, outputFile("rotated", ".pdf"));
            }
            case ToolNavigator.TOOL_REORDER: {
                List<Integer> order = PageRangeParser.parse(text(pagesInput), total);
                return PdfOperations.reorder(sourcePdf, order, outputFile("reordered", ".pdf"));
            }
            case ToolNavigator.TOOL_COMPRESS: {
                File out = outputFile(sourcePdf.getName().replace(".pdf", "") + "_compressed", ".pdf");
                return PdfOperations.compress(sourcePdf, out);
            }
            case ToolNavigator.TOOL_WATERMARK: {
                String text = text(watermarkInput);
                if (TextUtils.isEmpty(text)) throw new Exception(getString(R.string.tool_watermark_text));
                return PdfOperations.addWatermark(sourcePdf, text, outputFile("watermarked", ".pdf"));
            }
            case ToolNavigator.TOOL_PROTECT: {
                String password = text(passwordInput);
                return PdfOperations.protect(sourcePdf, password, outputFile("protected", ".pdf"));
            }
            case ToolNavigator.TOOL_UNPROTECT: {
                String password = text(passwordInput);
                return PdfOperations.unprotect(sourcePdf, password, outputFile("unlocked", ".pdf"));
            }
            case ToolNavigator.TOOL_EXTRACT_TEXT: {
                String extracted = PdfAnalyzer.extractAllText(sourcePdf);
                File out = FileUtils.uniqueFile(FileUtils.documentsDir(this),
                        sourcePdf.getName().replace(".pdf", "") + ".txt");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                    fos.write(extracted.getBytes("UTF-8"));
                }
                return new PdfOperations.OpResult(out, total);
            }
            default:
                throw new Exception("Unsupported tool");
        }
    }

    private File outputFile(String prefix, String ext) {
        String name = outputNameInput != null && !text(outputNameInput).isEmpty()
                ? text(outputNameInput) : prefix + ext;
        if (!name.endsWith(ext)) name = name.replace(".pdf", "") + ext;
        return FileUtils.uniqueFile(FileUtils.documentsDir(this), FileUtils.sanitizeFileName(name));
    }

    private String text(TextInputEditText input) {
        if (input == null || input.getText() == null) return "";
        return input.getText().toString().trim();
    }

    private int parseInt(String value, int def) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return def;
        }
    }

    private void onSuccess(PdfOperations.OpResult result) {
        new DocumentRepository(this).registerGenerated(result.file, true,
                new DocumentRepository.Callback<DocumentEntity>() {
                    @Override
                    public void onResult(DocumentEntity value) {
                    }

                    @Override
                    public void onError(String message) {
                    }
                });
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tool_run)
                .setMessage(getString(R.string.tool_success, result.file.getName())
                        + "\n" + getString(R.string.files_size, FileUtils.humanSize(result.sizeBytes)))
                .setPositiveButton(R.string.action_open, (d, w) -> {
                    if (result.file.getName().endsWith(".txt")) {
                        FileSharing.share(this, result.file, getString(R.string.action_share));
                    } else {
                        startActivity(PdfReaderActivity.intentFor(this, -1,
                                result.file.getAbsolutePath(), result.file.getName()));
                    }
                })
                .setNeutralButton(R.string.action_share, (d, w) ->
                        FileSharing.share(this, result.file, getString(R.string.action_share)))
                .setNegativeButton(R.string.action_close, null)
                .show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
