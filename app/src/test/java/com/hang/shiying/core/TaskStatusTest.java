package com.hang.shiying.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TaskStatusTest {
    @Test
    public void queuedCanBeginPreparing() {
        assertTrue(TaskStatus.QUEUED.canTransitionTo(TaskStatus.PREPARING));
    }

    @Test
    public void completedIsTerminal() {
        assertFalse(TaskStatus.COMPLETED.canTransitionTo(TaskStatus.DOWNLOADING));
    }

    @Test
    public void activeTaskCanBeInterruptedOnRestart() {
        assertTrue(TaskStatus.DOWNLOADING.canTransitionTo(TaskStatus.INTERRUPTED));
    }

    @Test
    public void failedTaskMustBeRequeuedBeforeDownloading() {
        assertFalse(TaskStatus.FAILED.canTransitionTo(TaskStatus.DOWNLOADING));
        assertTrue(TaskStatus.FAILED.canTransitionTo(TaskStatus.QUEUED));
    }
}
