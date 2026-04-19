package com.parcial1.controller;

import com.parcial1.dto.CreateTicketRequest;
import com.parcial1.dto.TicketResponse;
import com.parcial1.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getTickets(@PathVariable String projectId) {
        return ResponseEntity.ok(ticketService.getTickets(projectId));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @PathVariable String projectId,
            @RequestBody CreateTicketRequest request
    ) {
        return ResponseEntity.ok(ticketService.createTicket(projectId, request));
    }
}