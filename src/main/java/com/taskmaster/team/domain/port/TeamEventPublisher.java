package com.taskmaster.team.domain.port;

public interface TeamEventPublisher {

    void publish(Object event);
}
