package com.aipdf.assistant.ai;

import android.content.Context;
import android.text.TextUtils;

import com.aipdf.assistant.ai.models.AiModel;
import com.aipdf.assistant.ai.providers.GeminiProvider;
import com.aipdf.assistant.ai.providers.OpenRouterProvider;
import com.aipdf.assistant.network.ModelListParsers;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.settings.ProviderIds;
import com.aipdf.assistant.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry for AI providers. Keeps providers independent so switching
 * never overwrites the other provider's key or selected model.
 */
public class ProviderManager {

    private static ProviderManager instance;

    private final Context context;
    private final AiProvider gemini;
    private final AiProvider openRouter;

    private ProviderManager(Context context) {
        this.context = context.getApplicationContext();
        this.gemini = new GeminiProvider(context);
        this.openRouter = new OpenRouterProvider(context);
    }

    public static synchronized ProviderManager getInstance(Context context) {
        if (instance == null) instance = new ProviderManager(context);
        return instance;
    }

    public AiProvider getProvider(String id) {
        if (ProviderIds.OPENROUTER.equals(id)) return openRouter;
        return gemini;
    }

    public AiProvider getGemini() {
        return gemini;
    }

    public AiProvider getOpenRouter() {
        return openRouter;
    }

    public String getActiveProviderId() {
        return AppSettings.getDefaultProviderId();
    }

    public AiProvider getActiveProvider() {
        return getProvider(getActiveProviderId());
    }

    public void setActiveProviderId(String id) {
        if (ProviderIds.isValid(id)) AppSettings.setDefaultProviderId(id);
    }

    /** True when a provider has both a key and a selected model. */
    public boolean isReady(AiProvider provider) {
        return provider.isConfigured() && !TextUtils.isEmpty(provider.getModelId());
    }

    /** Model-cache keys for offline fallback UI. */
    public static String cacheKey(String providerId) {
        return "model_cache_" + providerId;
    }

    public void cacheModels(String providerId, List<AiModel> models) {
        context.getSharedPreferences("model_cache", Context.MODE_PRIVATE)
                .edit().putString(cacheKey(providerId), ModelListParsers.toJson(models)).apply();
    }

    public List<AiModel> getCachedModels(String providerId) {
        String json = context.getSharedPreferences("model_cache", Context.MODE_PRIVATE)
                .getString(cacheKey(providerId), null);
        if (json == null) return new ArrayList<>();
        try {
            AiModel[] arr = new com.google.gson.Gson().fromJson(json, AiModel[].class);
            List<AiModel> list = new ArrayList<>();
            if (arr != null) java.util.Collections.addAll(list, arr);
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean hasCachedModels(String providerId) {
        return !getCachedModels(providerId).isEmpty();
    }

    public void clearCaches() {
        context.getSharedPreferences("model_cache", Context.MODE_PRIVATE).edit().clear().apply();
    }
}
