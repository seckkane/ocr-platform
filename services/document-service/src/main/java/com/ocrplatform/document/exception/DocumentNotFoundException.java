package com.ocrplatform.document.exception;

/**
 * Levée quand un document recherché n'existe pas en base de données.
 */
public class DocumentNotFoundException extends BaseException {

    public DocumentNotFoundException(String documentId) {
        super(ErrorCode.DOCUMENT_NOT_FOUND, "Document not found with id: " + documentId);
        withDetail("documentId", documentId);
    }
}