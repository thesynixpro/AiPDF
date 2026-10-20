package com.aipdf.assistant.ai;

public interface AiCallback<T> {
    void onSuccess(T result);

    void onError(AiError error);
}
