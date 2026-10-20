package com.aipdf.assistant.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypts small secrets (API keys) with an AES-GCM key held in the Android Keystore.
 */
public final class CryptoManager {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "aipdf_secret_key";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_LENGTH = 12;

    private static CryptoManager instance;

    private CryptoManager() {
    }

    public static synchronized CryptoManager get() {
        if (instance == null) instance = new CryptoManager();
        return instance;
    }

    public String encrypt(String plain) throws Exception {
        if (plain == null) return null;
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] iv = cipher.getIV();
        byte[] cipherText = cipher.doFinal(plain.getBytes("UTF-8"));
        ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
        buf.put(iv);
        buf.put(cipherText);
        return Base64.encodeToString(buf.array(), Base64.NO_WRAP);
    }

    public String decrypt(String encoded) throws Exception {
        if (encoded == null) return null;
        byte[] data = Base64.decode(encoded, Base64.NO_WRAP);
        if (data.length < GCM_IV_LENGTH + 1) throw new IllegalArgumentException("Invalid ciphertext");
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buf.get(iv);
        byte[] cipherText = new byte[buf.remaining()];
        buf.get(cipherText);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(cipherText), "UTF-8");
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
        if (entry != null) {
            return entry.getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
