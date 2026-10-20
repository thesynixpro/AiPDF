package com.aipdf.assistant.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {

    private static final ExecutorService DISK = Executors.newFixedThreadPool(2);
    private static final ExecutorService NETWORK = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static void disk(Runnable r) {
        DISK.execute(r);
    }

    public static void network(Runnable r) {
        NETWORK.execute(r);
    }

    public static void main(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            MAIN.post(r);
        }
    }
}
