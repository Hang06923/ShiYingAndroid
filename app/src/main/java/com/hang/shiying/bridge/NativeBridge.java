package com.hang.shiying.bridge;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.JavascriptInterface;

import com.hang.shiying.core.ErrorCode;
import com.hang.shiying.core.MediaInfo;
import com.hang.shiying.core.Platform;
import com.hang.shiying.core.TaskStatus;
import com.hang.shiying.core.UrlNormalizer;
import com.hang.shiying.core.UserFacingException;
import com.hang.shiying.data.DownloadRepository;
import com.hang.shiying.data.DownloadTask;
import com.hang.shiying.download.DownloadService;
import com.hang.shiying.extractor.DouyinExtractor;
import com.hang.shiying.extractor.YtDlpExtractor;
import com.hang.shiying.extractor.YtDlpRuntime;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NativeBridge {
    public interface JsDispatcher {
        void dispatch(String javascript);
    }

    private final Context context;
    private final DownloadRepository repository;
    private final JsDispatcher dispatcher;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean parsing = new AtomicBoolean(false);

    public NativeBridge(Context context, JsDispatcher dispatcher) {
        this.context = context.getApplicationContext();
        this.repository = new DownloadRepository(this.context);
        this.dispatcher = dispatcher;
    }

    @JavascriptInterface
    public void parse(String raw, String requestId) {
        if (!parsing.compareAndSet(false, true)) {
            dispatchParseError(requestId, ErrorCode.PARSE_FAILED, "已有链接正在解析");
            return;
        }
        executor.execute(() -> {
            try {
                UrlNormalizer.Extracted link = UrlNormalizer.normalize(raw);
                MediaInfo media;
                if (link.platform == Platform.DOUYIN) {
                    media = new DouyinExtractor().extract(link.url);
                } else {
                    YtDlpRuntime.initialize(context);
                    if (!YtDlpRuntime.isReady()) {
                        throw new UserFacingException(
                                ErrorCode.PARSE_FAILED,
                                "本地视频解析组件初始化失败",
                                YtDlpRuntime.error());
                    }
                    media = new YtDlpExtractor().extract(link.url);
                }
                dispatchParseSuccess(requestId, media);
            } catch (UserFacingException error) {
                dispatchParseError(requestId, error.code, error.getMessage());
            } catch (Exception error) {
                dispatchParseError(
                        requestId,
                        ErrorCode.PARSE_FAILED,
                        "解析失败，请检查链接和网络");
            } finally {
                parsing.set(false);
            }
        });
    }

    @JavascriptInterface
    public String startDownload(String mediaJson) {
        try {
            JSONObject media = new JSONObject(mediaJson);
            String taskId = UUID.randomUUID().toString();
            DownloadTask task = new DownloadTask(
                    taskId,
                    media.getString("sourceUrl"),
                    Platform.valueOf(media.getString("platform")),
                    media.optString("title", "视频"),
                    media.optString("author", ""),
                    media.optLong("durationMs", 0),
                    System.currentTimeMillis());
            repository.create(task);
            startTask(task);
            return new JSONObject()
                    .put("ok", true)
                    .put("taskId", taskId)
                    .toString();
        } catch (Exception error) {
            return errorJson(ErrorCode.DOWNLOAD_FAILED, "无法开始下载");
        }
    }

    @JavascriptInterface
    public String retry(String taskId) {
        for (DownloadTask task : repository.listRecent(30)) {
            if (!task.id.equals(taskId)) {
                continue;
            }
            if (task.status != TaskStatus.FAILED
                    && task.status != TaskStatus.INTERRUPTED) {
                return errorJson(ErrorCode.DOWNLOAD_FAILED, "这个任务当前不能重试");
            }
            try {
                repository.requeue(task.id);
                task.status = TaskStatus.QUEUED;
                task.progress = 0;
                startTask(task);
                return new JSONObject().put("ok", true).put("taskId", task.id).toString();
            } catch (Exception error) {
                return errorJson(ErrorCode.DOWNLOAD_FAILED, "重试启动失败");
            }
        }
        return errorJson(ErrorCode.DOWNLOAD_FAILED, "没有找到这个下载任务");
    }

    @JavascriptInterface
    public void cancel(String taskId) {
        Intent intent = new Intent(context, DownloadService.class)
                .setAction(DownloadService.ACTION_CANCEL)
                .putExtra(DownloadService.EXTRA_TASK_ID, taskId);
        context.startService(intent);
    }

    @JavascriptInterface
    public String readClipboard() {
        try {
            ClipboardManager manager =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager == null || !manager.hasPrimaryClip()) {
                return "";
            }
            ClipData clip = manager.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                return "";
            }
            CharSequence text = clip.getItemAt(0).coerceToText(context);
            return text == null ? "" : text.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    @JavascriptInterface
    public String history() {
        JSONArray out = new JSONArray();
        try {
            List<DownloadTask> tasks = repository.listRecent(30);
            for (DownloadTask task : tasks) {
                out.put(taskJson(task));
            }
        } catch (Exception ignored) {
            // An empty history is safer than crashing the local UI.
        }
        return out.toString();
    }

    @JavascriptInterface
    public void openFile(String uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(uri))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {
            openDownloads();
        }
    }

    @JavascriptInterface
    public void openDownloads() {
        try {
            context.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {
            // Some minimal Android builds do not provide a file manager.
        }
    }

    private void startTask(DownloadTask task) {
        Intent intent = new Intent(context, DownloadService.class)
                .setAction(DownloadService.ACTION_START)
                .putExtra(DownloadService.EXTRA_TASK_ID, task.id)
                .putExtra(DownloadService.EXTRA_SOURCE_URL, task.sourceUrl)
                .putExtra(DownloadService.EXTRA_PLATFORM, task.platform.name())
                .putExtra(DownloadService.EXTRA_TITLE, task.title)
                .putExtra(DownloadService.EXTRA_AUTHOR, task.author)
                .putExtra(DownloadService.EXTRA_DURATION, task.durationMs);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private void dispatchParseSuccess(String requestId, MediaInfo media) {
        try {
            String json = new JSONObject()
                    .put("ok", true)
                    .put("media", mediaJson(media))
                    .toString();
            dispatcher.dispatch(
                    "window.onNativeParse && window.onNativeParse("
                            + JSONObject.quote(requestId)
                            + ","
                            + JSONObject.quote(json)
                            + ")");
        } catch (Exception ignored) {
            dispatchParseError(requestId, ErrorCode.PARSE_FAILED, "解析结果生成失败");
        }
    }

    private void dispatchParseError(
            String requestId,
            ErrorCode code,
            String message) {
        String json = errorJson(code, message);
        dispatcher.dispatch(
                "window.onNativeParse && window.onNativeParse("
                        + JSONObject.quote(requestId)
                        + ","
                        + JSONObject.quote(json)
                        + ")");
    }

    private static JSONObject mediaJson(MediaInfo media) throws Exception {
        return new JSONObject()
                .put("platform", media.platform.name())
                .put("sourceUrl", media.sourceUrl)
                .put("title", media.title)
                .put("author", media.author)
                .put("coverUrl", media.coverUrl)
                .put("durationMs", media.durationMs)
                .put("kind", media.kind.name())
                .put("imageCount", media.imageUrls.size());
    }

    private static JSONObject taskJson(DownloadTask task) throws Exception {
        return new JSONObject()
                .put("id", task.id)
                .put("sourceUrl", task.sourceUrl)
                .put("platform", task.platform.name())
                .put("title", task.title)
                .put("author", task.author)
                .put("status", task.status.name())
                .put("progress", task.progress)
                .put("localUri", task.localUri == null ? "" : task.localUri)
                .put("errorMessage", task.errorMessage == null ? "" : task.errorMessage)
                .put("createdAt", task.createdAt)
                .put("updatedAt", task.updatedAt);
    }

    private static String errorJson(ErrorCode code, String message) {
        try {
            return new JSONObject()
                    .put("ok", false)
                    .put("code", code.name())
                    .put("message", message)
                    .toString();
        } catch (Exception ignored) {
            return "{\"ok\":false,\"message\":\"操作失败\"}";
        }
    }
}
