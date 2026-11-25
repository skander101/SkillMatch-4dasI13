
package tn.esprit.skillmatch.offres.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;


// ========== CLASSE CONVERTER ==========

// Indique que cette classe est un convertisseur JPA.
@Converter

// Convertit une liste Java en texte JSON et inversement.
public class CompetencesConverter implements AttributeConverter<List<String>, String> {


    // ========== OUTIL DE CONVERSION JSON ==========

    // Crée un outil partagé pour effectuer les conversions JSON.
    private static final ObjectMapper MAPPER = new ObjectMapper();


    // ========== 1. CONVERTIR JAVA VERS MYSQL ==========

    // Indique que cette méthode vient de l'interface AttributeConverter.
    @Override

    // Transforme une liste de compétences Java en texte JSON.
    public String convertToDatabaseColumn(List<String> competences) {

        // Essaie d'effectuer la conversion.
        try {

            // Convertit la liste en JSON.
            // Utilise une liste vide si les compétences sont null.
            return MAPPER.writeValueAsString(competences == null ? List.of() : competences);

            // Récupère les erreurs de conversion JSON.
        } catch (JsonProcessingException exception) {

            // Déclenche une erreur si la conversion échoue.
            throw new IllegalArgumentException("Conversion des competences impossible", exception);
        }
    }


    // ========== 2. CONVERTIR MYSQL VERS JAVA ==========

    // Indique que cette méthode vient de l'interface AttributeConverter.
    @Override

    // Transforme un texte JSON récupéré depuis MySQL en liste Java.
    public List<String> convertToEntityAttribute(String json) {

        // Vérifie si le JSON est absent ou vide.
        if (json == null || json.isBlank()) {

            // Retourne une liste vide et modifiable.
            return new ArrayList<>();
        }

        // Essaie de convertir le JSON en liste Java.
        try {

            // Lit le JSON et le transforme en liste de compétences.
            List<String> competences = MAPPER.readValue(json, new TypeReference<List<String>>() {});

            // Retourne les compétences ou une liste vide si le résultat est null.
            return competences == null ? new ArrayList<>() : competences;
        }

        // Récupère les erreurs si le JSON est invalide.
        catch (JsonProcessingException exception) {

            // Déclenche une erreur si la lecture du JSON échoue.
            throw new IllegalArgumentException("JSON des competences invalide", exception);
        }
    }

}
