package com.taskmaster.collaboration.domain.port;

public interface CollaborationEventPublisher {

    void publish(Object event);
}
