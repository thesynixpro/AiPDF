package com.aipdf.assistant.util;

import android.net.Uri;

/** Tiny relay used to pass an ACTION_VIEW PDF Uri from MainActivity to FilesFragment. */
public final class PendingIntentBus {

    private static volatile Uri pendingImportUri;

    private PendingIntentBus() {
    }

    public static void setPendingImportUri(Uri uri) {
        pendingImportUri = uri;
    }

    public static Uri consumePendingImportUri() {
        Uri uri = pendingImportUri;
        pendingImportUri = null;
        return uri;
    }
}
