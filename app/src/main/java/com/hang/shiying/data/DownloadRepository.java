package com.hang.shiying.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hang.shiying.core.ErrorCode;
import com.hang.shiying.core.Platform;
import com.hang.shiying.core.TaskStatus;

import java.util.ArrayList;
import java.util.List;

public final class DownloadRepository extends SQLiteOpenHelper {
    private static final String DATABASE = "downloads.db";
    private static final String TABLE = "download_tasks";

    public DownloadRepository(Context context) {
        super(context, DATABASE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "id TEXT PRIMARY KEY,"
                + "source_url TEXT NOT NULL,"
                + "platform TEXT NOT NULL,"
                + "title TEXT NOT NULL,"
                + "author TEXT,"
                + "duration_ms INTEGER,"
                + "status TEXT NOT NULL,"
                + "progress INTEGER NOT NULL DEFAULT 0,"
                + "local_uri TEXT,"
                + "error_code TEXT,"
                + "error_message TEXT,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX tasks_updated ON " + TABLE + "(updated_at DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public void create(DownloadTask task) {
        getWritableDatabase().insertOrThrow(TABLE, null, values(task));
    }

    public void updateProgress(String id, TaskStatus status, int progress) {
        ContentValues values = new ContentValues();
        values.put("status", status.name());
        values.put("progress", Math.max(0, Math.min(100, progress)));
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update(TABLE, values, "id=?", new String[]{id});
    }

    public void finish(String id, String localUri) {
        ContentValues values = new ContentValues();
        values.put("status", TaskStatus.COMPLETED.name());
        values.put("progress", 100);
        values.put("local_uri", localUri);
        values.putNull("error_code");
        values.putNull("error_message");
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update(TABLE, values, "id=?", new String[]{id});
    }

    public void fail(String id, ErrorCode code, String message) {
        ContentValues values = new ContentValues();
        values.put("status", TaskStatus.FAILED.name());
        values.put("error_code", code.name());
        values.put("error_message", message);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update(TABLE, values, "id=?", new String[]{id});
    }

    public void cancel(String id) {
        ContentValues values = new ContentValues();
        values.put("status", TaskStatus.CANCELED.name());
        values.put("error_code", ErrorCode.CANCELED.name());
        values.put("error_message", "下载已取消");
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update(TABLE, values, "id=?", new String[]{id});
    }

    public void requeue(String id) {
        ContentValues values = new ContentValues();
        values.put("status", TaskStatus.QUEUED.name());
        values.put("progress", 0);
        values.putNull("local_uri");
        values.putNull("error_code");
        values.putNull("error_message");
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update(TABLE, values, "id=?", new String[]{id});
    }

    public void markActiveTasksInterrupted() {
        ContentValues values = new ContentValues();
        values.put("status", TaskStatus.INTERRUPTED.name());
        values.put("error_code", ErrorCode.INTERRUPTED.name());
        values.put("error_message", "上次下载被中断，可以重试");
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update(
                TABLE,
                values,
                "status IN (?,?,?,?)",
                new String[]{
                        TaskStatus.QUEUED.name(),
                        TaskStatus.PREPARING.name(),
                        TaskStatus.DOWNLOADING.name(),
                        TaskStatus.PROCESSING.name()
                });
    }

    public List<DownloadTask> listRecent(int limit) {
        List<DownloadTask> out = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE,
                null,
                null,
                null,
                null,
                null,
                "updated_at DESC",
                Integer.toString(Math.max(1, limit)))) {
            while (cursor.moveToNext()) {
                out.add(read(cursor));
            }
        }
        return out;
    }

    private static ContentValues values(DownloadTask task) {
        ContentValues values = new ContentValues();
        values.put("id", task.id);
        values.put("source_url", task.sourceUrl);
        values.put("platform", task.platform.name());
        values.put("title", task.title);
        values.put("author", task.author);
        values.put("duration_ms", task.durationMs);
        values.put("status", task.status.name());
        values.put("progress", task.progress);
        values.put("local_uri", task.localUri);
        values.put("error_code", task.errorCode);
        values.put("error_message", task.errorMessage);
        values.put("created_at", task.createdAt);
        values.put("updated_at", task.updatedAt);
        return values;
    }

    private static DownloadTask read(Cursor cursor) {
        DownloadTask task = new DownloadTask(
                cursor.getString(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("source_url")),
                Platform.valueOf(cursor.getString(
                        cursor.getColumnIndexOrThrow("platform"))),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("author")),
                cursor.getLong(cursor.getColumnIndexOrThrow("duration_ms")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        task.status = TaskStatus.valueOf(
                cursor.getString(cursor.getColumnIndexOrThrow("status")));
        task.progress = cursor.getInt(cursor.getColumnIndexOrThrow("progress"));
        task.localUri = cursor.getString(cursor.getColumnIndexOrThrow("local_uri"));
        task.errorCode = cursor.getString(cursor.getColumnIndexOrThrow("error_code"));
        task.errorMessage = cursor.getString(
                cursor.getColumnIndexOrThrow("error_message"));
        task.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
        return task;
    }
}
