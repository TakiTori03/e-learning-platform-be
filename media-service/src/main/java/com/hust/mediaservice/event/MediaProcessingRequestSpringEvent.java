package com.hust.mediaservice.event;

import com.hust.commonlibrary.event.MediaProcessingRequestEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MediaProcessingRequestSpringEvent extends ApplicationEvent {
    private final MediaProcessingRequestEvent kafkaPayload;

    public MediaProcessingRequestSpringEvent(Object source, MediaProcessingRequestEvent kafkaPayload) {
        super(source);
        this.kafkaPayload = kafkaPayload;
    }
}
