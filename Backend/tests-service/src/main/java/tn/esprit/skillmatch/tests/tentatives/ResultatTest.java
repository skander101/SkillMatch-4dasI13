package tn.esprit.skillmatch.tests.tentatives;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import tn.esprit.skillmatch.tests.catalogue.TestTechnique;

import java.time.Instant;

@Entity
@Table(
        name = "resultat_test",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resultat_test_candidat_tentative",
                columnNames = {"test_id", "candidat_id", "tentative"}
        ),
        indexes = @Index(name = "ix_resultat_test_candidat", columnList = "candidat_id")
)
public class ResultatTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resultat_test"))
    private TestTechnique test;

    @Column(name = "candidat_id", nullable = false, updatable = false)
    private Long candidatId;

    @Column(nullable = false, length = 120, updatable = false)
    private String competence;

    @Column(nullable = false, updatable = false)
    private int score;

    @Column(nullable = false, updatable = false)
    private boolean reussi;

    @Column(nullable = false, updatable = false)
    private int tentative;

    @Column(name = "date_passage", nullable = false, updatable = false)
    private Instant date;

    protected ResultatTest() {
    }

    ResultatTest(TestTechnique test, Long candidatId, int score, int tentative, Instant date) {
        this.test = test;
        this.candidatId = candidatId;
        this.competence = test.getCompetence();
        this.score = score;
        this.reussi = score >= test.getSeuilReussite();
        this.tentative = tentative;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public Long getTestId() {
        return test.getId();
    }

    public Long getCandidatId() {
        return candidatId;
    }

    public String getCompetence() {
        return competence;
    }

    public int getScore() {
        return score;
    }

    public boolean isReussi() {
        return reussi;
    }

    public int getTentative() {
        return tentative;
    }

    public Instant getDate() {
        return date;
    }
}
