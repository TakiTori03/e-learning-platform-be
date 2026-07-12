package com.hust.mediaservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import com.hust.mediaservice.entity.enums.MediaType;
import com.hust.mediaservice.entity.enums.StorageProvider;

@Document(collection = "media")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Media extends BaseDocument {
    private String fileName;
    private MediaType fileType; 
    private String contentType;
    private Long fileSize;
    private String url;
    private StorageProvider provider;
    private String ownerId;
    private String referenceId;
    private String hlsFolderName;
    private String rawFileKey;
    private String transcriptUrl;
    private Double duration;

    @Builder.Default
    private MediaStatus status = MediaStatus.READY;

    public enum MediaStatus {
        PENDING, PROCESSING, READY, FAILED
    }
}
