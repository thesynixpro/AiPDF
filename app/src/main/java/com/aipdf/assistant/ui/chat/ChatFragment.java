package com.aipdf.assistant.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ai.AiCallback;
import com.aipdf.assistant.ai.AiError;
import com.aipdf.assistant.ai.AiProvider;
import com.aipdf.assistant.ai.DocumentGenerator;
import com.aipdf.assistant.ai.PdfChatEngine;
import com.aipdf.assistant.ai.ProviderManager;
import com.aipdf.assistant.ai.models.ChatMessage;
import com.aipdf.assistant.ai.models.DocumentSpec;
import com.aipdf.assistant.ai.models.DocumentSpecParser;
import com.aipdf.assistant.data.AppDatabase;
import com.aipdf.assistant.data.entity.ConversationEntity;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.data.entity.MessageEntity;
import com.aipdf.assistant.data.repo.ChatRepository;
import com.aipdf.assistant.databinding.FragmentChatBinding;
import com.aipdf.assistant.pdf.PdfAnalyzer;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.settings.ProviderIds;
import com.aipdf.assistant.ui.preview.DocumentPreviewActivity;
import com.aipdf.assistant.ui.settings.AiSettingsActivity;
import com.aipdf.assistant.util.AppExecutors;
import com.aipdf.assistant.util.FileUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatAdapter adapter;
    private ProviderManager providers;
    private ChatRepository chatRepository;
    private DocumentGenerator generator;
    private PdfChatEngine pdfChatEngine;

    private long conversationId = -1;
    private DocumentEntity attachedDocument;
    private String pendingPrompt;
    private String pendingDocumentPath;
    private String pendingDocumentName;
    private AiProvider.Cancellable activeStream;
    private String lastUserPrompt;

    private final ActivityResultLauncher<String[]> attachLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) onDocumentAttached(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        providers = ProviderManager.getInstance(requireContext());
        chatRepository = new ChatRepository(requireContext());
        generator = new DocumentGenerator(requireContext());
        pdfChatEngine = new PdfChatEngine(requireContext());

        adapter = new ChatAdapter(new ChatAdapter.Listener() {
            @Override
            public void onCopy(ChatAdapter.Item item) {
                copyToClipboard(item.text);
            }

            @Override
            public void onGeneratePdf(ChatAdapter.Item item) {
                handleGeneratePdf(item);
            }

            @Override
            public void onRetry(ChatAdapter.Item item) {
                if (lastUserPrompt != null) sendMessage(lastUserPrompt);
            }
        });
        binding.messages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.messages.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> onSendClicked());
        binding.btnStop.setOnClickListener(v -> stopStreaming());
        binding.btnAttach.setOnClickListener(v -> attachLauncher.launch(new String[]{"application/pdf"}));
        binding.btnRemoveAttachment.setOnClickListener(v -> clearAttachment());

        buildSuggestions();
        refreshProviderBar();

        if (pendingPrompt != null) {
            binding.input.setText(pendingPrompt);
            pendingPrompt = null;
        }
        if (pendingDocumentPath != null) {
            loadAttachedDocument(pendingDocumentPath, pendingDocumentName);
            pendingDocumentPath = null;
            pendingDocumentName = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProviderBar();
    }

    public void setPendingPrompt(String prompt) {
        if (binding != null) {
            binding.input.setText(prompt);
        } else {
            pendingPrompt = prompt;
        }
    }

    public void setPendingDocument(String name, String path) {
        if (binding != null && path != null) {
            loadAttachedDocument(path, name);
        } else {
            pendingDocumentName = name;
            pendingDocumentPath = path;
        }
    }

    private void buildSuggestions() {
        binding.suggestions.removeAllViews();
        addSuggestion(getString(R.string.suggested_create_cv));
        addSuggestion(getString(R.string.suggested_business_plan));
        addSuggestion(getString(R.string.suggested_invoice));
        addSuggestion(getString(R.string.suggested_study_guide));
        if (attachedDocument != null) addSuggestion(getString(R.string.suggested_summarize));
        binding.emptyHint.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void addSuggestion(String text) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
        chip.setText(text);
        chip.setOnClickListener(v -> {
            binding.input.setText(text);
            onSendClicked();
        });
        binding.suggestions.addView(chip);
    }

    private void refreshProviderBar() {
        String activeId = providers.getActiveProviderId();
        AiProvider provider = providers.getProvider(activeId);
        boolean ready = providers.isReady(provider);
        String model = provider.getModelId();
        String label;
        if (!provider.isConfigured()) {
            label = getString(R.string.ai_provider_label, provider.getDisplayName(), getString(R.string.ai_settings_no_key));
        } else {
            label = getString(R.string.ai_provider_label, provider.getDisplayName(),
                    TextUtils.isEmpty(model) ? "no model" : model);
        }
        binding.providerLabel.setText(label);
        binding.statusDot.setBackgroundResource(ready ? R.drawable.bg_dot_success : R.drawable.bg_dot_error);
        binding.providerLabel.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AiSettingsActivity.class)));
        binding.statusDot.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AiSettingsActivity.class)));
    }

    private void onDocumentAttached(Uri uri) {
        AppExecutors.disk(() -> {
            try {
                String name = FileUtils.displayName(requireContext(), uri);
                File dir = FileUtils.documentsDir(requireContext());
                File dest = FileUtils.uniqueFile(dir, name);
                FileUtils.importUriToDocuments(requireContext(), uri, dest);
                int pages = PdfAnalyzer.countPages(dest);
                AppExecutors.main(() -> {
                    DocumentEntity doc = new DocumentEntity();
                    doc.name = dest.getName();
                    doc.path = dest.getAbsolutePath();
                    doc.sizeBytes = dest.length();
                    doc.pageCount = pages;
                    doc.addedAt = System.currentTimeMillis();
                    doc.lastOpenedAt = System.currentTimeMillis();
                    AppExecutors.disk(() -> {
                        long id = AppDatabase.get().documentDao().insert(doc);
                        doc.id = id;
                        AppExecutors.main(() -> {
                            attachedDocument = doc;
                            showAttachmentChip(doc.name);
                        });
                    });
                });
            } catch (Exception e) {
                AppExecutors.main(() -> toast(getString(R.string.files_import_error)));
            }
        });
    }

    private void loadAttachedDocument(String path, String name) {
        File file = new File(path);
        if (!file.exists()) {
            toast(getString(R.string.files_import_error));
            return;
        }
        DocumentEntity doc = new DocumentEntity();
        doc.name = name == null ? file.getName() : name;
        doc.path = path;
        doc.sizeBytes = file.length();
        doc.pageCount = PdfAnalyzer.countPages(file);
        attachedDocument = doc;
        showAttachmentChip(doc.name);
        if (adapter.getItemCount() == 0) buildSuggestions();
    }

    private void showAttachmentChip(String name) {
        binding.attachmentChip.setVisibility(View.VISIBLE);
        binding.attachmentName.setText(name);
    }

    private void clearAttachment() {
        attachedDocument = null;
        binding.attachmentChip.setVisibility(View.GONE);
    }

    private void onSendClicked() {
        String text = binding.input.getText() == null ? "" : binding.input.getText().toString().trim();
        if (text.isEmpty()) {
            toast(getString(R.string.ai_empty_input));
            return;
        }
        binding.input.setText("");
        sendMessage(text);
    }

    private void sendMessage(String userText) {
        lastUserPrompt = userText;
        AiProvider provider = providers.getActiveProvider();
        if (!providers.isReady(provider)) {
            adapter.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ERROR,
                    getString(R.string.ai_not_configured)));
            return;
        }
        ensureConversation(userText);
        adapter.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_USER, userText));
        binding.emptyHint.setVisibility(View.GONE);
        hideKeyboard();
        scrollToBottom();

        if (attachedDocument != null) {
            sendDocumentQuestion(userText, provider);
        } else {
            sendDocumentCreation(userText, provider);
        }
    }

    private void sendDocumentCreation(String userText, AiProvider provider) {
        generator.generate(userText, 0, null, new DocumentGenerator.Callback() {
            @Override
            public void onClarification(List<String> questions) {
                ChatAdapter.Item item = new ChatAdapter.Item(ChatAdapter.Item.TYPE_ASSISTANT,
                        "I need a little more information:\n\n" + joinQuestions(questions));
                item.messageId = persistMessage(ChatMessage.ROLE_ASSISTANT, item.text, false);
                adapter.add(item);
                scrollToBottom();
            }

            @Override
            public void onDocument(DocumentSpec spec, String rawText) {
                ChatAdapter.Item item = ChatAdapter.Item.document(spec, rawText);
                item.messageId = persistMessage(ChatMessage.ROLE_ASSISTANT, DocumentSpecParser.toJson(spec), true);
                adapter.add(item);
                scrollToBottom();
            }

            @Override
            public void onRawText(String rawText) {
                ChatAdapter.Item item = new ChatAdapter.Item(ChatAdapter.Item.TYPE_ASSISTANT, rawText);
                item.messageId = persistMessage(ChatMessage.ROLE_ASSISTANT, rawText, false);
                adapter.add(item);
                scrollToBottom();
            }

            @Override
            public void onError(AiError error) {
                adapter.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ERROR,
                        error.userMessage(requireContext())));
                scrollToBottom();
            }
        });
    }

    private void sendDocumentQuestion(String userText, AiProvider provider) {
        showGenerating(true, getString(R.string.ai_thinking));
        AppExecutors.disk(() -> {
            String excerpts;
            boolean scanned;
            try {
                File file = new File(attachedDocument.path);
                excerpts = PdfAnalyzer.extractAllText(file);
                scanned = PdfAnalyzer.isLikelyScanned(excerpts);
                if (excerpts.length() > 120000) excerpts = excerpts.substring(0, 120000);
            } catch (Exception e) {
                AppExecutors.main(() -> {
                    showGenerating(false, null);
                    adapter.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ERROR,
                            getString(R.string.reader_damaged)));
                    scrollToBottom();
                });
                return;
            }
            final String text = excerpts;
            final boolean isScanned = scanned;
            AppExecutors.main(() -> {
                if (isScanned) {
                    showGenerating(false, null);
                    adapter.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ASSISTANT,
                            getString(R.string.ai_ocr_warning)));
                    scrollToBottom();
                    return;
                }
                confirmUploadAndAsk(userText, text);
            });
        });
    }

    private void confirmUploadAndAsk(String userText, String excerpts) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ai_upload_consent_title)
                .setMessage(getString(R.string.ai_upload_consent_message,
                        providers.getActiveProvider().getDisplayName()))
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    showGenerating(false, null);
                    adapter.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ASSISTANT,
                            getString(R.string.action_cancel)));
                    scrollToBottom();
                })
                .setPositiveButton(R.string.action_ok, (d, w) -> askAboutDocument(userText, excerpts))
                .show();
    }

    private void askAboutDocument(String userText, String excerpts) {
        showGenerating(true, getString(R.string.ai_thinking));
        List<ChatMessage> history = recentHistory();
        pdfChatEngine.ask(attachedDocument.name, excerpts, history, userText, new AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                showGenerating(false, null);
                ChatAdapter.Item item = new ChatAdapter.Item(ChatAdapter.Item.TYPE_ASSISTANT, result);
                item.messageId = persistMessage(ChatMessage.ROLE_ASSISTANT, result, false);
                adapter.add(item);
                scrollToBottom();
            }

            @Override
            public void onError(AiError error) {
                showGenerating(false, null);
                adapter.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ERROR,
                        error.userMessage(requireContext())));
                scrollToBottom();
            }
        });
    }

    private List<ChatMessage> recentHistory() {
        List<ChatMessage> history = new ArrayList<>();
        List<ChatAdapter.Item> items = adapter.snapshot();
        int from = Math.max(0, items.size() - 6);
        for (int i = from; i < items.size(); i++) {
            ChatAdapter.Item item = items.get(i);
            if (item.type == ChatAdapter.Item.TYPE_USER) {
                history.add(ChatMessage.user(item.text));
            } else if (item.type == ChatAdapter.Item.TYPE_ASSISTANT) {
                history.add(ChatMessage.assistant(item.text));
            }
        }
        return history;
    }

    private void handleGeneratePdf(ChatAdapter.Item item) {
        DocumentSpec spec = item.spec;
        if (spec == null) {
            spec = DocumentSpecParser.parse(item.text);
        }
        if (spec == null) {
            spec = new DocumentSpec();
            spec.title = "AI document";
            spec.coverPage = false;
            spec.tableOfContents = false;
            DocumentSpec.Section section = new DocumentSpec.Section();
            section.heading = "Content";
            section.elements = new ArrayList<>();
            section.elements.add(DocumentSpec.Element.paragraph(item.text));
            spec.sections.add(section);
        }
        startActivity(DocumentPreviewActivity.intentFor(requireContext(),
                DocumentSpecParser.toJson(spec)));
    }

    private void stopStreaming() {
        if (activeStream != null) {
            activeStream.cancel();
            activeStream = null;
        }
        showGenerating(false, null);
    }

    private void showGenerating(boolean show, String label) {
        binding.generatingBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (label != null) binding.generatingText.setText(label);
    }

    private void ensureConversation(String firstMessage) {
        if (conversationId > 0) return;
        AiProvider provider = providers.getActiveProvider();
        String title = firstMessage.length() > 40 ? firstMessage.substring(0, 40) + "…" : firstMessage;
        chatRepository.createConversation(title, provider.getId(), provider.getModelId(),
                attachedDocument != null ? attachedDocument.id : -1,
                attachedDocument != null ? attachedDocument.name : null,
                id -> conversationId = id);
    }

    private long persistMessage(String role, String content, boolean isDoc) {
        if (conversationId <= 0) {
            return -1;
        }
        MessageEntity m = new MessageEntity();
        m.conversationId = conversationId;
        m.role = role;
        m.content = content;
        m.documentJson = isDoc;
        m.createdAt = System.currentTimeMillis();
        long id = chatRepository.addMessageSync(m);
        ConversationEntity c = chatRepository.findConversation(conversationId);
        if (c != null) chatRepository.updateConversation(c);
        return id;
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager cm = (android.content.ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("AI response", text == null ? "" : text));
        toast(getString(R.string.action_copy));
    }

    private void scrollToBottom() {
        binding.messages.post(() -> {
            if (adapter.getItemCount() > 0) {
                binding.messages.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && binding.input.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(binding.input.getWindowToken(), 0);
        }
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String joinQuestions(List<String> questions) {
        StringBuilder sb = new StringBuilder();
        for (String q : questions) sb.append("• ").append(q).append("\n");
        return sb.toString().trim();
    }

    public boolean hasConversation() {
        return conversationId > 0;
    }

    public long getConversationId() {
        return conversationId;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activeStream != null) activeStream.cancel();
        binding = null;
    }
}
