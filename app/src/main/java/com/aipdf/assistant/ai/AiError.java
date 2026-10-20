package com.aipdf.assistant.ai;

import android.content.Context;

import com.aipdf.assistant.R;

public class AiError extends Exception {

    public enum Code {
        NOT_CONFIGURED, INVALID_KEY, FORBIDDEN, RATE_LIMIT, TIMEOUT, NETWORK,
        SERVER, BAD_RESPONSE, MODEL_UNSUPPORTED, CANCELLED, UNKNOWN, EMPTY_MODELS
    }

    public final Code code;
    public final int httpCode;

    private AiError(Code code, String message, int httpCode, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpCode = httpCode;
    }

    public static AiError of(Code code, String message) {
        return new AiError(code, message, 0, null);
    }

    public static AiError of(Code code, String message, int httpCode, Throwable cause) {
        return new AiError(code, message, httpCode, cause);
    }

    public boolean isCancelled() {
        return code == Code.CANCELLED;
    }

    /** Maps this error to a user-facing, actionable message. */
    public String userMessage(Context context) {
        switch (code) {
            case NOT_CONFIGURED:
                return context.getString(R.string.error_no_provider);
            case INVALID_KEY:
                return context.getString(R.string.error_unauthorized);
            case FORBIDDEN:
                return context.getString(R.string.error_forbidden);
            case RATE_LIMIT:
                return context.getString(R.string.error_rate_limit);
            case TIMEOUT:
                return context.getString(R.string.error_timeout);
            case NETWORK:
                return context.getString(R.string.error_network);
            case SERVER:
                return context.getString(R.string.error_server);
            case BAD_RESPONSE:
                return context.getString(R.string.error_bad_response);
            case MODEL_UNSUPPORTED:
                return context.getString(R.string.error_model_unsupported);
            case EMPTY_MODELS:
                return context.getString(R.string.ai_settings_no_models);
            default:
                return getMessage() != null ? getMessage() : context.getString(R.string.error_generic);
        }
    }

    public static AiError fromHttp(int code, String body) {
        if (code == 401) return of(Code.INVALID_KEY, "Unauthorized", code, null);
        if (code == 403) return of(Code.FORBIDDEN, "Forbidden", code, null);
        if (code == 404) return of(Code.MODEL_UNSUPPORTED, "Model or endpoint not found", code, null);
        if (code == 429) return of(Code.RATE_LIMIT, "Rate limited", code, null);
        if (code >= 500) return of(Code.SERVER, "Server error " + code, code, null);
        String snippet = body != null && body.length() > 200 ? body.substring(0, 200) : body;
        return of(Code.BAD_RESPONSE, "HTTP " + code + (snippet == null ? "" : ": " + snippet), code, null);
    }

    public static AiError fromThrowable(Throwable t) {
        if (t instanceof java.net.SocketTimeoutException) {
            return of(Code.TIMEOUT, "Request timed out", 0, t);
        }
        if (t instanceof java.io.InterruptedIOException) {
            return of(Code.CANCELLED, "Cancelled", 0, t);
        }
        if (t instanceof java.io.IOException) {
            return of(Code.NETWORK, "Network error", 0, t);
        }
        return of(Code.UNKNOWN, t.getMessage() == null ? "Unexpected error" : t.getMessage(), 0, t);
    }
}
