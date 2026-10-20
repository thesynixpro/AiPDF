package com.aipdf.assistant.ai.models;

public class AiModel {

    public final String id;
    public final String displayName;
    public final String description;
    public final Long contextLength;
    public final String providerLabel;
    public final boolean supportsGeneration;

    public AiModel(String id, String displayName, String description,
                   Long contextLength, String providerLabel, boolean supportsGeneration) {
        this.id = id;
        this.displayName = displayName == null || displayName.isEmpty() ? id : displayName;
        this.description = description;
        this.contextLength = contextLength;
        this.providerLabel = providerLabel;
        this.supportsGeneration = supportsGeneration;
    }

    public String contextLabel() {
        if (contextLength == null || contextLength <= 0) return null;
        long k = contextLength / 1000;
        if (contextLength >= 1_000_000) {
            return String.format(java.util.Locale.US, "%.1fM tokens", contextLength / 1_000_000.0);
        }
        if (k > 0) return k + "K tokens";
        return contextLength + " tokens";
    }

    @Override
    public String toString() {
        return displayName;
    }
}
