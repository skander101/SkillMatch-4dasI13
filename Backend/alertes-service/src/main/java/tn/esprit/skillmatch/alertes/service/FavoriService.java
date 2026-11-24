package tn.esprit.skillmatch.alertes.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.alertes.client.OffresClient;
import tn.esprit.skillmatch.alertes.dto.FavoriResponse;
import tn.esprit.skillmatch.alertes.entity.OffreSauvegardee;
import tn.esprit.skillmatch.alertes.repository.OffreSauvegardeeRepository;

import java.util.List;

@Service
@Transactional
public class FavoriService {

    private final OffreSauvegardeeRepository favoriRepository;
    private final OffresClient offresClient;

    public FavoriService(OffreSauvegardeeRepository favoriRepository, OffresClient offresClient) {
        this.favoriRepository = favoriRepository;
        this.offresClient = offresClient;
    }

    /** POST /skillmatch/favorites : 201, 404 (offre inconnue), 409 (déjà en favori), 422 (offre non publiée) */
    public FavoriResponse create(Long userId, Long offerId) {
        if (favoriRepository.existsByUserIdAndOfferId(userId, offerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette offre est déjà dans vos favoris");
        }
        offresClient.verifierOffrePubliee(offerId);
        return toResponse(favoriRepository.save(new OffreSauvegardee(userId, offerId)));
    }

    /** GET /skillmatch/favorites/me */
    @Transactional(readOnly = true)
    public List<FavoriResponse> findMine(Long userId) {
        return favoriRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** DELETE /skillmatch/favorites/{id} : 204, 403, 404 */
    public void delete(Long userId, Long id) {
        OffreSauvegardee favori = favoriRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Favori introuvable"));
        if (!favori.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce favori appartient à un autre utilisateur");
        }
        favoriRepository.delete(favori);
    }

    private FavoriResponse toResponse(OffreSauvegardee favori) {
        return new FavoriResponse(favori.getId(), favori.getUserId(), favori.getOfferId(), favori.getDate());
    }
}
