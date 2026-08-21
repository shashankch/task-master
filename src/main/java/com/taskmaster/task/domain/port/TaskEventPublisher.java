package com.taskmaster.task.domain.port;

/**
 * Port interface for publishing task lifecycle domain events.
 */
public interface TaskEventPublisher {

    void publish(Object event);
}
