package tn.esprit.skillmatch.entreprises.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.entreprises.dto.EntrepriseRequest;
import tn.esprit.skillmatch.entreprises.dto.EntrepriseResponse;
import tn.esprit.skillmatch.entreprises.entity.Entreprise;
import tn.esprit.skillmatch.entreprises.repository.EntrepriseRepository;

import java.util.List;

@Service
@Transactional
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;

    public EntrepriseService(EntrepriseRepository entrepriseRepository) {
        this.entrepriseRepository = entrepriseRepository;
    }

    @Transactional(readOnly = true)
    public List<EntrepriseResponse> findAll() {
        return entrepriseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EntrepriseResponse findById(Long id) {
        return entrepriseRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise introuvable"));
    }

    public EntrepriseResponse create(EntrepriseRequest request) {
        Entreprise entreprise = new Entreprise(request.nom(), request.secteur(), request.ville(), request.description());
        return toResponse(entrepriseRepository.save(entreprise));
    }

    public EntrepriseResponse update(Long id, EntrepriseRequest request) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise introuvable"));
        entreprise.setNom(request.nom());
        entreprise.setSecteur(request.secteur());
        entreprise.setVille(request.ville());
        entreprise.setDescription(request.description());
        return toResponse(entrepriseRepository.save(entreprise));
    }

    public void delete(Long id) {
        if (!entrepriseRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise introuvable");
        }
        entrepriseRepository.deleteById(id);
    }

    private EntrepriseResponse toResponse(Entreprise entreprise) {
        return new EntrepriseResponse(
                entreprise.getId(),
                entreprise.getNom(),
                entreprise.getSecteur(),
                entreprise.getVille(),
                entreprise.getDescription(),
                entreprise.getNoteMoyenne()
        );
    }
}
