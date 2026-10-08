package com.chatapp.media.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
public class MediaController {

    private final Path uploadLocation = Paths.get("./uploads").toAbsolutePath().normalize();

    public MediaController() {
        try {
            Files.createDirectories(uploadLocation);
            log.info("Media upload directory initialized at: {}", uploadLocation);
        } catch (IOException e) {
            log.error("Could not initialize upload directory: {}", e.getMessage());
        }
    }

    /**
     * Upload an image, PDF or document
     * POST /media/upload
     */
    @PostMapping("/media/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot upload empty file"));
        }

        try {
            String originalFileName = StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "file_" + System.currentTimeMillis()
            );

            // Determine message type
            String contentType = file.getContentType();
            String messageType = "document";
            if (contentType != null && contentType.startsWith("image/")) {
                messageType = "image";
            } else if ((contentType != null && contentType.equalsIgnoreCase("application/pdf"))
                    || originalFileName.toLowerCase().endsWith(".pdf")) {
                messageType = "pdf";
            }

            // Create unique file name
            String fileExtension = "";
            int extIdx = originalFileName.lastIndexOf(".");
            if (extIdx > 0) {
                fileExtension = originalFileName.substring(extIdx);
            } else if (contentType != null) {
                if (contentType.contains("jpeg") || contentType.contains("jpg")) fileExtension = ".jpg";
                else if (contentType.contains("png")) fileExtension = ".png";
                else if (contentType.contains("webp")) fileExtension = ".webp";
                else if (contentType.contains("pdf")) fileExtension = ".pdf";
            }
            String uniqueFileName = UUID.randomUUID() + fileExtension;

            Path targetPath = this.uploadLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + uniqueFileName;

            Map<String, Object> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            response.put("fileName", originalFileName);
            response.put("fileType", contentType != null ? contentType : "application/octet-stream");
            response.put("fileSize", file.getSize());
            response.put("messageType", messageType);

            log.info("File uploaded successfully: {} -> {}", originalFileName, fileUrl);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "File upload failed: " + e.getMessage()));
        }
    }

    /**
     * Serve uploaded images and PDF documents directly
     * GET /uploads/{fileName}
     */
    @GetMapping({"/uploads/{fileName:.+}", "/uploads/{fileName}"})
    public ResponseEntity<Resource> serveFile(@PathVariable("fileName") String fileName) {
        try {
            Path filePath = this.uploadLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
