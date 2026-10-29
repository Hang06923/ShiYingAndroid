package com.hang.shiying.extractor;

import com.hang.shiying.core.MediaInfo;
import com.hang.shiying.core.Platform;
import com.hang.shiying.core.UserFacingException;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import kotlin.Unit;

public final class YtDlpExtractor implements MediaExtractor {
    public interface ProgressListener {
        void onProgress(int percent, String line);
    }

    @Override
    public MediaInfo extract(String normalizedUrl) throws UserFacingException {
        try {
            YoutubeDLRequest request = new YoutubeDLRequest(normalizedUrl);
            request.addOption("--no-playlist");
            request.addOption("--no-warnings");
            VideoInfo info = YoutubeDL.getInstance().getInfo(request);
            return new MediaInfo(
                    platformOf(normalizedUrl),
                    normalizedUrl,
                    info.getTitle(),
                    info.getUploader(),
                    info.getThumbnail(),
                    Math.max(0, info.getDuration()) * 1000L,
                    info.getUrl());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new UserFacingException(
                    com.hang.shiying.core.ErrorCode.CANCELED,
                    "解析已取消",
                    error);
        } catch (Exception error) {
            throw YtDlpErrors.map(error);
        }
    }

    public File download(
            String normalizedUrl,
            File outputDir,
            String taskId,
            ProgressListener listener) throws UserFacingException {
        try {
            YoutubeDLRequest request = new YoutubeDLRequest(normalizedUrl);
            request.addOption("--no-playlist");
            request.addOption("--no-mtime");
            request.addOption("-f",
                    "bv*[ext=mp4][vcodec^=avc1]+ba[ext=m4a]/b[ext=mp4]/b");
            request.addOption("--merge-output-format", "mp4");
            request.addOption(
                    "-o",
                    new File(outputDir, "%(title).80B.%(ext)s").getAbsolutePath());
            request.addOption("--windows-filenames");

            YoutubeDL.getInstance().execute(
                    request,
                    taskId,
                    (Float progress, Long etaSeconds, String line) -> {
                        listener.onProgress(
                                progress == null ? 0 : progress.intValue(),
                                line == null ? "" : line);
                        return Unit.INSTANCE;
                    });

            File[] files = outputDir.listFiles((dir, name) ->
                    name.toLowerCase(Locale.ROOT).endsWith(".mp4"));
            if (files == null || files.length == 0) {
                throw new IllegalStateException("yt-dlp 未生成 MP4 文件");
            }
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            return files[0];
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new UserFacingException(
                    com.hang.shiying.core.ErrorCode.CANCELED,
                    "下载已取消",
                    error);
        } catch (Exception error) {
            throw YtDlpErrors.map(error);
        }
    }

    public void cancel(String taskId) {
        YoutubeDL.getInstance().destroyProcessById(taskId);
    }

    private static Platform platformOf(String url) {
        return url.contains("x.com") || url.contains("twitter.com")
                ? Platform.X
                : Platform.YOUTUBE;
    }
}
