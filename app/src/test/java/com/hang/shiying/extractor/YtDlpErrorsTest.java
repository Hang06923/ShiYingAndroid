package com.hang.shiying.extractor;

import static org.junit.Assert.assertEquals;

import com.hang.shiying.core.ErrorCode;
import com.hang.shiying.core.UserFacingException;

import org.junit.Test;

public final class YtDlpErrorsTest {
    @Test
    public void mapsLoginWall() {
        UserFacingException error = YtDlpErrors.map(
                new RuntimeException("Sign in to confirm you are not a bot"));
        assertEquals(ErrorCode.LOGIN_REQUIRED, error.code);
        assertEquals("该视频需要登录，首版暂不支持", error.getMessage());
    }

    @Test
    public void mapsRegionRestriction() {
        UserFacingException error = YtDlpErrors.map(
                new RuntimeException("Video unavailable in your country"));
        assertEquals(ErrorCode.REGION_BLOCKED, error.code);
    }

    @Test
    public void mapsNetworkFailure() {
        UserFacingException error = YtDlpErrors.map(
                new RuntimeException("Unable to download webpage: network is unreachable"));
        assertEquals(ErrorCode.NETWORK_ERROR, error.code);
    }

    @Test
    public void keepsUnknownErrorsAsParseFailure() {
        UserFacingException error = YtDlpErrors.map(
                new RuntimeException("unexpected extractor output"));
        assertEquals(ErrorCode.PARSE_FAILED, error.code);
        assertEquals("视频解析失败，请检查链接和网络", error.getMessage());
    }
}
