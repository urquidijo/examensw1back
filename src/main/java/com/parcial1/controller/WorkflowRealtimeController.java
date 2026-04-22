package com.parcial1.controller;

import com.parcial1.dto.realtime.WorkflowRealtimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WorkflowRealtimeController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/workflows/{workflowId}/sync")
    public void syncWorkflow(
        @DestinationVariable String workflowId,
        WorkflowRealtimeMessage message
    ) {
        messagingTemplate.convertAndSend(
            "/topic/workflows/" + workflowId,
            message
        );
    }
}