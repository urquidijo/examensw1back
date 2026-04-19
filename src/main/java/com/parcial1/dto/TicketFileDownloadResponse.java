package com.parcial1.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketFileDownloadResponse {
    private byte[] content;
    private String originalName;
    private String contentType;
}