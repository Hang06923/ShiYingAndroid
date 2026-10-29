package com.hang.shiying.extractor;

import android.content.Context;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;

public final class YtDlpRuntime {
    private static volatile Throwable initializationError;
    private static volatile boolean initialized;

    private YtDlpRuntime() {}

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }
        try {
            YoutubeDL.getInstance().init(context.getApplicationContext());
            FFmpeg.getInstance().init(context.getApplicationContext());
            initialized = true;
            initializationError = null;
        } catch (Throwable error) {
            initializationError = error;
        }
    }

    public static boolean isReady() {
        return initialized && initializationError == null;
    }

    public static Throwable error() {
        return initializationError;
    }
}
