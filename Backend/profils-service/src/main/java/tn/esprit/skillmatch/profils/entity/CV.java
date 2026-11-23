package tn.esprit.skillmatch.profils.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "cvs")
public class CV {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profil_id", nullable = false)
    private Profil profil;

    @Column(name = "fichier_uri", nullable = false, length = 500)
    private String fichierUri;

    @Column(name = "date_upload", nullable = false)
    private LocalDateTime dateUpload;

    public CV() {
    }

    public CV(Profil profil, String fichierUri, LocalDateTime dateUpload) {
        this.profil = profil;
        this.fichierUri = fichierUri;
        this.dateUpload = dateUpload;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Profil getProfil() {
        return profil;
    }

    public void setProfil(Profil profil) {
        this.profil = profil;
    }

    public String getFichierUri() {
        return fichierUri;
    }

    public void setFichierUri(String fichierUri) {
        this.fichierUri = fichierUri;
    }

    public LocalDateTime getDateUpload() {
        return dateUpload;
    }

    public void setDateUpload(LocalDateTime dateUpload) {
        this.dateUpload = dateUpload;
    }
}
