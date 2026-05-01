package com.ocrplatform.document.controller;

import com.ocrplatform.document.exception.DocumentNotFoundException;
import com.ocrplatform.document.exception.ErrorCode;
import com.ocrplatform.document.exception.StorageException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller temporaire pour tester le GlobalExceptionHandler.
 * À SUPPRIMER après validation de la Phase 2.
 */
@RestController
@RequestMapping("/api/test/errors")
public class TestExceptionController {

    @GetMapping("/not-found")
    public void triggerNotFound() {
        throw new DocumentNotFoundException("test-id-123");
    }

    @GetMapping("/storage")
    public void triggerStorage() {
        throw new StorageException(
                ErrorCode.STORAGE_UPLOAD_FAILED,
                "Connection refused to MinIO",
                new RuntimeException("simulated cause")
        );
    }

    @GetMapping("/generic")
    public void triggerGeneric() {
        throw new RuntimeException("Une erreur imprévue");
    }
}