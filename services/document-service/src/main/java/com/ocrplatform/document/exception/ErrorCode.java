package com.ocrplatform.document.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Codes d'erreur métier centralisés.
 * <p>
 * Chaque code est unique, stable, et exposé au client. Les clients
 * peuvent les utiliser pour réagir programmatiquement aux erreurs
 * (i18n, retry logic, redirection, etc.).
 * <p>
 * Convention de nommage :
 * <ul>
 *   <li>{@code DOC-xxx} : erreurs liées aux documents</li>
 *   <li>{@code STG-xxx} : erreurs liées au stockage (MinIO)</li>
 *   <li>{@code BUS-xxx} : erreurs de règles métier</li>
 *   <li>{@code VAL-xxx} : erreurs de validation</li>
 *   <li>{@code SYS-xxx} : erreurs système / fallback</li>
 * </ul>
 */
@Getter
public enum ErrorCode {

    // ===== Documents =====
    DOCUMENT_NOT_FOUND("DOC-001", HttpStatus.NOT_FOUND, "Document not found"),
    DOCUMENT_INVALID_FORMAT("DOC-002", HttpStatus.UNPROCESSABLE_ENTITY, "Invalid document format"),
    DOCUMENT_TOO_LARGE("DOC-003", HttpStatus.PAYLOAD_TOO_LARGE, "Document exceeds maximum allowed size"),
    DOCUMENT_EMPTY("DOC-004", HttpStatus.BAD_REQUEST, "Document content is empty"),

    // ===== Stockage (MinIO) =====
    STORAGE_UPLOAD_FAILED("STG-001", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload document to storage"),
    STORAGE_DOWNLOAD_FAILED("STG-002", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve document from storage"),
    STORAGE_UNAVAILABLE("STG-003", HttpStatus.SERVICE_UNAVAILABLE, "Storage service is unavailable"),

    // ===== Règles métier =====
    BUSINESS_RULE_VIOLATION("BUS-001", HttpStatus.CONFLICT, "Business rule violation"),
    DOCUMENT_ALREADY_PROCESSED("BUS-002", HttpStatus.CONFLICT, "Document has already been processed"),

    // ===== Validation =====
    VALIDATION_FAILED("VAL-001", HttpStatus.BAD_REQUEST, "Request validation failed"),
    CONSTRAINT_VIOLATION("VAL-002", HttpStatus.BAD_REQUEST, "Database constraint violation"),

    // ===== Système =====
    ROUTE_NOT_FOUND("SYS-404", HttpStatus.NOT_FOUND, "Route not found"),
    INTERNAL_ERROR("SYS-001", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    SERVICE_UNAVAILABLE("SYS-002", HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");


    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
