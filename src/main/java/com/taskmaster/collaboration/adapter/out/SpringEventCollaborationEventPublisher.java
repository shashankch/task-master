package com.taskmaster.collaboration.adapter.out;

import com.taskmaster.collaboration.domain.port.CollaborationEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventCollaborationEventPublisher implements CollaborationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventCollaborationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        if (event != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }
}
