package com.taskmaster.team.adapter.out;

import com.taskmaster.team.domain.port.TeamEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventTeamEventPublisher implements TeamEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventTeamEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        if (event != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }
}
