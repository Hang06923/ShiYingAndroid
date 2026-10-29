package com.hang.shiying.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MediaInfo {
    public final Platform platform;
    public final String sourceUrl;
    public final String title;
    public final String author;
    public final String coverUrl;
    public final long durationMs;
    public final String directMediaUrl;
    public final MediaKind kind;
    public final List<String> imageUrls;

    public MediaInfo(
            Platform platform,
            String sourceUrl,
            String title,
            String author,
            String coverUrl,
            long durationMs,
            String directMediaUrl) {
        this(
                platform,
                sourceUrl,
                title,
                author,
                coverUrl,
                durationMs,
                directMediaUrl,
                MediaKind.VIDEO,
                Collections.emptyList());
    }

    public MediaInfo(
            Platform platform,
            String sourceUrl,
            String title,
            String author,
            String coverUrl,
            long durationMs,
            String directMediaUrl,
            MediaKind kind,
            List<String> imageUrls) {
        this.platform = platform;
        this.sourceUrl = sourceUrl;
        this.title = title == null ? "" : title;
        this.author = author == null ? "" : author;
        this.coverUrl = coverUrl == null ? "" : coverUrl;
        this.durationMs = Math.max(0, durationMs);
        this.directMediaUrl = directMediaUrl == null ? "" : directMediaUrl;
        this.kind = kind == null ? MediaKind.VIDEO : kind;
        this.imageUrls = imageUrls == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(imageUrls));
    }
}
