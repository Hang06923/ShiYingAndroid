package com.hang.shiying.storage;

import java.io.File;

public final class FileNames {
    private FileNames() {}

    public static String safe(String title) {
        String value = title == null ? "" : title.trim();
        value = value.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        if (value.isBlank()) {
            value = "视频";
        }
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    public static File tempDir(File cacheRoot, String taskId) {
        File dir = new File(cacheRoot, "downloads/" + taskId);
        if (!dir.mkdirs() && !dir.isDirectory()) {
            throw new IllegalStateException("无法创建下载缓存目录");
        }
        return dir;
    }
}
