package com.aipdf.assistant.settings;

public final class ProviderIds {
    public static final String GEMINI = "gemini";
    public static final String OPENROUTER = "openrouter";
    public static final String GEMINI_DISPLAY = "Google Gemini";
    public static final String OPENROUTER_DISPLAY = "OpenRouter";

    private ProviderIds() {
    }

    public static String displayName(String id) {
        if (OPENROUTER.equals(id)) return OPENROUTER_DISPLAY;
        return GEMINI_DISPLAY;
    }

    public static boolean isValid(String id) {
        return GEMINI.equals(id) || OPENROUTER.equals(id);
    }
}
