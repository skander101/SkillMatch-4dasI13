package tn.esprit.skillmatch.offres.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import tn.esprit.skillmatch.offres.enums.ContratType;
import tn.esprit.skillmatch.offres.enums.OffreStatut;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "offres")
public class Offre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Convert(converter = CompetencesConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<@NotBlank String> competences = new ArrayList<>();

    @Size(max = 255)
    private String localisation;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContratType contrat;

    @PositiveOrZero
    private Double salaire;

    @NotNull
    @Column(nullable = false)
    private LocalDate dateExpiration;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OffreStatut statut = OffreStatut.BROUILLON;

    @Positive
    private Long entrepriseId;

    @Positive
    private Long testRequisId;

    // Le proprietaire est un utilisateur, pas une entreprise.
    @NotNull
    @Positive
    @Column(nullable = false)
    private Long recruteurId;

    public Boolean estExpiree() {
        return dateExpiration != null && dateExpiration.isBefore(LocalDate.now());
    }

    // Pourcentage de competences requises presentes, sans recommandations.
    public Double calculerScore(List<String> competencesProfil) {
        if (competences == null || competences.isEmpty() || competencesProfil == null) {
            return 0.0;
        }
        Set<String> requises = normaliser(competences);
        if (requises.isEmpty()) {
            return 0.0;
        }
        Set<String> profil = normaliser(competencesProfil);
        long communes = requises.stream().filter(profil::contains).count();
        return 100.0 * communes / requises.size();
    }

    private Set<String> normaliser(List<String> valeurs) {
        return valeurs.stream()
                .filter(valeur -> valeur != null && !valeur.isBlank())
                .map(valeur -> valeur.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getCompetences() {
        return competences;
    }

    public void setCompetences(List<String> competences) {
        this.competences = competences;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public ContratType getContrat() {
        return contrat;
    }

    public void setContrat(ContratType contrat) {
        this.contrat = contrat;
    }

    public Double getSalaire() {
        return salaire;
    }

    public void setSalaire(Double salaire) {
        this.salaire = salaire;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public OffreStatut getStatut() {
        return statut;
    }

    public void setStatut(OffreStatut statut) {
        this.statut = statut;
    }

    public Long getEntrepriseId() {
        return entrepriseId;
    }

    public void setEntrepriseId(Long entrepriseId) {
        this.entrepriseId = entrepriseId;
    }

    public Long getTestRequisId() {
        return testRequisId;
    }

    public void setTestRequisId(Long testRequisId) {
        this.testRequisId = testRequisId;
    }

    public Long getRecruteurId() {
        return recruteurId;
    }

    public void setRecruteurId(Long recruteurId) {
        this.recruteurId = recruteurId;
    }
}
