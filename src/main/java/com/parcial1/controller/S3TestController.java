package com.parcial1.controller;

import com.parcial1.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/s3-test")
@RequiredArgsConstructor
public class S3TestController {

    private final S3StorageService s3StorageService;

    @PostMapping(value = "/ticket/{ticketId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadTicketFile(
            @PathVariable String ticketId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String key = s3StorageService.uploadTicketFile(ticketId, file);

        return ResponseEntity.ok(Map.of(
                "message", "Archivo subido correctamente",
                "key", key,
                "fileName", file.getOriginalFilename(),
                "size", file.getSize()
        ));
    }

    @PostMapping(value = "/tramite/{tramiteId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadTramiteFile(
            @PathVariable String tramiteId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String key = s3StorageService.uploadTramiteFile(tramiteId, file);

        return ResponseEntity.ok(Map.of(
                "message", "Archivo subido correctamente",
                "key", key,
                "fileName", file.getOriginalFilename(),
                "size", file.getSize()
        ));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String key) {
        byte[] content = s3StorageService.download(key);
        return ResponseEntity.ok(content);
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestParam String key) {
        s3StorageService.delete(key);
        return ResponseEntity.ok(Map.of("message", "Archivo eliminado correctamente"));
    }
}