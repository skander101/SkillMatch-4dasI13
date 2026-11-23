package tn.esprit.skillmatch.profils.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        try {
            return MAPPER.writeValueAsString(values == null ? new ArrayList<>() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Impossible de sérialiser la liste", exception);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(value, TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Impossible de désérialiser la liste", exception);
        }
    }
}
