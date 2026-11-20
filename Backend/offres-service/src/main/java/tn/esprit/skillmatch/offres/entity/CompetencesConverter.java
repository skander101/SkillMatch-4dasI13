package tn.esprit.skillmatch.offres.entity; // Place ce fichier dans le package indiqué, qui organise les classes du service.

import com.fasterxml.jackson.core.JsonProcessingException; // Importe les erreurs de lecture ou d'écriture JSON.
import com.fasterxml.jackson.core.type.TypeReference; // Importe la précision du type de liste à reconstruire depuis le JSON.
import com.fasterxml.jackson.databind.ObjectMapper; // Importe l'outil Jackson qui lit et écrit du JSON.
import jakarta.persistence.AttributeConverter; // Importe le contrat de conversion entre Java et la base.
import jakarta.persistence.Converter; // Importe l'annotation qui déclare un convertisseur JPA.

import java.util.ArrayList; // Importe une liste modifiable.
import java.util.List; // Importe le type liste.

@Converter // Déclare cette classe comme convertisseur de valeurs pour JPA.
public class CompetencesConverter implements AttributeConverter<List<String>, String> { // Convertit une liste de textes Java en texte JSON, et inversement.
    private static final ObjectMapper MAPPER = new ObjectMapper(); // Crée un outil JSON partagé par les conversions.

    @Override // Indique que la méthode réalise une opération définie par AttributeConverter.
    public String convertToDatabaseColumn(List<String> competences) { // Convertit les compétences Java avant leur stockage en base.
        try { // Essaie la conversion et permet de traiter une éventuelle erreur.
            return MAPPER.writeValueAsString(competences == null ? List.of() : competences); // Écrit la liste en JSON ; utilise une liste vide si la valeur est null.
        } catch (JsonProcessingException exception) { // Intercepte une erreur liée à la conversion JSON.
            throw new IllegalArgumentException("Conversion des competences impossible", exception); // Signale l'échec de conversion en conservant la cause.
        } // Ferme le bloc précédent (méthode, condition ou classe).
    } // Ferme le bloc précédent (méthode, condition ou classe).

    @Override // Indique que la méthode réalise une opération définie par AttributeConverter.
    public List<String> convertToEntityAttribute(String json) { // Reconstruit la liste Java à partir du texte lu en base.
        if (json == null || json.isBlank()) { // Vérifie si le texte JSON est absent ou vide.
            return new ArrayList<>(); // Retourne une liste vide et modifiable.
        } // Ferme le bloc précédent (méthode, condition ou classe).
        try { // Essaie la conversion et permet de traiter une éventuelle erreur.
            List<String> competences = MAPPER.readValue(json, new TypeReference<List<String>>() {}); // Lit le JSON en précisant que le résultat doit être une liste de textes.
            return competences == null ? new ArrayList<>() : competences; // Retourne la liste obtenue, ou une liste vide si le JSON contient null.
        } catch (JsonProcessingException exception) { // Intercepte une erreur liée à la conversion JSON.
            throw new IllegalArgumentException("JSON des competences invalide", exception); // Signale un JSON illisible au lieu de perdre silencieusement les données.
        } // Ferme le bloc précédent (méthode, condition ou classe).
    } // Ferme le bloc précédent (méthode, condition ou classe).
} // Ferme le bloc précédent (méthode, condition ou classe).
