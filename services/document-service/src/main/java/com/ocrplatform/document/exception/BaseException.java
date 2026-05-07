package com.ocrplatform.document.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception métier de base.
 * <p>
 * Toutes les exceptions métier de l'application doivent hériter de cette classe.
 * Elle porte un {@link ErrorCode} et une map de détails optionnels qui seront
 * exposés dans la réponse HTTP au format ProblemDetail (RFC 7807).
 * <p>
 * <b>Convention</b> : ne JAMAIS exposer de détails techniques sensibles
 * (stack traces, requêtes SQL, credentials...) dans les détails.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    protected BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    /**
     * Ajoute un détail contextuel à l'exception (ex: "documentId" -> "abc-123").
     * <p>
     * Pattern fluent : permet le chaînage.
     */
    public BaseException withDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    public String getCode() {
        return errorCode.getCode();
    }
}
