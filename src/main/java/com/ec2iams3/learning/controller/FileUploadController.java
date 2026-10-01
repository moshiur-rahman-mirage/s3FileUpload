package com.ec2iams3.learning.controller;

import com.ec2iams3.learning.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/upload")
@Tag(name = "File Upload", description = "Upload files to the configured storage backend")
public class FileUploadController {
    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/files")
    @Operation(summary = "List uploaded files", description = "Returns all file names/keys currently stored in the configured backend.")
    public ResponseEntity<List<String>> listFiles() {
        return ResponseEntity.ok(fileStorageService.listFiles());
    }

    @GetMapping("/download/{fileName}")
    @Operation(summary = "Download a file by name", description = "Downloads a file from the configured backend using the stored key/name.")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.downloadFile(fileName);

        String contentType = "application/octet-stream";
        String originalName = fileName;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/{fileName}")
    @Operation(summary = "Delete a file by name", description = "Deletes a stored file by its generated key/name from the configured backend.")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileName) {
        fileStorageService.deleteFile(fileName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an invoice file", description = "Stores the uploaded invoice in the configured backend and returns the generated key.")
    public ResponseEntity<String> uploadInvoice(@Parameter(description = "Invoice file", required = true, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary"))) @RequestPart("file") MultipartFile file) {
        String location = fileStorageService.upload(file, FileStorageService.FileCategory.INVOICE);
        return ResponseEntity.ok(location);
    }

    @PostMapping(value = "/profileimage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a profile image", description = "Stores the uploaded profile image in the configured backend and returns the generated key.")
    public ResponseEntity<String> uploadProfileImage(@Parameter(description = "Profile image file", required = true, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary"))) @RequestPart("file") MultipartFile file) {
        String location = fileStorageService.upload(file, FileStorageService.FileCategory.PROFILE_IMAGE);
        return ResponseEntity.ok(location);
    }
}