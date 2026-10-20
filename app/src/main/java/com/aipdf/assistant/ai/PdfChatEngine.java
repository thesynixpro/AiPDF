package com.aipdf.assistant.ai;

import android.content.Context;

import com.aipdf.assistant.ai.models.ChatMessage;
import com.aipdf.assistant.util.AppExecutors;

import java.util.List;

/**
 * Handles context-aware Q&A about a single PDF. The caller is responsible for
 * extracting document text (locally) and passing only relevant excerpts so the
 * context window and the user's privacy are respected.
 */
public class PdfChatEngine {

    private final Context context;

    public PdfChatEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public void ask(String documentTitle, String documentExcerpts, List<ChatMessage> history,
                    String question, AiCallback<String> callback) {
        ProviderManager providers = ProviderManager.getInstance(context);
        AiProvider provider = providers.getActiveProvider();
        if (!providers.isReady(provider)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "Provider not configured"));
            return;
        }
        String system = PromptLibrary.documentChatSystem(context, documentTitle)
                + "\n\nDOCUMENT CONTENT:\n" + truncate(documentExcerpts, 120_000);

        List<ChatMessage> messages = new java.util.ArrayList<>();
        if (history != null) messages.addAll(history);
        messages.add(ChatMessage.user(question));

        provider.chat(messages, system, new AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                AppExecutors.main(() -> callback.onSuccess(result));
            }

            @Override
            public void onError(AiError error) {
                AppExecutors.main(() -> callback.onError(error));
            }
        });
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "\n…(document excerpt truncated for context limits)…";
    }
}
