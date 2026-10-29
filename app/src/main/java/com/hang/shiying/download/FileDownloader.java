package com.hang.shiying.download;

import com.hang.shiying.core.ErrorCode;
import com.hang.shiying.core.UserFacingException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FileDownloader {
    public interface ProgressListener {
        void onProgress(int percent);
    }

    private volatile HttpURLConnection activeConnection;

    public File download(
            String mediaUrl,
            File output,
            AtomicBoolean canceled,
            ProgressListener listener,
            String referer) throws UserFacingException {
        File partial = new File(output.getAbsolutePath() + ".part");
        try {
            File parent = output.getParentFile();
            if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IllegalStateException("无法创建视频临时目录");
            }

            URLConnection base = new URL(mediaUrl).openConnection();
            HttpURLConnection connection = (HttpURLConnection) base;
            activeConnection = connection;
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/132.0 Mobile Safari/537.36");
            if (referer != null && !referer.isEmpty()) {
                connection.setRequestProperty("Referer", referer);
            }
            connection.connect();

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("HTTP " + status);
            }

            long total = connection.getContentLengthLong();
            long received = 0;
            try (InputStream input = connection.getInputStream();
                 FileOutputStream file = new FileOutputStream(partial)) {
                byte[] buffer = new byte[128 * 1024];
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    if (canceled.get()) {
                        throw new UserFacingException(ErrorCode.CANCELED, "下载已取消");
                    }
                    file.write(buffer, 0, count);
                    received += count;
                    if (listener != null) {
                        listener.onProgress(total > 0
                                ? (int) Math.min(99, received * 100 / total)
                                : 0);
                    }
                }
            }

            if (canceled.get()) {
                throw new UserFacingException(ErrorCode.CANCELED, "下载已取消");
            }
            if (partial.length() == 0) {
                throw new IllegalStateException("下载文件为空");
            }
            if (output.exists() && !output.delete()) {
                throw new IllegalStateException("无法替换已有临时文件");
            }
            if (!partial.renameTo(output)) {
                throw new IllegalStateException("无法保存下载文件");
            }
            if (listener != null) {
                listener.onProgress(100);
            }
            return output;
        } catch (UserFacingException error) {
            throw error;
        } catch (Exception error) {
            String message = canceled.get() ? "下载已取消" : "视频下载失败，请检查网络";
            ErrorCode code = canceled.get() ? ErrorCode.CANCELED : ErrorCode.DOWNLOAD_FAILED;
            throw new UserFacingException(code, message, error);
        } finally {
            HttpURLConnection connection = activeConnection;
            activeConnection = null;
            if (connection != null) {
                connection.disconnect();
            }
            if (partial.exists() && (!output.exists() || output.length() == 0)) {
                //noinspection ResultOfMethodCallIgnored
                partial.delete();
            }
        }
    }

    public void cancel() {
        HttpURLConnection connection = activeConnection;
        if (connection != null) {
            connection.disconnect();
        }
    }
}
