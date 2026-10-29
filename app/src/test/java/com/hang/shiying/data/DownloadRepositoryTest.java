package com.hang.shiying.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.hang.shiying.core.ErrorCode;
import com.hang.shiying.core.Platform;
import com.hang.shiying.core.TaskStatus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class DownloadRepositoryTest {
    private DownloadRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("downloads.db");
        repository = new DownloadRepository(context);
    }

    @Test
    public void storesProgressAndCompletedFileUri() {
        repository.create(task("task-1", TaskStatus.QUEUED));

        repository.updateProgress("task-1", TaskStatus.DOWNLOADING, 42);
        repository.finish("task-1", "content://media/video/1");

        DownloadTask saved = repository.listRecent(1).get(0);
        assertEquals(TaskStatus.COMPLETED, saved.status);
        assertEquals(100, saved.progress);
        assertEquals("content://media/video/1", saved.localUri);
        assertNull(saved.errorCode);
    }

    @Test
    public void marksUnfinishedTasksInterrupted() {
        repository.create(task("active", TaskStatus.QUEUED));
        repository.updateProgress("active", TaskStatus.DOWNLOADING, 55);
        repository.create(task("done", TaskStatus.QUEUED));
        repository.finish("done", "content://media/video/2");

        repository.markActiveTasksInterrupted();
        List<DownloadTask> tasks = repository.listRecent(10);

        DownloadTask active = find(tasks, "active");
        DownloadTask done = find(tasks, "done");
        assertEquals(TaskStatus.INTERRUPTED, active.status);
        assertEquals(ErrorCode.INTERRUPTED.name(), active.errorCode);
        assertEquals(TaskStatus.COMPLETED, done.status);
    }

    private static DownloadTask task(String id, TaskStatus status) {
        DownloadTask task = new DownloadTask(
                id,
                "https://www.douyin.com/video/7569541184671974899",
                Platform.DOUYIN,
                "测试视频",
                "测试作者",
                12000,
                1L);
        task.status = status;
        return task;
    }

    private static DownloadTask find(List<DownloadTask> tasks, String id) {
        for (DownloadTask task : tasks) {
            if (id.equals(task.id)) {
                assertNotNull(task);
                return task;
            }
        }
        throw new AssertionError("missing task " + id);
    }
}
