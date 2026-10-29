package com.hang.shiying.extractor;

import com.hang.shiying.core.ErrorCode;
import com.hang.shiying.core.UserFacingException;

import java.util.Locale;

public final class YtDlpErrors {
    private YtDlpErrors() {}

    public static UserFacingException map(Throwable error) {
        String text = error.getMessage() == null
                ? error.toString()
                : error.getMessage();
        String lower = text.toLowerCase(Locale.ROOT);

        if (containsAny(
                lower,
                "sign in",
                "login required",
                "requires authentication",
                "cookies are needed",
                "confirm you are not a bot")) {
            return new UserFacingException(
                    ErrorCode.LOGIN_REQUIRED,
                    "该视频需要登录，首版暂不支持",
                    error);
        }
        if (containsAny(
                lower,
                "not available in your country",
                "unavailable in your country",
                "geo restricted",
                "geo-restricted",
                "region blocked")) {
            return new UserFacingException(
                    ErrorCode.REGION_BLOCKED,
                    "该视频存在地区限制",
                    error);
        }
        if (containsAny(
                lower,
                "network is unreachable",
                "connection refused",
                "connect timed out",
                "read timed out",
                "unknown host",
                "unable to download webpage")) {
            return new UserFacingException(
                    ErrorCode.NETWORK_ERROR,
                    "网络连接失败，请检查网络或系统代理",
                    error);
        }
        return new UserFacingException(
                ErrorCode.PARSE_FAILED,
                "视频解析失败，请检查链接和网络",
                error);
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
