package tn.esprit.skillmatch.entreprises.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entreprises")
public class Entreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String secteur;

    @Column(nullable = false)
    private String ville;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private double noteMoyenne;

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvisEntreprise> avis = new ArrayList<>();

    public Entreprise() {
    }

    public Entreprise(String nom, String secteur, String ville, String description) {
        this.nom = nom;
        this.secteur = secteur;
        this.ville = ville;
        this.description = description;
        this.noteMoyenne = 0.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getSecteur() {
        return secteur;
    }

    public void setSecteur(String secteur) {
        this.secteur = secteur;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getNoteMoyenne() {
        return noteMoyenne;
    }

    public void setNoteMoyenne(double noteMoyenne) {
        this.noteMoyenne = noteMoyenne;
    }

    public List<AvisEntreprise> getAvis() {
        return avis;
    }

    public void setAvis(List<AvisEntreprise> avis) {
        this.avis = avis;
    }
}
