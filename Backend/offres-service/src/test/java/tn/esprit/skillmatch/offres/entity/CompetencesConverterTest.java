package tn.esprit.skillmatch.offres.entity;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompetencesConverterTest {
    private final CompetencesConverter converter = new CompetencesConverter();

    @Test
    void conserveLesCaracteresSpeciauxDansLeJson() {
        List<String> competences = List.of("C++", "Analyse de données", "Java \"avancé\"");
        assertEquals(competences,
                converter.convertToEntityAttribute(converter.convertToDatabaseColumn(competences)));
    }

    @Test
    void gereLesValeursVidesEtRefuseLeJsonInvalide() {
        assertEquals("[]", converter.convertToDatabaseColumn(null));
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute("pas du JSON"));
    }
}
