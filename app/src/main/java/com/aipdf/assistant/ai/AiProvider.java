package com.aipdf.assistant.ai;

import com.aipdf.assistant.ai.models.AiModel;
import com.aipdf.assistant.ai.models.ChatMessage;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provider-agnostic AI client. Implementations keep their own endpoints,
 * auth schemes, request bodies and response parsing.
 */
public interface AiProvider {

    String getId();

    String getDisplayName();

    /** True when an API key is available for this provider. */
    boolean isConfigured();

    /** Saves the API key for this provider (secure storage). */
    void setApiKey(String apiKey);

    String getApiKey();

    void clearApiKey();

    /** Selected model id for this provider, or null. */
    String getModelId();

    void setModelId(String modelId);

    /** Validates the key by calling the provider's model-list endpoint. */
    void validateKey(String apiKey, AiCallback<Void> callback);

    /** Fetches models that support content generation. */
    void listModels(String apiKey, AiCallback<List<AiModel>> callback);

    /** Non-streaming chat completion. */
    void chat(List<ChatMessage> messages, String systemInstruction, AiCallback<String> callback);

    /** Streaming chat completion; returns a handle that can cancel the request. */
    Cancellable chatStream(List<ChatMessage> messages, String systemInstruction, StreamCallback callback);

    interface StreamCallback {
        void onToken(String token);

        void onComplete(String fullText);

        void onError(AiError error);
    }

    interface Cancellable {
        void cancel();

        boolean isCancelled();
    }

    /** Simple cancellable that wraps an AtomicBoolean and an optional in-flight call. */
    class FlagCancellable implements Cancellable {
        private final AtomicBoolean flag = new AtomicBoolean(false);
        private volatile okhttp3.Call call;

        /** Attaches the OkHttp call so cancellation aborts the network request. */
        public void attach(okhttp3.Call call) {
            this.call = call;
            if (flag.get()) call.cancel();
        }

        @Override
        public void cancel() {
            flag.set(true);
            okhttp3.Call c = this.call;
            if (c != null) c.cancel();
        }

        @Override
        public boolean isCancelled() {
            return flag.get();
        }
    }
}
