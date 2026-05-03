package com.ocrplatform.document.controller;

import com.ocrplatform.document.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Outil DEV : valide isolément le StorageService avant câblage avec DocumentService.
 * Profile-protégé.
 */
@Profile("!prod")
@Slf4j
@RestController
@RequestMapping("/api/test/storage")
@RequiredArgsConstructor
public class TestStorageController {

    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {
        String key = storageService.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
        return "[STORED] key=" + key;
    }
}