package com.hust.orderservice.dto.event;

import com.hust.commonlibrary.event.OrderPaidEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderPaidInternalEvent extends ApplicationEvent {
    private final OrderPaidEvent event;

    public OrderPaidInternalEvent(Object source, OrderPaidEvent event) {
        super(source);
        this.event = event;
    }
}
