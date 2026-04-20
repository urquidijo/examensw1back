package com.parcial1.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMonitorResponse {
    private TicketResponse ticket;
    private TicketMonitorSummaryResponse summary;
    private List<TicketMonitorStepResponse> timeline;
}