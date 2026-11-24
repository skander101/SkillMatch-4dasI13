package tn.esprit.skillmatch.alertes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "alertes_emploi", indexes = {
        @Index(name = "idx_alerte_user", columnList = "user_id"),
        @Index(name = "idx_alerte_active", columnList = "active")
})
public class AlerteEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de l'utilisateur du commun-service : pas de clé étrangère entre bases. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Liste sans cycle de vie propre : colonne JSON. */
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "mots_cles", nullable = false, columnDefinition = "json")
    private List<String> motsCles = new ArrayList<>();

    @Column(length = 100)
    private String localisation;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContratType contrat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frequence frequence = Frequence.IMMEDIATE;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    public AlerteEmploi() {
    }

    public AlerteEmploi(Long userId, List<String> motsCles, String localisation,
                        ContratType contrat, Frequence frequence) {
        this.userId = userId;
        this.motsCles = new ArrayList<>(motsCles);
        this.localisation = localisation;
        this.contrat = contrat;
        this.frequence = frequence == null ? Frequence.IMMEDIATE : frequence;
    }

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    /**
     * Une offre publiée correspond à l'alerte si : l'alerte est active, le contrat est le même (s'il est précisé),
     * la localisation est la même (si elle est précisée) et au moins un mot-clé apparaît dans le titre
     * ou dans les compétences de l'offre.
     */
    public boolean correspondA(String titre, List<String> competences, String localisationOffre, ContratType contratOffre) {
        if (!active) {
            return false;
        }
        if (contrat != null && contrat != contratOffre) {
            return false;
        }
        if (localisation != null && !localisation.isBlank()
                && (localisationOffre == null || !localisationOffre.trim().equalsIgnoreCase(localisation.trim()))) {
            return false;
        }
        String titreMinuscule = titre == null ? "" : titre.toLowerCase(Locale.ROOT);
        List<String> competencesMinuscules = competences == null
                ? List.of()
                : competences.stream().map(c -> c.trim().toLowerCase(Locale.ROOT)).toList();
        return motsCles.stream()
                .map(mot -> mot.trim().toLowerCase(Locale.ROOT))
                .anyMatch(mot -> !mot.isEmpty() && (titreMinuscule.contains(mot) || competencesMinuscules.contains(mot)));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getMotsCles() {
        return motsCles;
    }

    public void setMotsCles(List<String> motsCles) {
        this.motsCles = new ArrayList<>(motsCles);
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

    public Frequence getFrequence() {
        return frequence;
    }

    public void setFrequence(Frequence frequence) {
        this.frequence = frequence;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
}
