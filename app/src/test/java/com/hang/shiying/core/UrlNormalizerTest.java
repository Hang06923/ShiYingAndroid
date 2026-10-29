package com.hang.shiying.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class UrlNormalizerTest {
    @Test
    public void extractsDouyinUrlFromShareText() throws Exception {
        UrlNormalizer.Extracted result = UrlNormalizer.normalize(
                "3.14 复制打开抖音 https://v.douyin.com/abc123/ 看看");

        assertEquals(Platform.DOUYIN, result.platform);
        assertEquals("abc123", result.id);
    }

    @Test
    public void normalizesYouTubeShort() throws Exception {
        UrlNormalizer.Extracted result = UrlNormalizer.normalize(
                "https://youtube.com/shorts/abcdefghijk?feature=share");

        assertEquals(Platform.YOUTUBE, result.platform);
        assertEquals("abcdefghijk", result.id);
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", result.url);
    }

    @Test
    public void normalizesXPostWithoutTrackingQuery() throws Exception {
        UrlNormalizer.Extracted result = UrlNormalizer.normalize(
                "https://twitter.com/example/status/1234567890?s=20");

        assertEquals(Platform.X, result.platform);
        assertEquals("1234567890", result.id);
        assertEquals("https://x.com/example/status/1234567890", result.url);
    }

    @Test
    public void rejectsUnknownHost() {
        try {
            UrlNormalizer.normalize("https://example.com/video/1");
            fail("expected UserFacingException");
        } catch (UserFacingException error) {
            assertEquals(ErrorCode.UNSUPPORTED_URL, error.code);
        }
    }
}
