package com.aipdf.assistant.ai;

import android.content.Context;
import android.text.TextUtils;

import com.aipdf.assistant.ai.models.ChatMessage;
import com.aipdf.assistant.ai.models.DocumentSpec;
import com.aipdf.assistant.ai.models.DocumentSpecParser;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the AI document workflow: determine intent, ask follow-ups when
 * needed, produce a structured DocumentSpec, and fall back to plain text when
 * the model does not return valid JSON.
 */
public class DocumentGenerator {

    private final Context context;
    private final ProviderManager providers;

    public DocumentGenerator(Context context) {
        this.context = context.getApplicationContext();
        this.providers = ProviderManager.getInstance(context);
    }

    public interface Callback {
        void onClarification(List<String> questions);

        void onDocument(DocumentSpec spec, String rawText);

        void onRawText(String rawText);

        void onError(AiError error);
    }

    public void generate(String userPrompt, int targetPages, List<ChatMessage> priorTurns, Callback callback) {
        AiProvider provider = providers.getActiveProvider();
        if (!providers.isReady(provider)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED,
                    context.getString(com.aipdf.assistant.R.string.ai_not_configured)));
            return;
        }
        String language = AppSettings.getDocumentLanguage();
        String system = PromptLibrary.documentSystemInstruction(language, targetPages);

        List<ChatMessage> messages = new ArrayList<>();
        if (priorTurns != null) messages.addAll(priorTurns);
        messages.add(ChatMessage.user("Create the following document:\n" + userPrompt));

        provider.chat(messages, system, new AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                List<String> clarifications = DocumentSpecParser.parseClarifications(result);
                if (clarifications != null) {
                    AppExecutors.main(() -> callback.onClarification(clarifications));
                    return;
                }
                DocumentSpec spec = DocumentSpecParser.parse(result);
                if (spec != null && spec.isValid()) {
                    AppExecutors.main(() -> callback.onDocument(spec, result));
                } else {
                    // The model returned prose instead of JSON. Keep it usable.
                    AppExecutors.main(() -> callback.onRawText(result));
                }
            }

            @Override
            public void onError(AiError error) {
                AppExecutors.main(() -> callback.onError(error));
            }
        });
    }

    public void regenerateFromEdited(DocumentSpec spec, Callback callback) {
        AiProvider provider = providers.getActiveProvider();
        if (!providers.isReady(provider)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "Provider not configured"));
            return;
        }
        String system = "You refine document specifications. Return only the corrected JSON schema.";
        String user = "Improve and complete this document specification. Keep the same JSON schema:\n"
                + DocumentSpecParser.toJson(spec);
        provider.chat(java.util.Collections.singletonList(ChatMessage.user(user)), system, new AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                DocumentSpec updated = DocumentSpecParser.parse(result);
                if (updated != null && updated.isValid()) {
                    AppExecutors.main(() -> callback.onDocument(updated, result));
                } else {
                    AppExecutors.main(() -> callback.onDocument(spec, result));
                }
            }

            @Override
            public void onError(AiError error) {
                AppExecutors.main(() -> callback.onError(error));
            }
        });
    }
}
