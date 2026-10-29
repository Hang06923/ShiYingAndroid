package com.hang.shiying.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.hang.shiying.MainActivity;
import com.hang.shiying.core.ErrorCode;
import com.hang.shiying.core.MediaInfo;
import com.hang.shiying.core.MediaKind;
import com.hang.shiying.core.Platform;
import com.hang.shiying.core.TaskStatus;
import com.hang.shiying.core.UserFacingException;
import com.hang.shiying.data.DownloadRepository;
import com.hang.shiying.extractor.DouyinExtractor;
import com.hang.shiying.extractor.YtDlpExtractor;
import com.hang.shiying.extractor.YtDlpRuntime;
import com.hang.shiying.storage.FileNames;
import com.hang.shiying.storage.MediaStoreExporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    public static final String ACTION_START = "com.hang.shiying.download.START";
    public static final String ACTION_CANCEL = "com.hang.shiying.download.CANCEL";
    public static final String EXTRA_TASK_ID = "taskId";
    public static final String EXTRA_SOURCE_URL = "sourceUrl";
    public static final String EXTRA_PLATFORM = "platform";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_AUTHOR = "author";
    public static final String EXTRA_DURATION = "durationMs";

    private static final int NOTIFICATION_ACTIVE = 1001;
    private static final int NOTIFICATION_COMPLETE = 1002;
    private static final String CHANNEL_ID = "downloads";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final AtomicReference<String> activeTaskId = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final FileDownloader fileDownloader = new FileDownloader();

    private DownloadRepository repository;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new DownloadRepository(this);
        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (ACTION_CANCEL.equals(intent.getAction())) {
            canceled.set(true);
            String taskId = intent.getStringExtra(EXTRA_TASK_ID);
            if (taskId != null) {
                new YtDlpExtractor().cancel(taskId);
            }
            fileDownloader.cancel();
            return START_NOT_STICKY;
        }

        if (!ACTION_START.equals(intent.getAction())) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        if (taskId == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (!running.compareAndSet(false, true)) {
            repository.fail(taskId, ErrorCode.DOWNLOAD_FAILED, "已有一个下载任务正在进行");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        canceled.set(false);
        activeTaskId.set(taskId);
        startAsForeground(buildNotification(
                intent.getStringExtra(EXTRA_TITLE),
                "准备下载",
                0,
                true));
        executor.execute(() -> executeDownload(intent));
        return START_NOT_STICKY;
    }

    private void executeDownload(Intent intent) {
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        String sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL);
        String title = intent.getStringExtra(EXTRA_TITLE);
        Platform platform = Platform.valueOf(intent.getStringExtra(EXTRA_PLATFORM));
        File tempDir = null;
        boolean saving = false;
        try {
            repository.updateProgress(taskId, TaskStatus.PREPARING, 0);
            tempDir = FileNames.tempDir(getCacheDir(), taskId);
            File mediaFile = null;
            List<File> imageFiles = null;

            if (platform == Platform.DOUYIN) {
                MediaInfo info = new DouyinExtractor().extract(sourceUrl);
                if (title == null || title.isBlank()) {
                    title = info.title;
                }
                if (info.kind == MediaKind.IMAGES) {
                    imageFiles = downloadImages(info, tempDir, intent);
                } else {
                    repository.updateProgress(taskId, TaskStatus.DOWNLOADING, 1);
                    mediaFile = fileDownloader.download(
                            info.directMediaUrl,
                            new File(tempDir, "video.mp4"),
                            canceled,
                            progress -> updateDownloadProgress(intent, progress),
                            "https://www.douyin.com/");
                }
            } else {
                YtDlpRuntime.initialize(this);
                if (!YtDlpRuntime.isReady()) {
                    throw new UserFacingException(
                            ErrorCode.PARSE_FAILED,
                            "本地视频解析组件初始化失败",
                            YtDlpRuntime.error());
                }
                mediaFile = new YtDlpExtractor().download(
                        sourceUrl,
                        tempDir,
                        taskId,
                        (progress, line) -> updateDownloadProgress(intent, progress));
            }

            if (canceled.get()) {
                repository.cancel(taskId);
                return;
            }
            repository.updateProgress(taskId, TaskStatus.PROCESSING, 96);
            saving = true;
            String localUri = imageFiles == null
                    ? MediaStoreExporter.save(
                            getContentResolver(),
                            mediaFile,
                            title)
                    : MediaStoreExporter.saveImages(
                            getContentResolver(),
                            imageFiles,
                            title);
            repository.finish(taskId, localUri);
            postCompletedNotification(title);
        } catch (UserFacingException error) {
            Log.e(TAG, "Download failed: " + taskId, error);
            if (error.code == ErrorCode.CANCELED) {
                repository.cancel(taskId);
            } else {
                repository.fail(taskId, error.code, error.getMessage());
            }
        } catch (SecurityException error) {
            Log.e(TAG, "Storage permission failed: " + taskId, error);
            repository.fail(taskId, ErrorCode.STORAGE_DENIED, "没有权限写入系统下载目录");
        } catch (Exception error) {
            Log.e(TAG, "Unexpected download failure: " + taskId, error);
            ErrorCode code;
            String message;
            if (isStorageFull(error)) {
                code = ErrorCode.STORAGE_FULL;
                message = "手机存储空间不足";
            } else if (saving) {
                code = ErrorCode.STORAGE_DENIED;
                message = "保存到手机下载目录失败";
            } else {
                code = ErrorCode.DOWNLOAD_FAILED;
                message = "下载失败，请稍后重试";
            }
            repository.fail(taskId, code, message);
        } finally {
            if (tempDir != null) {
                deleteRecursively(tempDir);
            }
            running.set(false);
            activeTaskId.set(null);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private List<File> downloadImages(
            MediaInfo info,
            File tempDir,
            Intent intent) throws UserFacingException {
        List<File> files = new ArrayList<>();
        int total = info.imageUrls.size();
        for (int index = 0; index < total; index++) {
            if (canceled.get()) {
                throw new UserFacingException(ErrorCode.CANCELED, "下载已取消");
            }
            String imageUrl = info.imageUrls.get(index);
            File target = new File(
                    tempDir,
                    String.format(
                            Locale.ROOT,
                            "image-%02d%s",
                            index + 1,
                            imageExtension(imageUrl)));
            int current = index;
            files.add(fileDownloader.download(
                    imageUrl,
                    target,
                    canceled,
                    imageProgress -> updateDownloadProgress(
                            intent,
                            Math.min(
                                    99,
                                    (current * 100 + imageProgress) / total)),
                    "https://www.douyin.com/"));
        }
        return files;
    }

    private static String imageExtension(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) {
            return ".png";
        }
        if (lower.contains(".webp")) {
            return ".webp";
        }
        return ".jpg";
    }

    private void updateDownloadProgress(Intent intent, int progress) {
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        repository.updateProgress(taskId, TaskStatus.DOWNLOADING, progress);
        Notification notification = buildNotification(
                intent.getStringExtra(EXTRA_TITLE),
                progress + "%",
                progress,
                true);
        notificationManager.notify(NOTIFICATION_ACTIVE, notification);
    }

    private void startAsForeground(Notification notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    NOTIFICATION_ACTIVE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ACTIVE, notification);
        }
    }

    private Notification buildNotification(
            String title,
            String text,
            int progress,
            boolean ongoing) {
        PendingIntent openApp = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title == null || title.isBlank() ? "拾影" : title)
                .setContentText(text)
                .setContentIntent(openApp)
                .setOnlyAlertOnce(true)
                .setOngoing(ongoing)
                .setProgress(100, Math.max(0, Math.min(100, progress)), progress <= 0)
                .build();
    }

    private void postCompletedNotification(String title) {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("已保存到 下载/拾影")
                .setContentText(title == null ? "视频下载完成" : title)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(
                        this,
                        1,
                        new Intent(this, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .build();
        notificationManager.notify(NOTIFICATION_COMPLETE, notification);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "视频下载",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("显示视频解析和下载进度");
        notificationManager.createNotificationChannel(channel);
    }

    private static boolean isStorageFull(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof IOException
                    && current.getMessage() != null
                    && current.getMessage().toLowerCase().contains("enospc")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
