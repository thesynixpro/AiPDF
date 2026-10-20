package com.aipdf.assistant.ai.providers;

import android.content.Context;
import android.text.TextUtils;

import com.aipdf.assistant.AiPdfApp;
import com.aipdf.assistant.ai.AiCallback;
import com.aipdf.assistant.ai.AiError;
import com.aipdf.assistant.ai.AiProvider;
import com.aipdf.assistant.ai.models.AiModel;
import com.aipdf.assistant.ai.models.ChatMessage;
import com.aipdf.assistant.network.ApiClient;
import com.aipdf.assistant.network.ModelListParsers;
import com.aipdf.assistant.security.SecureKeyStore;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.settings.ProviderIds;
import com.aipdf.assistant.util.AppExecutors;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Google Gemini API (v1beta) integration.
 * Docs: https://ai.google.dev/api/models / generateContent.
 */
public class GeminiProvider implements AiProvider {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private volatile Call activeCall;

    public GeminiProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String getId() {
        return ProviderIds.GEMINI;
    }

    @Override
    public String getDisplayName() {
        return ProviderIds.GEMINI_DISPLAY;
    }

    @Override
    public boolean isConfigured() {
        return !TextUtils.isEmpty(getApiKey());
    }

    @Override
    public void setApiKey(String apiKey) {
        SecureKeyStore.getInstance(context).saveApiKey(getId(), apiKey);
    }

    @Override
    public String getApiKey() {
        return SecureKeyStore.getInstance(context).getApiKey(getId());
    }

    @Override
    public void clearApiKey() {
        SecureKeyStore.getInstance(context).removeApiKey(getId());
    }

    @Override
    public String getModelId() {
        return AppSettings.getModelFor(getId());
    }

    @Override
    public void setModelId(String modelId) {
        AppSettings.setModelFor(getId(), modelId);
    }

    private String modelPath() {
        String model = getModelId();
        if (TextUtils.isEmpty(model)) model = "";
        model = model.startsWith("models/") ? model.substring("models/".length()) : model;
        return model;
    }

    private OkHttpClient client() {
        return ApiClient.http();
    }

    private static String keyQuery(String apiKey) {
        return "?key=" + apiKey;
    }

