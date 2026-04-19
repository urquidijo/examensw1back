package com.parcial1.dto;

import com.parcial1.model.StoredFileInfo;
import com.parcial1.model.TicketStatus;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTaskInfoResponse {
    private String id;
    private String title;
    private String description;

    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private String clientReference;

    private TicketStatus status;
    private Map<String, Object> metadata;
    private List<StoredFileInfo> uploadedFiles;
}