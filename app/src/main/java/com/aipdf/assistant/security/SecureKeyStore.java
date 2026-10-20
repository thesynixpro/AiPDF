package com.aipdf.assistant.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.aipdf.assistant.settings.ProviderIds;

/**
 * Securely stores per-provider API keys. Keys are encrypted with the Android
 * Keystore before being written to SharedPreferences. Cleartext keys are never
 * logged and never written to the preference value space unencrypted.
 */
public class SecureKeyStore {

    private static final String TAG = "SecureKeyStore";
    private static final String PREFS = "secure_credentials";
    private static final String PREFIX = "key_";

    private static SecureKeyStore instance;

    private final SharedPreferences prefs;

    private SecureKeyStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized SecureKeyStore getInstance(Context context) {
        if (instance == null) instance = new SecureKeyStore(context);
        return instance;
    }

    public void saveApiKey(String providerId, String apiKey) {
        if (TextUtils.isEmpty(apiKey)) {
            removeApiKey(providerId);
            return;
        }
        try {
            String encoded = CryptoManager.get().encrypt(apiKey.trim());
            prefs.edit().putString(PREFIX + providerId, encoded).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt API key");
        }
    }

    public String getApiKey(String providerId) {
        String encoded = prefs.getString(PREFIX + providerId, null);
        if (encoded == null) return null;
        try {
            String key = CryptoManager.get().decrypt(encoded);
            return TextUtils.isEmpty(key) ? null : key;
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt stored API key");
            return null;
        }
    }

    public boolean hasApiKey(String providerId) {
        return getApiKey(providerId) != null;
    }

    public void removeApiKey(String providerId) {
        prefs.edit().remove(PREFIX + providerId).apply();
    }

    /** Returns a masked representation safe for display, e.g. "sk-…abcd". */
    public String maskedKey(String providerId) {
        String key = getApiKey(providerId);
        if (key == null) return null;
        if (key.length() <= 8) return "••••••";
        return key.substring(0, 4) + "…" + key.substring(key.length() - 4);
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public boolean hasAnyKey() {
        return hasApiKey(ProviderIds.GEMINI) || hasApiKey(ProviderIds.OPENROUTER);
    }
}
