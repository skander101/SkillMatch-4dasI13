package tn.esprit.skillmatch.tests.catalogue;

import java.util.List;

public record VueTest(
        Long id,
        String titre,
        String competence,
        int dureeMinutes,
        int seuilReussite,
        List<VueQuestion> questions) {

    static VueTest de(TestTechnique test) {
        return new VueTest(
                test.getId(),
                test.getTitre(),
                test.getCompetence(),
                test.getDureeMinutes(),
                test.getSeuilReussite(),
                test.getQuestions().stream().map(VueQuestion::de).toList());
    }

    public record VueQuestion(String id, String enonce, List<String> options) {
        static VueQuestion de(Question question) {
            return new VueQuestion(question.id(), question.enonce(), question.options());
        }
    }
}
