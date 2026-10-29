package com.hang.shiying.core;

import java.util.EnumSet;
import java.util.Set;

public enum TaskStatus {
    QUEUED,
    PREPARING,
    DOWNLOADING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED,
    INTERRUPTED;

    public boolean canTransitionTo(TaskStatus next) {
        return allowed(this).contains(next);
    }

    private static Set<TaskStatus> allowed(TaskStatus status) {
        switch (status) {
            case QUEUED:
                return EnumSet.of(PREPARING, FAILED, CANCELED);
            case PREPARING:
                return EnumSet.of(DOWNLOADING, FAILED, CANCELED, INTERRUPTED);
            case DOWNLOADING:
                return EnumSet.of(PROCESSING, FAILED, CANCELED, INTERRUPTED);
            case PROCESSING:
                return EnumSet.of(COMPLETED, FAILED, INTERRUPTED);
            case FAILED:
            case INTERRUPTED:
                return EnumSet.of(QUEUED);
            default:
                return EnumSet.noneOf(TaskStatus.class);
        }
    }
}
