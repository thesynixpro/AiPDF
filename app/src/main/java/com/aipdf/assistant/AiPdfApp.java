package com.aipdf.assistant;

import android.app.Application;

import com.aipdf.assistant.data.AppDatabase;
import com.aipdf.assistant.settings.AppSettings;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class AiPdfApp extends Application {

    private static AiPdfApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        PDFBoxResourceLoader.init(getApplicationContext());
        AppSettings.init(getApplicationContext());
        AppDatabase.init(getApplicationContext());
    }

    public static AiPdfApp get() {
        return instance;
    }
}
