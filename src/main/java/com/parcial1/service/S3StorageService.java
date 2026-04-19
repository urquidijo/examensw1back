package com.parcial1.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.parcial1.model.StoredFileInfo;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucket;

    public S3StorageService(
            S3Client s3Client,
            @Value("${app.aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public String uploadTicketFile(String ticketId, MultipartFile file) throws IOException {
        String key = buildKey("tickets", ticketId, file.getOriginalFilename());
        upload(file, key);
        return key;
    }

    public String uploadTramiteFile(String tramiteId, MultipartFile file) throws IOException {
        String key = buildKey("tramites", tramiteId, file.getOriginalFilename());
        upload(file, key);
        return key;
    }

    public byte[] download(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    private void upload(MultipartFile file, String key) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(inputStream, file.getSize()));
        }
    }

    public StoredFileInfo uploadTaskTramiteFile(String ticketId, String taskId, MultipartFile file, String uploadedBy)
            throws IOException {
        String key = buildTaskTramiteKey(ticketId, taskId, file.getOriginalFilename());
        upload(file, key);

        return StoredFileInfo.builder()
                .key(key)
                .bucket(bucket)
                .originalName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(uploadedBy)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    private String buildTaskTramiteKey(String ticketId, String taskId, String originalFilename) {
        String safeName = sanitizeFilename(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8);

        return "tramites/" + ticketId + "/" + taskId + "/" + timestamp + "-" + random + "-" + safeName;
    }

    private String buildKey(String section, String entityId, String originalFilename) {
        String safeName = sanitizeFilename(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8);

        return section + "/" + entityId + "/" + timestamp + "-" + random + "-" + safeName;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "archivo";
        }

        String normalized = Normalizer.normalize(filename, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String safe = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        return safe.isBlank() ? "archivo" : safe;
    }
}