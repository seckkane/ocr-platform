package com.ocrplatform.document.exception;

/**
 * Violation d'une règle métier (état invalide, action non autorisée, etc.).
 * <p>
 * Exemples : tentative d'OCR sur un document déjà traité, modification
 * d'un document verrouillé, etc.
 */
public class BusinessRuleException extends BaseException {

    public BusinessRuleException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
