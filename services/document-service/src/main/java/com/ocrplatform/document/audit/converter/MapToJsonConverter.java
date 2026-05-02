package com.ocrplatform.document.audit.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Convertit automatiquement un {@link Map} Java ↔ chaîne JSON pour la BDD.
 * <p>
 * Utilisé sur les colonnes de type {@code JSON} (MySQL 5.7+) pour stocker
 * des structures flexibles sans nécessiter de migration à chaque ajout
 * de champ.
 * <p>
 * Hibernate appelle automatiquement ce converter grâce à l'annotation
 * {@code @Convert} sur le champ de l'entity.
 */
@Slf4j
@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String, Object>, String> {

    /**
     * ObjectMapper partagé (thread-safe par design).
     * Configuration minimaliste : sérialisation JSON standard.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * TypeReference pour préserver le typage générique lors de la désérialisation
     * (sinon Jackson retourne un {@code Map<String, Object>} brut).
     */
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Failed to serialize Map to JSON: {}", attribute, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, MAP_TYPE_REF);
        } catch (Exception e) {
            log.error("Failed to deserialize JSON to Map: {}", dbData, e);
            return new HashMap<>();
        }
    }
}

