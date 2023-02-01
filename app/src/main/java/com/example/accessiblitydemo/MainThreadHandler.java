package com.example.accessiblitydemo;

import android.os.Handler;
import android.os.Looper;

public class MainThreadHandler {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void runOnUiThread(Runnable r) {
        handler.post(r);
    }

    public static void postDelayed(Runnable r, long millis) {
        handler.postDelayed(r, millis);
    }

    public static void remove(Runnable r) {
        handler.removeCallbacks(r);
    }
}
