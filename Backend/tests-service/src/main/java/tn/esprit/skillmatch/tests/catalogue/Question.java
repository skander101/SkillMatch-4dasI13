package tn.esprit.skillmatch.tests.catalogue;

import java.util.List;

public record Question(String id, String enonce, List<String> options, int indexBonneReponse) {

    public Question {
        if (id == null || !id.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,39}")) {
            throw new IllegalArgumentException("L'identifiant de question est invalide.");
        }
        if (enonce == null || enonce.isBlank() || enonce.length() > 1000) {
            throw new IllegalArgumentException("L'énoncé doit contenir entre 1 et 1000 caractères.");
        }
        if (options == null || options.size() < 2 || options.size() > 8
                || options.stream().anyMatch(option -> option == null || option.isBlank() || option.length() > 300)) {
            throw new IllegalArgumentException("Une question doit comporter entre deux et huit options valides.");
        }
        if (indexBonneReponse < 0 || indexBonneReponse >= options.size()) {
            throw new IllegalArgumentException("L'index de la bonne réponse doit désigner une option.");
        }
        enonce = enonce.strip();
        options = options.stream().map(String::strip).toList();
        if (options.stream().distinct().count() != options.size()) {
            throw new IllegalArgumentException("Les options d'une question doivent être distinctes.");
        }
    }
}
