package com.hang.shiying.extractor;

import com.hang.shiying.core.ErrorCode;
import com.hang.shiying.core.MediaInfo;
import com.hang.shiying.core.UserFacingException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DouyinExtractor implements MediaExtractor {
    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/132.0 Mobile Safari/537.36";
    private static final Pattern VIDEO_ID = Pattern.compile(
            "(?:/video/|modal_id=|/note/)(\\d{8,})");

    @Override
    public MediaInfo extract(String normalizedUrl) throws UserFacingException {
        String id = resolveId(normalizedUrl);
        String shareUrl = "https://www.iesdouyin.com/share/video/" + id;
        return DouyinHtmlParser.parse(
                fetchSharePage(shareUrl),
                "https://www.douyin.com/video/" + id);
    }

    static String resolveId(String raw) throws UserFacingException {
        Matcher direct = VIDEO_ID.matcher(raw);
        if (direct.find()) {
            return direct.group(1);
        }
        try {
            HttpURLConnection connection = openConnection(raw);
            connection.setInstanceFollowRedirects(true);
            // HttpURLConnection updates getURL() only after the response status is read.
            connection.getResponseCode();
            String finalUrl = connection.getURL().toString();
            connection.disconnect();
            Matcher matcher = VIDEO_ID.matcher(finalUrl);
            if (!matcher.find()) {
                throw new UserFacingException(
                        ErrorCode.NOT_VIDEO,
                        "无法识别抖音作品编号");
            }
            return matcher.group(1);
        } catch (UserFacingException error) {
            throw error;
        } catch (Exception error) {
            throw new UserFacingException(
                    ErrorCode.NETWORK_ERROR,
                    "抖音短链解析失败，请检查网络",
                    error);
        }
    }

    static String fetchSharePage(String raw) throws UserFacingException {
        try {
            String cookie = "";
            for (int attempt = 0; attempt < 5; attempt++) {
                PageResponse response = requestPage(raw, cookie);
                if (response.body.contains("videoInfoRes")) {
                    return response.body;
                }
                cookie = mergeCookies(cookie, response.cookie);
            }
            throw new UserFacingException(
                    ErrorCode.PARSE_FAILED,
                    "抖音分享页没有返回作品数据");
        } catch (UserFacingException error) {
            throw error;
        } catch (Exception error) {
            throw new UserFacingException(
                    ErrorCode.NETWORK_ERROR,
                    "抖音分享页加载失败",
                    error);
        }
    }

    private static PageResponse requestPage(
            String raw,
            String cookie) throws Exception {
        HttpURLConnection connection = openConnection(raw);
        if (!cookie.isEmpty()) {
            connection.setRequestProperty("Cookie", cookie);
        }
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("HTTP " + status);
            }
            String body = new String(
                    connection.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            return new PageResponse(body, cookiesFrom(connection));
        } finally {
            connection.disconnect();
        }
    }

    private static String cookiesFrom(HttpURLConnection connection) {
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry
                : connection.getHeaderFields().entrySet()) {
            if (entry.getKey() == null
                    || !"Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (value != null && !value.isEmpty()) {
                    pairs.add(value.split(";", 2)[0].trim());
                }
            }
        }
        return String.join("; ", pairs);
    }

    private static String mergeCookies(String existing, String next) {
        Map<String, String> merged = new LinkedHashMap<>();
        addCookies(merged, existing);
        addCookies(merged, next);
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : merged.entrySet()) {
            values.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join("; ", values);
    }

    private static void addCookies(Map<String, String> target, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        for (String pair : value.split(";")) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length == 2 && !parts[0].isEmpty()) {
                target.put(parts[0], parts[1]);
            }
        }
    }

    private static HttpURLConnection openConnection(String raw) throws Exception {
        URLConnection base = new URL(raw).openConnection();
        HttpURLConnection connection = (HttpURLConnection) base;
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(20_000);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Referer", "https://www.douyin.com/");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        return connection;
    }

    private static final class PageResponse {
        final String body;
        final String cookie;

        PageResponse(String body, String cookie) {
            this.body = body;
            this.cookie = cookie;
        }
    }
}
