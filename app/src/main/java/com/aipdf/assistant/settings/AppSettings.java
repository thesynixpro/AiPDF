package com.aipdf.assistant.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class AppSettings {

    private static final String PREFS = "app_settings";

    private static SharedPreferences prefs;

    public enum PageSize {A4, LETTER, LEGAL}

    public enum Orientation {PORTRAIT, LANDSCAPE}

    private AppSettings() {
    }

    public static void init(Context context) {
        if (prefs == null) {
            prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }

    private static SharedPreferences p() {
        if (prefs == null) throw new IllegalStateException("AppSettings.init not called");
        return prefs;
    }

    public static String getDefaultProviderId() {
        String v = p().getString("default_provider", ProviderIds.GEMINI);
        return ProviderIds.isValid(v) ? v : ProviderIds.GEMINI;
    }

    public static void setDefaultProviderId(String id) {
        p().edit().putString("default_provider", id).apply();
    }

    public static String getModelFor(String providerId) {
        return p().getString("model_" + providerId, null);
    }

    public static void setModelFor(String providerId, String modelId) {
        p().edit().putString("model_" + providerId, modelId).apply();
    }

    public static PageSize getPageSize() {
        try {
            return PageSize.valueOf(p().getString("page_size", PageSize.A4.name()));
        } catch (Exception e) {
            return PageSize.A4;
        }
    }

    public static void setPageSize(PageSize size) {
        p().edit().putString("page_size", size.name()).apply();
    }

    public static Orientation getOrientation() {
        try {
            return Orientation.valueOf(p().getString("orientation", Orientation.PORTRAIT.name()));
        } catch (Exception e) {
            return Orientation.PORTRAIT;
        }
    }

    public static void setOrientation(Orientation orientation) {
        p().edit().putString("orientation", orientation.name()).apply();
    }

    public static String getDocumentLanguage() {
        return p().getString("doc_language", "en");
    }

    public static void setDocumentLanguage(String lang) {
        p().edit().putString("doc_language", lang).apply();
    }

    public static boolean isDarkMode() {
        return p().getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                == AppCompatDelegate.MODE_NIGHT_YES;
    }

    public static int getDarkMode() {
        return p().getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setDarkMode(int mode) {
        p().edit().putInt("dark_mode", mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void applyThemeOnStart() {
        AppCompatDelegate.setDefaultNightMode(getDarkMode());
    }

    public static void clearModelCache() {
        p().edit().remove("model_cache_gemini").remove("model_cache_openrouter").apply();
    }
}
