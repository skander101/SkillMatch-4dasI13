package tn.esprit.skillmatch.alertes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "offres_sauvegardees",
        uniqueConstraints = @UniqueConstraint(name = "uk_favori_user_offer", columnNames = {"user_id", "offer_id"}))
public class OffreSauvegardee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de l'utilisateur (commun-service). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Identifiant de l'offre (offres-service). */
    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "date_sauvegarde", nullable = false)
    private LocalDateTime date;

    public OffreSauvegardee() {
    }

    public OffreSauvegardee(Long userId, Long offerId) {
        this.userId = userId;
        this.offerId = offerId;
    }

    @PrePersist
    void onCreate() {
        if (date == null) {
            date = LocalDateTime.now();
        }
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

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
