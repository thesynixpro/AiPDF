package com.aipdf.assistant.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import com.aipdf.assistant.ai.models.AiModel;

/**
 * Response parsing kept separate from the providers so it can be unit tested
 * against recorded API payloads.
 */
public final class ModelListParsers {

    private ModelListParsers() {
    }

    /** Parses Google Gemini v1beta models list response. */
    public static List<AiModel> parseGeminiModels(String json) {
        List<AiModel> out = new ArrayList<>();
        JsonObject root = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
        JsonElement modelsEl = root.get("models");
        if (modelsEl == null || modelsEl.isJsonNull()) return out;
        JsonArray models = modelsEl.getAsJsonArray();
        for (JsonElement e : models) {
            if (!e.isJsonObject()) continue;
            JsonObject m = e.getAsJsonObject();
            String name = str(m, "name", null);
            if (name == null || name.isEmpty()) continue;
            // API returns "models/gemini-…"; strip the prefix for display/storage.
            String id = name.startsWith("models/") ? name.substring("models/".length()) : name;
            if (id.contains("tuned")) continue;
            boolean supports = false;
            JsonElement methods = m.get("supportedGenerationMethods");
            if (methods != null && methods.isJsonArray()) {
                for (JsonElement me : methods.getAsJsonArray()) {
                    if (me.isJsonPrimitive() && "generateContent".equals(me.getAsString())) {
                        supports = true;
                        break;
                    }
                }
            } else {
                // Older payloads omit the field; assume generation-capable if name looks generative.
                supports = id.startsWith("gemini");
            }
            if (!supports) continue;
            String display = str(m, "displayName", id);
            String desc = str(m, "description", null);
            Long ctx = null;
            if (m.has("inputTokenLimit") && m.get("inputTokenLimit").isJsonPrimitive()) {
                try {
                    ctx = m.get("inputTokenLimit").getAsLong();
                } catch (Exception ignored) {
                }
            }
            out.add(new AiModel(id, display, desc, ctx, "Google", true));
        }
        out.sort((a, b) -> a.id.compareToIgnoreCase(b.id));
        return out;
    }

    /** Parses OpenRouter /api/v1/models response. */
    public static List<AiModel> parseOpenRouterModels(String json) {
        List<AiModel> out = new ArrayList<>();
        JsonObject root = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
        JsonElement dataEl = root.get("data");
        if (dataEl == null || dataEl.isJsonNull()) return out;
        JsonArray data = dataEl.getAsJsonArray();
        for (JsonElement e : data) {
            if (!e.isJsonObject()) continue;
            JsonObject m = e.getAsJsonObject();
            String id = str(m, "id", null);
            if (id == null || id.trim().isEmpty()) continue;
            String display = str(m, "name", id);
            String desc = str(m, "description", null);
            Long ctx = null;
            if (m.has("context_length") && m.get("context_length").isJsonPrimitive()
                    && !m.get("context_length").isJsonNull()) {
                try {
                    ctx = m.get("context_length").getAsLong();
                } catch (Exception ignored) {
                }
            }
            String providerLabel = null;
            int slash = id.indexOf('/');
            if (slash > 0) providerLabel = id.substring(0, slash);
            out.add(new AiModel(id, display, desc, ctx, providerLabel, true));
        }
        out.sort((a, b) -> a.id.compareToIgnoreCase(b.id));
        return out;
    }

    private static String str(JsonObject o, String key, String def) {
        if (!o.has(key) || o.get(key).isJsonNull()) return def;
        try {
            return o.get(key).getAsString();
        } catch (Exception e) {
            return def;
        }
    }

    public static String toJson(List<AiModel> models) {
        return new Gson().toJson(models);
    }
}
