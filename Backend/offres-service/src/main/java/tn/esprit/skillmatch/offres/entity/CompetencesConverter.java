package tn.esprit.skillmatch.offres.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class CompetencesConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> competences) {
        try {
            return MAPPER.writeValueAsString(competences == null ? List.of() : competences);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Conversion des competences impossible", exception);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> competences = MAPPER.readValue(json, new TypeReference<List<String>>() {});
            return competences == null ? new ArrayList<>() : competences;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON des competences invalide", exception);
        }
    }
}
