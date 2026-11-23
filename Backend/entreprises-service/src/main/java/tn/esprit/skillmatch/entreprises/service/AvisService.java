package tn.esprit.skillmatch.entreprises.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.entreprises.dto.AvisRequest;
import tn.esprit.skillmatch.entreprises.dto.AvisResponse;
import tn.esprit.skillmatch.entreprises.dto.ModerationRequest;
import tn.esprit.skillmatch.entreprises.entity.AvisEntreprise;
import tn.esprit.skillmatch.entreprises.entity.Entreprise;
import tn.esprit.skillmatch.entreprises.entity.StatutModeration;
import tn.esprit.skillmatch.entreprises.mapper.AvisMapper;
import tn.esprit.skillmatch.entreprises.repository.AvisEntrepriseRepository;
import tn.esprit.skillmatch.entreprises.repository.EntrepriseRepository;

import java.util.List;

@Service
@Transactional
public class AvisService {

    private static final String AVIS_DUPLIQUE = "Un avis a déjà été publié par cet utilisateur pour cette entreprise";
    private static final String AVIS_INTROUVABLE = "Avis introuvable";
    private static final String STATUT_MODERATION_INVALIDE = "Le statut EN_ATTENTE n'est pas valide pour la modération";

    private final AvisEntrepriseRepository avisEntrepriseRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final AvisMapper avisMapper;

    public AvisService(AvisEntrepriseRepository avisEntrepriseRepository,
                       EntrepriseRepository entrepriseRepository,
                       AvisMapper avisMapper) {
        this.avisEntrepriseRepository = avisEntrepriseRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.avisMapper = avisMapper;
    }

    @Transactional(readOnly = true)
    public List<AvisResponse> findByEntreprise(Long entrepriseId) {
        if (!entrepriseRepository.existsById(entrepriseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise introuvable");
        }
        return avisEntrepriseRepository.findByEntreprise_IdOrderByDateCreationDesc(entrepriseId)
                .stream()
                .map(avisMapper::toResponse)
                .toList();
    }

    public AvisResponse submit(Long entrepriseId, AvisRequest request) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise introuvable"));

        if (avisEntrepriseRepository.existsByEntreprise_IdAndUtilisateurId(entrepriseId, request.utilisateurId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, AVIS_DUPLIQUE);
        }

        try {
            return avisMapper.toResponse(avisEntrepriseRepository.saveAndFlush(avisMapper.toEntity(request, entreprise)));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, AVIS_DUPLIQUE);
        }
    }

    public AvisResponse moderate(Long avisId, ModerationRequest request) {
        AvisEntreprise avis = avisEntrepriseRepository.findById(avisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, AVIS_INTROUVABLE));

        if (request.statut() == null || request.statut() == StatutModeration.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, STATUT_MODERATION_INVALIDE);
        }

        avis.setStatut(request.statut());
        AvisEntreprise avisModere = avisEntrepriseRepository.saveAndFlush(avis);
        recalculerNoteMoyenne(avis.getEntreprise().getId());
        return avisMapper.toResponse(avisModere);
    }

    public void delete(Long avisId) {
        AvisEntreprise avis = avisEntrepriseRepository.findById(avisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, AVIS_INTROUVABLE));

        Long entrepriseId = avis.getEntreprise().getId();
        avisEntrepriseRepository.delete(avis);
        avisEntrepriseRepository.flush();
        recalculerNoteMoyenne(entrepriseId);
    }

    private void recalculerNoteMoyenne(Long entrepriseId) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise introuvable"));

        double moyenne = avisEntrepriseRepository.findByEntreprise_IdAndStatut(entrepriseId, StatutModeration.APPROUVE)
                .stream()
                .mapToInt(AvisEntreprise::getNote)
                .average()
                .orElse(0.0);

        entreprise.setNoteMoyenne(Math.round(moyenne * 100.0) / 100.0);
        entrepriseRepository.save(entreprise);
    }
}
