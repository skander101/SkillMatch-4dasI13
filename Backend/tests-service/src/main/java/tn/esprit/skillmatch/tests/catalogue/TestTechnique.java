package tn.esprit.skillmatch.tests.catalogue;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "test_technique")
public class TestTechnique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, length = 100)
    private String competence;

    @Column(nullable = false)
    private int dureeMinutes;

    @Column(nullable = false)
    private int seuilReussite;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Question> questions;

    protected TestTechnique() {
    }

    public TestTechnique(String titre, String competence, int dureeMinutes, int seuilReussite,
            List<Question> questions) {
        modifier(titre, competence, dureeMinutes, seuilReussite, questions);
    }

    public void modifier(String titre, String competence, int dureeMinutes, int seuilReussite,
            List<Question> questions) {
        if (titre == null || titre.isBlank() || titre.length() > 150
                || competence == null || competence.isBlank() || competence.length() > 100) {
            throw new IllegalArgumentException("Le titre et la compétence sont obligatoires.");
        }
        if (dureeMinutes < 1 || dureeMinutes > 240 || seuilReussite < 1 || seuilReussite > 100) {
            throw new IllegalArgumentException("La durée ou le seuil de réussite est hors limites.");
        }
        if (questions == null || questions.isEmpty() || questions.size() > 100
                || questions.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Un test doit comporter entre une et cent questions valides.");
        }
        if (questions.stream().map(Question::id).distinct().count() != questions.size()) {
            throw new IllegalArgumentException("Les identifiants des questions doivent être distincts.");
        }
        this.titre = titre.strip();
        this.competence = competence.strip();
        this.dureeMinutes = dureeMinutes;
        this.seuilReussite = seuilReussite;
        this.questions = List.copyOf(questions);
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getCompetence() {
        return competence;
    }

    public int getDureeMinutes() {
        return dureeMinutes;
    }

    public int getSeuilReussite() {
        return seuilReussite;
    }

    public List<Question> getQuestions() {
        return List.copyOf(questions);
    }
}
