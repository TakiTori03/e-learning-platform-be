package com.hust.orderservice.dto.event;

import org.springframework.context.ApplicationEvent;

public class OutboxAddedEvent extends ApplicationEvent {
    public OutboxAddedEvent(Object source) {
        super(source);
    }
}
