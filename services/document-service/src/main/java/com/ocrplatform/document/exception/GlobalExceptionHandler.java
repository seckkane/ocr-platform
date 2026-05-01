package com.ocrplatform.document.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler global des exceptions.
 * <p>
 * Convertit toutes les exceptions levées par les controllers en réponses HTTP
 * standardisées au format <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807 (Problem Details)</a>.
 * <p>
 * Chaque réponse inclut :
 * <ul>
 *     <li><b>type</b> : URI du type d'erreur (ex: /errors/document-not-found)</li>
 *     <li><b>title</b> : titre court de l'erreur</li>
 *     <li><b>status</b> : code HTTP</li>
 *     <li><b>detail</b> : message détaillé (sans info technique sensible)</li>
 *     <li><b>instance</b> : URI de la requête à l'origine de l'erreur</li>
 *     <li><b>code</b> : code métier (ex: DOC-001) — propriété personnalisée</li>
 *     <li><b>correlationId</b> : ID de corrélation (lu depuis MDC)</li>
 *     <li><b>timestamp</b> : timestamp ISO-8601</li>
 * </ul>
 *
 * <h3>Stratégie de logging</h3>
 * <ul>
 *     <li><b>5xx</b> : log {@code ERROR} avec stack trace (problèmes serveur, à investiguer)</li>
 *     <li><b>4xx (client errors)</b> : log {@code WARN} sans stack trace (erreurs attendues)</li>
 *     <li><b>Exceptions non gérées</b> : log {@code ERROR} avec stack trace, message générique au client</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_ERROR_TYPE = "https://api.ocrplatform.com/errors/";
    private static final String CORRELATION_ID_KEY = "correlationId";

    // ============================================================================
    // Exceptions métier custom (BaseException et descendants)
    // ============================================================================

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBaseException(BaseException ex) {
        HttpStatus status = ex.getHttpStatus();
        ProblemDetail problem = buildProblemDetail(
                status,
                ex.getErrorCode().getDefaultMessage(),
                ex.getMessage(),
                ex.getErrorCode().getCode(),
                ex.getDetails()
        );

        // Logging différencié et silencieux par défaut
        if (status.is5xxServerError()) {
            // 5xx : on logge avec la cause si présente, sinon juste le message (pas de stack inutile)
            if (ex.getCause() != null) {
                log.error("Server error [{}]: {} (cause: {})",
                        ex.getCode(), ex.getMessage(), ex.getCause().getMessage(), ex.getCause());
            } else {
                log.error("Server error [{}]: {}", ex.getCode(), ex.getMessage());
            }
        } else {
            // 4xx : juste un WARN court, pas de stack
            log.warn("Client error [{}]: {}", ex.getCode(), ex.getMessage());
        }

        return ResponseEntity.status(status).body(problem);
    }


    // ============================================================================
    // Validation Bean (@Valid sur DTO)
    // ============================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex) {
        // Extraction des erreurs champ par champ
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing  // en cas de doublon, on garde le premier
                ));

        Map<String, Object> details = new HashMap<>();
        details.put("fieldErrors", fieldErrors);

        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                "Validation failed for " + fieldErrors.size() + " field(s)",
                ErrorCode.VALIDATION_FAILED.getCode(),
                details
        );

        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ============================================================================
    // Validation Jakarta (sur paramètres @PathVariable, @RequestParam)
    // ============================================================================

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        cv -> cv.getMessage(),
                        (existing, replacement) -> existing
                ));

        Map<String, Object> details = new HashMap<>();
        details.put("violations", violations);

        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                "Constraint violation",
                ErrorCode.VALIDATION_FAILED.getCode(),
                details
        );

        log.warn("Constraint violation: {}", violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ============================================================================
    // Upload trop gros (Spring Multipart)
    // ============================================================================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxSizeExceeded(MaxUploadSizeExceededException ex) {
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorCode.DOCUMENT_TOO_LARGE.getDefaultMessage(),
                "Maximum upload size exceeded",
                ErrorCode.DOCUMENT_TOO_LARGE.getCode(),
                Map.of("maxSize", ex.getMaxUploadSize())
        );

        log.warn("Upload size exceeded: max={}", ex.getMaxUploadSize());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }

    // ============================================================================
    // Contraintes BDD (unique, foreign key, etc.)
    // ============================================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Important : on ne renvoie PAS le détail technique au client (peut leak des infos sur le schema)
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.CONFLICT,
                ErrorCode.CONSTRAINT_VIOLATION.getDefaultMessage(),
                "Data integrity constraint violated",
                ErrorCode.CONSTRAINT_VIOLATION.getCode(),
                Map.of()
        );

        // Côté serveur, on log le détail complet pour pouvoir investiguer
        log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    // ============================================================================
    // Route inconnue (404 propre au lieu de 500)
    // ============================================================================

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {

        ProblemDetail problem = buildProblemDetail(
                HttpStatus.NOT_FOUND,
                "Route not found",
                "The requested resource does not exist: " + ex.getResourcePath(),
                "SYS-404",
                Map.of("path", ex.getResourcePath())
        );

        log.warn("Route not found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    // ============================================================================
    // Fallback : toute exception non gérée
    // ============================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                // IMPORTANT : message générique côté client, jamais le message technique brut
                "An unexpected error occurred. Please contact support if the problem persists.",
                ErrorCode.INTERNAL_ERROR.getCode(),
                Map.of()
        );

        // Côté serveur, on a TOUT le détail (avec stack trace) pour investiguer
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // ============================================================================
    // Helper privé : construction du ProblemDetail
    // ============================================================================

    private ProblemDetail buildProblemDetail(HttpStatus status,
                                             String title,
                                             String detail,
                                             String code,
                                             Map<String, Object> details) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_ERROR_TYPE + code.toLowerCase()));

        // Propriétés personnalisées (non-RFC mais standard de fait)
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now().toString());

        // CorrelationId lu depuis MDC (rempli par le filter HTTP — Phase 5)
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }

        // Détails contextuels (fieldErrors, documentId, etc.)
        if (details != null && !details.isEmpty()) {
            details.forEach(problem::setProperty);
        }

        return problem;
    }
}