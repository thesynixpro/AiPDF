package com.aipdf.assistant.ai.providers;

import android.content.Context;
import android.text.TextUtils;

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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * OpenRouter integration (https://openrouter.ai/docs).
 * Uses /api/v1/models and /api/v1/chat/completions.
 */
public class OpenRouterProvider implements AiProvider {

    private static final String BASE = "https://openrouter.ai/api/v1/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String APP_HEADER = "X-Title";
    private static final String APP_NAME = "AI PDF Assistant";

    private final Context context;
    private volatile Call activeCall;

    public OpenRouterProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String getId() {
        return ProviderIds.OPENROUTER;
    }

    @Override
    public String getDisplayName() {
        return ProviderIds.OPENROUTER_DISPLAY;
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

    private OkHttpClient client() {
        return ApiClient.http();
    }

    private Request.Builder authed(String url, String apiKey) {
        return new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .header(APP_HEADER, APP_NAME)
                .header("Content-Type", "application/json");
    }

    @Override
    public void validateKey(String apiKey, AiCallback<Void> callback) {
        if (TextUtils.isEmpty(apiKey)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No API key"));
            return;
        }
        AppExecutors.network(() -> {
            Request request = authed(BASE + "models", apiKey).get().build();
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
            Request request = authed(BASE + "models", apiKey).get().build();
            try (Response response = client().newCall(request).execute()) {
                String body = safeBody(response);
                if (response.isSuccessful()) {
                    List<AiModel> models = ModelListParsers.parseOpenRouterModels(body);
                    if (models.isEmpty()) {
                        AppExecutors.main(() -> callback.onError(
                                AiError.of(AiError.Code.EMPTY_MODELS, "Empty model list")));
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
        String model = getModelId();
        String apiKey = getApiKey();
        if (TextUtils.isEmpty(apiKey)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No API key"));
            return;
        }
        if (TextUtils.isEmpty(model)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No model selected"));
            return;
        }
        String payload = buildPayload(model, messages, systemInstruction, false);
        AppExecutors.network(() -> {
            Request request = authed(BASE + "chat/completions", apiKey)
                    .post(RequestBody.create(payload, JSON))
                    .build();
            try (Response response = client().newCall(request).execute()) {
                String body = safeBody(response);
                if (response.isSuccessful()) {
                    String text = parseOpenRouterText(body);
                    if (text == null) {
                        AppExecutors.main(() -> callback.onError(
                                AiError.of(AiError.Code.BAD_RESPONSE, "No choices in response")));
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
        String model = getModelId();
        String apiKey = getApiKey();
        if (TextUtils.isEmpty(apiKey)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No API key"));
            return new FlagCancellable();
        }
        if (TextUtils.isEmpty(model)) {
            callback.onError(AiError.of(AiError.Code.NOT_CONFIGURED, "No model selected"));
            return new FlagCancellable();
        }
        String payload = buildPayload(model, messages, systemInstruction, true);
        FlagCancellable cancellable = new FlagCancellable();
        Request request = authed(BASE + "chat/completions", apiKey)
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
                            String token = extractDelta(obj);
                            if (token != null && !token.isEmpty()) {
                                full.append(token);
                                String t = token;
                                AppExecutors.main(() -> callback.onToken(t));
                            }
                        } catch (Exception ignored) {
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

    public static String buildPayload(String model, List<ChatMessage> messages, String systemInstruction, boolean stream) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model == null ? "" : model);
        JsonArray arr = new JsonArray();
        if (!TextUtils.isEmpty(systemInstruction)) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", ChatMessage.ROLE_SYSTEM);
            sys.addProperty("content", systemInstruction);
            arr.add(sys);
        }
        for (ChatMessage m : messages) {
            if (ChatMessage.ROLE_SYSTEM.equals(m.role)) continue;
            JsonObject msg = new JsonObject();
            msg.addProperty("role", m.role);
            msg.addProperty("content", m.content);
            arr.add(msg);
        }
        root.add("messages", arr);
        root.addProperty("stream", stream);
        return root.toString();
    }

    public static String parseOpenRouterText(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray choices = obj.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return null;
            JsonObject first = choices.get(0).getAsJsonObject();
            if (first.has("message") && first.get("message").isJsonObject()) {
                JsonObject msg = first.getAsJsonObject("message");
                if (msg.has("content") && msg.get("content").isJsonPrimitive()) {
                    return msg.get("content").getAsString();
                }
            }
            if (first.has("text") && first.get("text").isJsonPrimitive()) {
                return first.get("text").getAsString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractDelta(JsonObject obj) {
        try {
            JsonArray choices = obj.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return null;
            JsonObject first = choices.get(0).getAsJsonObject();
            if (first.has("delta") && first.get("delta").isJsonObject()) {
                JsonObject delta = first.getAsJsonObject("delta");
                if (delta.has("content") && delta.get("content").isJsonPrimitive()) {
                    return delta.get("content").getAsString();
                }
            }
            if (first.has("message") && first.get("message").isJsonObject()) {
                JsonObject msg = first.getAsJsonObject("message");
                if (msg.has("content") && msg.get("content").isJsonPrimitive()) {
                    return msg.get("content").getAsString();
                }
            }
            return null;
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
