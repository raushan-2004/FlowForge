package com.flowforge.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Converter
public class HeadersConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            Map<String, String> normalized = new HashMap<>();
            for (Map.Entry<String, String> entry : attribute.entrySet()) {
                if (entry.getKey() != null) {
                    String normKey = entry.getKey().trim().toLowerCase();
                    String normValue = entry.getValue() != null ? entry.getValue().trim() : "";
                    normalized.put(normKey, normValue);
                }
            }
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize headers to JSON", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize headers from JSON", e);
        }
    }
}
