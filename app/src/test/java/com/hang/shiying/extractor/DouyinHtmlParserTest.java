package com.hang.shiying.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.hang.shiying.core.MediaInfo;
import com.hang.shiying.core.MediaKind;
import com.hang.shiying.core.Platform;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public final class DouyinHtmlParserTest {
    @Test
    public void parsesVideoMetadataAndRemovesWatermarkPath() throws Exception {
        InputStream input = getClass().getResourceAsStream("/fixtures/douyin-share.html");
        assertNotNull(input);
        String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);

        MediaInfo info = DouyinHtmlParser.parse(
                html,
                "https://www.douyin.com/video/7569541184671974899");

        assertEquals(Platform.DOUYIN, info.platform);
        assertEquals("测试视频", info.title);
        assertEquals("测试作者", info.author);
        assertEquals(12000, info.durationMs);
        assertEquals("https://p3.example.com/cover.jpg", info.coverUrl);
        assertEquals(
                "https://aweme.snssdk.com/aweme/v1/play/?video_id=v0200fg10000",
                info.directMediaUrl);
    }

    @Test
    public void parsesImageGalleryAndPrefersJpegOriginal() throws Exception {
        InputStream input = getClass().getResourceAsStream("/fixtures/douyin-images.html");
        assertNotNull(input);
        String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);

        MediaInfo info = DouyinHtmlParser.parse(
                html,
                "https://www.douyin.com/video/7672671369703853312");

        assertEquals(MediaKind.IMAGES, info.kind);
        assertEquals("两张测试图片", info.title);
        assertEquals("图片作者", info.author);
        assertEquals(2, info.imageUrls.size());
        assertEquals("https://p3.example.com/one.jpeg", info.imageUrls.get(0));
        assertEquals("https://p3.example.com/two.jpeg", info.imageUrls.get(1));
        assertEquals("https://p3.example.com/one.jpeg", info.coverUrl);
    }
}
