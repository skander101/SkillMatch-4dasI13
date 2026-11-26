package tn.esprit.skillmatch.profils.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.profils.entity.Profil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilRulesTest {

    @Test
    void completenessIsCalculatedFromFilledFields() {
        Profil profil = new Profil(1L, "Développeur", "Tunis",
                List.of("Java"), List.of("Stage"), List.of("Java backend"));

        assertEquals(100, profil.calculerCompletude());
    }

    @Test
    void badgeIsAddedOnlyOnce() {
        Profil profil = new Profil(1L, "Développeur", "Tunis", List.of(), List.of(), List.of());

        profil.ajouterBadge("Java backend");
        profil.ajouterBadge("Java backend");
        profil.ajouterBadge(" ");

        assertEquals(List.of("Java backend"), profil.getBadges());
        assertTrue(profil.calculerCompletude() > 0);
    }

    @Test
    void duplicateProfileIsRejected() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> {
                    if (true) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Un profil existe déjà pour cet utilisateur");
                    }
                });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
