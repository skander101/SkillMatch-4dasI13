package tn.esprit.skillmatch.tests.catalogue;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DefinitionTest(
        @NotBlank @Size(max = 150) String titre,
        @NotBlank @Size(max = 100) String competence,
        @NotNull @Min(1) @Max(240) Integer dureeMinutes,
        @NotNull @Min(1) @Max(100) Integer seuilReussite,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid DefinitionQuestion> questions) {

    List<Question> construireQuestions() {
        return questions.stream().map(DefinitionQuestion::construire).toList();
    }

    public record DefinitionQuestion(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]{0,39}") String id,
            @NotBlank @Size(max = 1000) String enonce,
            @NotNull @Size(min = 2, max = 8) List<@NotBlank @Size(max = 300) String> options,
            @NotNull @Min(0) @Max(7) Integer indexBonneReponse) {

        Question construire() {
            return new Question(id, enonce, options, indexBonneReponse);
        }
    }
}
