package com.ocrplatform.document.exception;

/**
 * Erreur liée au stockage objet (MinIO/S3).
 * <p>
 * Utilisée pour les échecs d'upload, de download, ou d'indisponibilité
 * du stockage.
 */
public class StorageException extends BaseException {

    public StorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public StorageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
