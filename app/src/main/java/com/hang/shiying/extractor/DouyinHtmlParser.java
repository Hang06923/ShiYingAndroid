package com.hang.shiying.extractor;

import com.hang.shiying.core.ErrorCode;
import com.hang.shiying.core.MediaInfo;
import com.hang.shiying.core.MediaKind;
import com.hang.shiying.core.Platform;
import com.hang.shiying.core.UserFacingException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public final class DouyinHtmlParser {
    private static final String ROUTER_MARKER = "window._ROUTER_DATA";

    private DouyinHtmlParser() {}

    public static MediaInfo parse(String html, String sourceUrl) throws UserFacingException {
        if (html == null || html.isEmpty()) {
            throw new UserFacingException(ErrorCode.PARSE_FAILED, "抖音分享页内容为空");
        }

        int markerStart = html.indexOf(ROUTER_MARKER);
        if (markerStart < 0) {
            throw new UserFacingException(ErrorCode.PARSE_FAILED, "抖音页面结构已变化");
        }

        int objectStart = html.indexOf('{', markerStart);
        int objectEnd = matchingBrace(html, objectStart);
        if (objectStart < 0 || objectEnd < 0) {
            throw new UserFacingException(ErrorCode.PARSE_FAILED, "抖音数据格式不正确");
        }

        try {
            JSONObject root = new JSONObject(html.substring(objectStart, objectEnd + 1));
            JSONObject item = findContentItem(root);
            if (item == null) {
                throw new UserFacingException(ErrorCode.NOT_VIDEO, "这条内容不是视频");
            }

            JSONObject author = item.optJSONObject("author");
            String title = item.optString("desc", "抖音作品").trim();
            String authorName = author == null
                    ? ""
                    : author.optString("nickname", "").trim();
            List<String> imageUrls = imageUrls(item.optJSONArray("images"));
            if (!imageUrls.isEmpty()) {
                return new MediaInfo(
                        Platform.DOUYIN,
                        sourceUrl,
                        title,
                        authorName,
                        imageUrls.get(0),
                        0,
                        "",
                        MediaKind.IMAGES,
                        imageUrls);
            }

            JSONObject video = item.optJSONObject("video");
            if (video == null) {
                throw new UserFacingException(ErrorCode.NOT_VIDEO, "这条内容不是视频");
            }
            String direct = firstUrl(video.getJSONObject("play_addr"))
                    .replace("/playwm/", "/play/")
                    .replace("watermark=1", "watermark=0");

            if (direct.isEmpty()) {
                throw new UserFacingException(ErrorCode.PARSE_FAILED, "没有找到可下载的视频地址");
            }

            return new MediaInfo(
                    Platform.DOUYIN,
                    sourceUrl,
                    title,
                    authorName,
                    firstUrl(video.optJSONObject("cover")),
                    video.optLong("duration", 0),
                    direct);
        } catch (UserFacingException error) {
            throw error;
        } catch (Exception error) {
            throw new UserFacingException(
                    ErrorCode.PARSE_FAILED,
                    "无法读取抖音视频信息",
                    error);
        }
    }

    private static JSONObject findContentItem(Object value) {
        if (value instanceof JSONObject object) {
            if (object.has("aweme_id")
                    && (object.has("video") || object.has("images"))) {
                return object;
            }
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                JSONObject found = findContentItem(object.opt(keys.next()));
                if (found != null) {
                    return found;
                }
            }
        } else if (value instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject found = findContentItem(array.opt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<String> imageUrls(JSONArray images) {
        List<String> urls = new ArrayList<>();
        if (images == null) {
            return urls;
        }
        for (int index = 0; index < images.length(); index++) {
            JSONObject image = images.optJSONObject(index);
            if (image == null) {
                continue;
            }
            JSONArray candidates = image.optJSONArray("url_list");
            if (candidates == null || candidates.length() == 0) {
                continue;
            }
            String selected = "";
            for (int urlIndex = 0; urlIndex < candidates.length(); urlIndex++) {
                String candidate = candidates.optString(urlIndex, "");
                if (!candidate.isEmpty()) {
                    if (selected.isEmpty()) {
                        selected = candidate;
                    }
                    if (candidate.contains("jpeg") || candidate.contains(".jpg")) {
                        selected = candidate;
                        break;
                    }
                }
            }
            if (!selected.isEmpty()) {
                urls.add(selected);
            }
        }
        return urls;
    }

    private static String firstUrl(JSONObject holder) {
        if (holder == null) {
            return "";
        }
        JSONArray urls = holder.optJSONArray("url_list");
        if (urls == null || urls.length() == 0) {
            return "";
        }
        return urls.optString(0, "");
    }

    private static int matchingBrace(String text, int start) {
        if (start < 0) {
            return -1;
        }
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    quoted = false;
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }
}
