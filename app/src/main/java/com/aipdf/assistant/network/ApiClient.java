package com.aipdf.assistant.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public final class ApiClient {

    private static volatile OkHttpClient client;

    private ApiClient() {
    }

    public static OkHttpClient http() {
        if (client == null) {
            synchronized (ApiClient.class) {
                if (client == null) {
                    // Logging interceptor at NONE: request/response bodies may contain
                    // API keys or document content and must never reach logcat.
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.NONE);
                    client = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(180, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .callTimeout(240, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .addInterceptor(logging)
                            .build();
                }
            }
        }
        return client;
    }
}
