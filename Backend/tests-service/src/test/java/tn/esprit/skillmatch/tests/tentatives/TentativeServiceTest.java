package tn.esprit.skillmatch.tests.tentatives;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.tests.catalogue.Question;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TentativeServiceTest {
    private final List<Question> questions = List.of(
            new Question("java", "Type primitif ?", List.of("String", "int"), 1),
            new Question("http", "Succès HTTP ?", List.of("200", "403"), 0),
            new Question("git", "Commande de lecture ?", List.of("log", "merge"), 0)
    );

    @Test
    void attribueUnScoreEntierSansFavoriserLesQuestionsOmises() {
        assertThat(TentativeService.corriger(questions, Map.of("java", 1, "http", 0))).isEqualTo(66);
        assertThat(TentativeService.corriger(questions, Map.of("java", 0))).isZero();
        assertThat(TentativeService.corriger(questions, Map.of("java", 1, "http", 0, "git", 0))).isEqualTo(100);
    }

    @Test
    void rejetteLesQuestionsEtOptionsInconnues() {
        verifierReponseInvalide(Map.of("inconnue", 0));
        verifierReponseInvalide(Map.of("java", 2));
        verifierReponseInvalide(Map.of("java", -1));
        verifierReponseInvalide(Map.of());
    }

    private void verifierReponseInvalide(Map<String, Integer> reponses) {
        var erreur = assertThrows(ResponseStatusException.class, () -> TentativeService.corriger(questions, reponses));
        assertThat(erreur.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