    @Override
    public void validateKey(String apiKey, AiCallback<Void> callback) {
        if (TextUtils.isEmpty(apiKey)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No API key"));
            return;
        }
        AppExecutors.network(() -> {
            Request request = new Request.Builder()
                    .url(BASE + "models" + keyQuery(apiKey))
                    .get().build();
            try (Response response = client().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    AppExecutors.main(() -> callback.onSuccess(null));
                } else {
                    AiError err = AiError.fromHttp(response.code(), safeBody(response));
                    AppExecutors.main(() -> callback.onError(err));
                }
            } catch (IOException e) {
                AiError err = AiError.fromThrowable(e);
                AppExecutors.main(() -> callback.onError(err));
            }
        });
    }

    @Override
    public void listModels(String apiKey, AiCallback<List<AiModel>> callback) {
        if (TextUtils.isEmpty(apiKey)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No API key"));
            return;
        }
        AppExecutors.network(() -> {
            Request request = new Request.Builder()
                    .url(BASE + "models" + keyQuery(apiKey) + "&pageSize=1000")
                    .get().build();
            try (Response response = client().newCall(request).execute()) {
                String body = safeBody(response);
                if (response.isSuccessful()) {
                    List<AiModel> models = ModelListParsers.parseGeminiModels(body);
                    if (models.isEmpty()) {
                        AppExecutors.main(() -> callback.onError(
                                AiError.of(AiError.Code.EMPTY_MODELS, "No generation models returned")));
                    } else {
                        AppExecutors.main(() -> callback.onSuccess(models));
                    }
                } else {
                    AiError err = AiError.fromHttp(response.code(), body);
                    AppExecutors.main(() -> callback.onError(err));
                }
            } catch (Exception e) {
                AiError err = AiError.fromThrowable(e);
                AppExecutors.main(() -> callback.onError(err));
            }
        });
    }

    @Override
    public void chat(List<ChatMessage> messages, String systemInstruction, AiCallback<String> callback) {
        String model = modelPath();
        String apiKey = getApiKey();
        if (TextUtils.isEmpty(apiKey)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No API key"));
            return;
        }
        if (TextUtils.isEmpty(model)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No model selected"));
            return;
        }
        String payload = buildChatPayload(messages, systemInstruction);
        AppExecutors.network(() -> {
            Request request = new Request.Builder()
                    .url(BASE + "models/" + model + ":generateContent" + keyQuery(apiKey))
                    .post(RequestBody.create(payload, JSON))
                    .build();
            try (Response response = client().newCall(request).execute()) {
                String body = safeBody(response);
                if (response.isSuccessful()) {
                    String text = parseGeminiText(body);
                    if (text == null) {
                        AppExecutors.main(() -> callback.onError(
                                AiError.of(AiError.Code.BAD_RESPONSE, "Empty candidates in response")));
                    } else {
                        AppExecutors.main(() -> callback.onSuccess(text));
                    }
                } else {
                    AiError err = AiError.fromHttp(response.code(), body);
                    AppExecutors.main(() -> callback.onError(err));
                }
            } catch (IOException e) {
                AiError err = AiError.fromThrowable(e);
                AppExecutors.main(() -> callback.onError(err));
            }
        });
    }

    @Override
    public Cancellable chatStream(List<ChatMessage> messages, String systemInstruction, StreamCallback callback) {
        String model = modelPath();
        String apiKey = getApiKey();
        if (TextUtils.isEmpty(apiKey)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No API key"));
            return new FlagCancellable();
        }
        if (TextUtils.isEmpty(model)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No model selected"));
            return new FlagCancellable();
        }
        String payload = buildChatPayload(messages, systemInstruction);
        FlagCancellable cancellable = new FlagCancellable();
        Request request = new Request.Builder()
                .url(BASE + "models/" + model + ":streamGenerateContent" + keyQuery(apiKey) + "&alt=sse")
                .post(RequestBody.create(payload, JSON))
                .build();
        Call call = client().newCall(request);
        cancellable.attach(call);
        activeCall = call;
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                activeCall = null;
                if (c.isCanceled() || cancellable.isCancelled()) {
                    AppExecutors.main(() -> callback.onError(AiError.of(AiError.Code.CANCELLED, "Cancelled")));
                } else {
                    AppExecutors.main(() -> callback.onError(AiError.fromThrowable(e)));
                }
            }

            @Override
            public void onResponse(Call c, Response response) {
                activeCall = null;
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        String errBody = body != null ? body.string() : "";
                        AppExecutors.main(() -> callback.onError(AiError.fromHttp(response.code(), errBody)));
                        return;
                    }
                    if (body == null) {
                        AppExecutors.main(() -> callback.onError(
                                AiError.of(AiError.Code.BAD_RESPONSE, "Empty response body")));
                        return;
                    }
                    StringBuilder full = new java.lang.StringBuilder();
                    okio.BufferedSource source = body.source();
                    String line;
                    while ((line = source.readUtf8Line()) != null) {
                        if (cancellable.isCancelled()) {
                            AppExecutors.main(() -> callback.onError(AiError.of(AiError.Code.CANCELLED, "Cancelled")));
                            return;
                        }
                        if (line.isEmpty() || !line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if (data.isEmpty() || "[DONE]".equals(data)) continue;
                        try {
                            JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
                            String token = extractGeminiText(obj);
                            if (token != null && !token.isEmpty()) {
                                full.append(token);
                                String t = token;
                                AppExecutors.main(() -> callback.onToken(t));
                            }
                        } catch (Exception ignored) {
                            // Skip malformed SSE frames rather than aborting the stream.
                        }
                    }
                    String result = full.toString();
                    if (result.isEmpty()) {
                        AppExecutors.main(() -> callback.onError(
                                AiError.of(AiError.Code.BAD_RESPONSE, "No text in stream")));
                    } else {
                        AppExecutors.main(() -> callback.onComplete(result));
                    }
                } catch (Exception e) {
                    if (c.isCanceled() || cancellable.isCancelled()) {
                        AppExecutors.main(() -> callback.onError(AiError.of(AiError.Code.CANCELLED, "Cancelled")));
                    } else {
                        AppExecutors.main(() -> callback.onError(AiError.fromThrowable(e)));
                    }
                }
            }
        });
        return cancellable;
    }

    public void cancelActive() {
        Call c = activeCall;
        if (c != null) c.cancel();
    }

    static String buildChatPayload(List<ChatMessage> messages, String systemInstruction) {
        JsonObject root = new JsonObject();
        JsonArray contents = new JsonArray();
        for (ChatMessage m : messages) {
            if (ChatMessage.ROLE_SYSTEM.equals(m.role)) continue;
            JsonObject msg = new JsonObject();
            msg.addProperty("role", ChatMessage.ROLE_ASSISTANT.equals(m.role) ? "model" : "user");
            JsonObject part = new JsonObject();
            part.addProperty("text", m.content);
            JsonArray parts = new JsonArray();
            parts.add(part);
            msg.add("parts", parts);
            contents.add(msg);
        }
        root.add("contents", contents);
        if (!TextUtils.isEmpty(systemInstruction)) {
            JsonObject sys = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject p = new JsonObject();
            p.addProperty("text", systemInstruction);
            parts.add(p);
            sys.add("parts", parts);
            root.add("systemInstruction", sys);
        }
        JsonObject gen = new JsonObject();
        gen.addProperty("temperature", 0.7);
        gen.addProperty("topP", 0.95);
        gen.addProperty("maxOutputTokens", 8192);
        root.add("generationConfig", gen);
        return root.toString();
    }

    /** Extracts text from a full generateContent JSON response. */
    public static String parseGeminiText(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return extractGeminiText(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractGeminiText(JsonObject obj) {
        try {
            JsonArray candidates = obj.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            JsonObject first = candidates.get(0).getAsJsonObject();
            if (!first.has("content") || first.get("content").isJsonNull()) return null;
            JsonObject content = first.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null) return null;
            StringBuilder sb = new StringBuilder();
            for (JsonElement pe : parts) {
                if (pe.isJsonObject() && pe.getAsJsonObject().has("text")
                        && !pe.getAsJsonObject().get("text").isJsonNull()) {
                    sb.append(pe.getAsJsonObject().get("text").getAsString());
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeBody(Response response) {
        try (ResponseBody b = response.body()) {
            return b != null ? b.string() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
