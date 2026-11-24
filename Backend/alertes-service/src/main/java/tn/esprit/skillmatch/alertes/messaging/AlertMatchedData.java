package tn.esprit.skillmatch.alertes.messaging;

/** Contenu de AlertMatched, consommé par commun-service pour notifier le candidat. */
public record AlertMatchedData(
        Long alerteId,
        Long userId,
        Long offerId,
        String titreOffre
) {
}
