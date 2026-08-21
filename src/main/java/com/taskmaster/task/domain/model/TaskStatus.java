package com.taskmaster.task.domain.model;

import java.util.Set;

/**
 * Task lifecycle state machine.
 */
public enum TaskStatus {
    OPEN,
    IN_PROGRESS,
    REVIEW,
    COMPLETED,
    ARCHIVED;

    public boolean canTransitionTo(TaskStatus next) {
        if (this == next) {
            return true;
        }

        return switch (this) {
            case OPEN -> Set.of(IN_PROGRESS, ARCHIVED).contains(next);
            case IN_PROGRESS -> Set.of(REVIEW, OPEN, ARCHIVED).contains(next);
            case REVIEW -> Set.of(COMPLETED, IN_PROGRESS, ARCHIVED).contains(next);
            case COMPLETED -> Set.of(ARCHIVED, OPEN).contains(next);
            case ARCHIVED -> Set.of(OPEN).contains(next);
        };
    }
}
