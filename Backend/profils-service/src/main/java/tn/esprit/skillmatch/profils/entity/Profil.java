package tn.esprit.skillmatch.profils.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profils")
public class Profil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(length = 150)
    private String titre;

    @Column(length = 150)
    private String localisation;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<String> competences = new ArrayList<>();

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<String> experiences = new ArrayList<>();

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<String> badges = new ArrayList<>();

    public Profil() {
    }

    public Profil(Long userId, String titre, String localisation, List<String> competences,
                  List<String> experiences, List<String> badges) {
        this.userId = userId;
        this.titre = titre;
        this.localisation = localisation;
        this.competences = competences == null ? new ArrayList<>() : competences;
        this.experiences = experiences == null ? new ArrayList<>() : experiences;
        this.badges = badges == null ? new ArrayList<>() : badges;
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

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public List<String> getCompetences() {
        return competences;
    }

    public void setCompetences(List<String> competences) {
        this.competences = competences == null ? new ArrayList<>() : competences;
    }

    public List<String> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<String> experiences) {
        this.experiences = experiences == null ? new ArrayList<>() : experiences;
    }

    public List<String> getBadges() {
        return badges;
    }

    public void setBadges(List<String> badges) {
        this.badges = badges == null ? new ArrayList<>() : badges;
    }

    public void ajouterBadge(String badge) {
        if (badge != null && !badge.isBlank() && !badges.contains(badge)) {
            badges.add(badge);
        }
    }

    @JsonIgnore
    public int calculerCompletude() {
        int points = 0;
        if (titre != null && !titre.isBlank()) {
            points += 20;
        }
        if (localisation != null && !localisation.isBlank()) {
            points += 20;
        }
        if (!competences.isEmpty()) {
            points += 25;
        }
        if (!experiences.isEmpty()) {
            points += 20;
        }
        if (!badges.isEmpty()) {
            points += 15;
        }
        return points;
    }
}
