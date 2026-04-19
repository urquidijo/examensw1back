package com.parcial1.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFileInfo {
    private String key;
    private String bucket;
    private String originalName;
    private String contentType;
    private Long size;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}