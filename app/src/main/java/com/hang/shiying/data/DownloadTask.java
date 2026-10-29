package com.hang.shiying.data;

import com.hang.shiying.core.Platform;
import com.hang.shiying.core.TaskStatus;

public final class DownloadTask {
    public final String id;
    public final String sourceUrl;
    public final Platform platform;
    public final String title;
    public final String author;
    public final long durationMs;
    public final long createdAt;
    public TaskStatus status;
    public int progress;
    public String localUri;
    public String errorCode;
    public String errorMessage;
    public long updatedAt;

    public DownloadTask(
            String id,
            String sourceUrl,
            Platform platform,
            String title,
            String author,
            long durationMs,
            long createdAt) {
        this.id = id;
        this.sourceUrl = sourceUrl;
        this.platform = platform;
        this.title = title == null ? "" : title;
        this.author = author == null ? "" : author;
        this.durationMs = Math.max(0, durationMs);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = TaskStatus.QUEUED;
    }
}
