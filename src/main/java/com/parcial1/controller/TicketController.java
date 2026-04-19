package com.parcial1.controller;

import com.parcial1.dto.CreateTicketRequest;
import com.parcial1.dto.TicketFileDownloadResponse;
import com.parcial1.dto.TicketResponse;
import com.parcial1.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketResponse> createTicket(
            @PathVariable String projectId,
            @RequestPart("payload") CreateTicketRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(ticketService.createTicket(projectId, request, files));
    }

    @GetMapping("/{ticketId}/files/download")
    public ResponseEntity<byte[]> downloadTicketFile(
            @PathVariable String projectId,
            @PathVariable String ticketId,
            @RequestParam String key) {
        TicketFileDownloadResponse file = ticketService.downloadTicketFile(projectId, ticketId, key);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalName() + "\"")
                .header("Content-Type",
                        file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .body(file.getContent());
    }
    
}