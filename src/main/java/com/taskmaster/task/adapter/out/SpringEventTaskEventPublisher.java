package com.taskmaster.task.adapter.out;

import com.taskmaster.task.domain.port.TaskEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter publishing domain events using Spring ApplicationEventPublisher.
 */
@Component
public class SpringEventTaskEventPublisher implements TaskEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventTaskEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        if (event != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }
}
