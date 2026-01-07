package com.paul.csvpipeline.backend.csvupload;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/uploads")
public class CsvUploadController {

    private final CsvUploadService service;

    public CsvUploadController(CsvUploadService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<CsvUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        CsvUploadResponse response = service.registerUpload(file, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public List<CsvUploadResponse> list(Authentication authentication) {
        return service.listUploads(authentication.getName());
    }

    @GetMapping("/{id}")
    public CsvUploadResponse get(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.getUpload(id, authentication.getName());
    }
}