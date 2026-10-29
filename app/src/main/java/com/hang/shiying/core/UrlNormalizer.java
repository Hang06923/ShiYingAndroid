package com.hang.shiying.core;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlNormalizer {
    private static final Pattern URL = Pattern.compile(
            "https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final Pattern YOUTUBE_ID = Pattern.compile("[A-Za-z0-9_-]{6,}");
    private static final Pattern X_POST = Pattern.compile("/[^/]+/status/(\\d+)");
    private static final Pattern DOUYIN_SHORT = Pattern.compile("^/([A-Za-z0-9_-]+)/?$");
    private static final Pattern DOUYIN_VIDEO = Pattern.compile("^/(?:video|note)/(\\d+)");

    private UrlNormalizer() {}

    public static Extracted normalize(String raw) throws UserFacingException {
        if (raw == null) {
            throw new UserFacingException(ErrorCode.UNSUPPORTED_URL, "请粘贴视频链接");
        }

        Matcher matcher = URL.matcher(raw.trim());
        if (!matcher.find()) {
            throw new UserFacingException(ErrorCode.UNSUPPORTED_URL, "没有找到有效链接");
        }

        String candidate = matcher.group()
                .replaceAll("[),.;，。；、）】]+$", "");
        URI uri;
        try {
            uri = URI.create(candidate);
        } catch (IllegalArgumentException error) {
            throw new UserFacingException(ErrorCode.UNSUPPORTED_URL, "链接格式不正确", error);
        }

        String host = uri.getHost() == null
                ? ""
                : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();

        if (host.equals("v.douyin.com")
                || host.equals("www.douyin.com")
                || host.equals("douyin.com")
                || host.equals("www.iesdouyin.com")) {
            String id = modalId(uri.getRawQuery());
            if (id == null) {
                Matcher video = DOUYIN_VIDEO.matcher(path);
                if (video.find()) {
                    id = video.group(1);
                }
            }
            if (id == null) {
                Matcher shortMatch = DOUYIN_SHORT.matcher(path);
                if (shortMatch.find()) {
                    id = shortMatch.group(1);
                }
            }
            return new Extracted(Platform.DOUYIN, candidate, id == null ? "" : id);
        }

        if (host.equals("youtu.be")) {
            String id = path.replaceFirst("^/", "").split("/")[0];
            return youtube(id);
        }

        if (host.equals("youtube.com") || host.endsWith(".youtube.com")) {
            String id = queryValue(uri.getRawQuery(), "v");
            if (path.startsWith("/shorts/")) {
                id = path.substring(8).split("/")[0];
            }
            if ((path.startsWith("/watch") || path.startsWith("/shorts/")) && id != null) {
                return youtube(id);
            }
            throw new UserFacingException(
                    ErrorCode.NOT_VIDEO, "首版只支持单条 YouTube 视频");
        }

        if (host.equals("x.com")
                || host.equals("www.x.com")
                || host.equals("twitter.com")
                || host.equals("www.twitter.com")
                || host.equals("mobile.twitter.com")) {
            Matcher post = X_POST.matcher(path);
            if (!post.find()) {
                throw new UserFacingException(
                        ErrorCode.NOT_VIDEO, "请粘贴单条 X 推文链接");
            }
            String[] parts = path.split("/");
            if (parts.length < 3) {
                throw new UserFacingException(
                        ErrorCode.NOT_VIDEO, "请粘贴单条 X 推文链接");
            }
            return new Extracted(
                    Platform.X,
                    "https://x.com/" + parts[1] + "/status/" + post.group(1),
                    post.group(1));
        }

        throw new UserFacingException(ErrorCode.UNSUPPORTED_URL, "暂不支持这个平台");
    }

    private static Extracted youtube(String id) throws UserFacingException {
        if (id == null || !YOUTUBE_ID.matcher(id).matches()) {
            throw new UserFacingException(ErrorCode.NOT_VIDEO, "YouTube 视频 ID 不正确");
        }
        return new Extracted(
                Platform.YOUTUBE,
                "https://www.youtube.com/watch?v=" + id,
                id);
    }

    private static String queryValue(String raw, String key) {
        if (raw == null) {
            return null;
        }
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            String decodedKey = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            if (key.equals(decodedKey) && parts.length == 2) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String modalId(String raw) {
        return queryValue(raw, "modal_id");
    }

    public static final class Extracted {
        public final Platform platform;
        public final String url;
        public final String id;

        Extracted(Platform platform, String url, String id) {
            this.platform = platform;
            this.url = url;
            this.id = id;
        }
    }
}
