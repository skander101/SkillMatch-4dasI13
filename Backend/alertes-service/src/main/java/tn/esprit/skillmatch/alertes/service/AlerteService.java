package tn.esprit.skillmatch.alertes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.alertes.dto.AlerteRequest;
import tn.esprit.skillmatch.alertes.dto.AlerteResponse;
import tn.esprit.skillmatch.alertes.entity.AlerteEmploi;
import tn.esprit.skillmatch.alertes.entity.ContratType;
import tn.esprit.skillmatch.alertes.repository.AlerteEmploiRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class AlerteService {

    private final AlerteEmploiRepository alerteRepository;
    private final int maxActives;

    public AlerteService(AlerteEmploiRepository alerteRepository,
                         @Value("${alertes.max-actives:5}") int maxActives) {
        this.alerteRepository = alerteRepository;
        this.maxActives = maxActives;
    }

    /** POST /skillmatch/alerts : 201, 400, 409 (alerte identique), 422 (5 alertes actives maximum). */
    public AlerteResponse create(Long userId, AlerteRequest request) {
        AlerteEmploi alerte = new AlerteEmploi(userId, normaliser(request.motsCles()),
                nettoyer(request.localisation()), request.contrat(), request.frequence());
        verifierPasDeDoublon(userId, alerte, null);
        verifierQuotaActives(userId);
        return toResponse(alerteRepository.save(alerte));
    }

    /** GET /skillmatch/alerts/me */
    @Transactional(readOnly = true)
    public List<AlerteResponse> findMine(Long userId) {
        return alerteRepository.findByUserIdOrderByDateCreationDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** GET /skillmatch/alerts/{id} : 200, 403, 404 */
    @Transactional(readOnly = true)
    public AlerteResponse findById(Long userId, Long id) {
        return toResponse(getAlerteDuCandidat(userId, id));
    }

    /** PUT /skillmatch/alerts/{id} : 200, 400, 403, 404, 409 */
    public AlerteResponse update(Long userId, Long id, AlerteRequest request) {
        AlerteEmploi alerte = getAlerteDuCandidat(userId, id);
        AlerteEmploi criteres = new AlerteEmploi(userId, normaliser(request.motsCles()),
                nettoyer(request.localisation()), request.contrat(), request.frequence());
        verifierPasDeDoublon(userId, criteres, id);

        alerte.setMotsCles(criteres.getMotsCles());
        alerte.setLocalisation(criteres.getLocalisation());
        alerte.setContrat(criteres.getContrat());
        if (request.frequence() != null) {
            alerte.setFrequence(request.frequence());
        }
        return toResponse(alerteRepository.save(alerte));
    }

    /** PUT /skillmatch/alerts/{id}/active : 200, 403, 404, 422 (réactivation au-delà de 5) */
    public AlerteResponse changeActive(Long userId, Long id, boolean active) {
        AlerteEmploi alerte = getAlerteDuCandidat(userId, id);
        if (active && !alerte.isActive()) {
            verifierQuotaActives(userId);
        }
        alerte.setActive(active);
        return toResponse(alerteRepository.save(alerte));
    }

    /** DELETE /skillmatch/alerts/{id} : 204, 403, 404 */
    public void delete(Long userId, Long id) {
        alerteRepository.delete(getAlerteDuCandidat(userId, id));
    }

    /** Utilisé par le consommateur RabbitMQ OfferPublished. */
    @Transactional(readOnly = true)
    public List<AlerteEmploi> findCorrespondantes(String titre, List<String> competences,
                                                  String localisation, ContratType contrat) {
        return alerteRepository.findByActiveTrue()
                .stream()
                .filter(alerte -> alerte.correspondA(titre, competences, localisation, contrat))
                .toList();
    }

    private void verifierQuotaActives(Long userId) {
        if (alerteRepository.countByUserIdAndActiveTrue(userId) >= maxActives) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Un candidat ne peut pas avoir plus de " + maxActives + " alertes actives");
        }
    }

    private void verifierPasDeDoublon(Long userId, AlerteEmploi criteres, Long idExclu) {
        boolean doublon = alerteRepository.findByUserId(userId)
                .stream()
                .filter(autre -> idExclu == null || !autre.getId().equals(idExclu))
                .anyMatch(autre -> new HashSet<>(autre.getMotsCles()).equals(new HashSet<>(criteres.getMotsCles()))
                        && Objects.equals(minuscule(autre.getLocalisation()), minuscule(criteres.getLocalisation()))
                        && autre.getContrat() == criteres.getContrat());
        if (doublon) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une alerte identique existe déjà");
        }
    }

    /** 404 si l'alerte n'existe pas, 403 si elle appartient à un autre candidat. */
    private AlerteEmploi getAlerteDuCandidat(Long userId, Long id) {
        AlerteEmploi alerte = alerteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte introuvable"));
        if (!alerte.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette alerte appartient à un autre utilisateur");
        }
        return alerte;
    }

    private static List<String> normaliser(List<String> motsCles) {
        return motsCles.stream()
                .map(mot -> mot.trim().toLowerCase(Locale.ROOT))
                .filter(mot -> !mot.isEmpty())
                .distinct()
                .toList();
    }

    private static String nettoyer(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private static String minuscule(String valeur) {
        return valeur == null ? null : valeur.toLowerCase(Locale.ROOT);
    }

    private AlerteResponse toResponse(AlerteEmploi alerte) {
        return new AlerteResponse(
                alerte.getId(),
                alerte.getUserId(),
                alerte.getMotsCles(),
                alerte.getLocalisation(),
                alerte.getContrat(),
                alerte.getFrequence(),
                alerte.isActive(),
                alerte.getDateCreation()
        );
    }
}
