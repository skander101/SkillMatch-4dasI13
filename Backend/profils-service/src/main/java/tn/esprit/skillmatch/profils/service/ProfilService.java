package tn.esprit.skillmatch.profils.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.profils.dto.CVResponse;
import tn.esprit.skillmatch.profils.dto.CompletenessResponse;
import tn.esprit.skillmatch.profils.dto.ProfilRequest;
import tn.esprit.skillmatch.profils.dto.ProfilResponse;
import tn.esprit.skillmatch.profils.dto.ProfilUpdate;
import tn.esprit.skillmatch.profils.entity.CV;
import tn.esprit.skillmatch.profils.entity.Profil;
import tn.esprit.skillmatch.profils.repository.CVRepository;
import tn.esprit.skillmatch.profils.repository.ProfilRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ProfilService {

    private final ProfilRepository profilRepository;
    private final CVRepository cvRepository;
    private final Path cvDirectory;
    private final long maxFileSize;

    public ProfilService(
            ProfilRepository profilRepository,
            CVRepository cvRepository,
            @Value("${profils.cv.directory:${java.io.tmpdir}/skillmatch/cv}") String cvDirectory,
            @Value("${profils.cv.max-size-bytes:5242880}") long maxFileSize
    ) throws IOException {
        this.profilRepository = profilRepository;
        this.cvRepository = cvRepository;
        this.cvDirectory = Path.of(cvDirectory);
        this.maxFileSize = maxFileSize;
        Files.createDirectories(this.cvDirectory);
    }

    public ProfilResponse creer(ProfilRequest request) {
        if (profilRepository.existsByUserId(request.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un profil existe déjà pour cet utilisateur");
        }
        Profil profil = profilRepository.save(new Profil(
                request.userId(), request.titre(), request.localisation(),
                request.competences(), request.experiences(), request.badges()
        ));
        return toResponse(profil);
    }

    @Transactional(readOnly = true)
    public ProfilResponse findByUserId(Long userId) {
        return toResponse(exigerProfil(userId));
    }

    @Transactional(readOnly = true)
    public Profil findEntityByUserId(Long userId) {
        return exigerProfil(userId);
    }

    public ProfilResponse modifier(Long userId, ProfilUpdate update) {
        Profil profil = exigerProfil(userId);
        if (update.titre() != null) {
            profil.setTitre(update.titre());
        }
        if (update.localisation() != null) {
            profil.setLocalisation(update.localisation());
        }
        if (update.competences() != null) {
            profil.setCompetences(update.competences());
        }
        if (update.experiences() != null) {
            profil.setExperiences(update.experiences());
        }
        if (update.badges() != null) {
            profil.setBadges(update.badges());
        }
        return toResponse(profil);
    }

    @Transactional(readOnly = true)
    public CompletenessResponse completude(Long userId) {
        Profil profil = exigerProfil(userId);
        int value = profil.calculerCompletude();
        return new CompletenessResponse(userId, value, value == 100);
    }

    public void ajouterBadge(Long userId, String badge) {
        Profil profil = exigerProfil(userId);
        profil.ajouterBadge(badge);
    }

    public CVResponse uploadCV(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier CV est vide");
        }
        if (file.getSize() > maxFileSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Le fichier CV dépasse la taille maximale autorisée");
        }
        String original = file.getOriginalFilename() == null ? "cv.pdf" : file.getOriginalFilename();
        String extension = original.toLowerCase().endsWith(".pdf") ? ".pdf" : "";
        if (extension.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le CV doit être un fichier PDF");
        }
        Profil profil = exigerProfil(userId);
        String filename = "cv-" + userId + "-" + System.currentTimeMillis() + extension;
        Path destination = cvDirectory.resolve(filename);
        try (var input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible d'enregistrer le CV", exception);
        }
        CV cv = cvRepository.save(new CV(profil, destination.toString(), LocalDateTime.now()));
        return toResponse(cv);
    }

    @Transactional(readOnly = true)
    public CVResponse findCV(Long userId, Long cvId) {
        Profil profil = exigerProfil(userId);
        CV cv = cvRepository.findById(cvId)
                .filter(value -> value.getProfil().getId().equals(profil.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV introuvable"));
        return toResponse(cv);
    }

    @Transactional(readOnly = true)
    public List<CVResponse> findCVs(Long userId) {
        Profil profil = exigerProfil(userId);
        return cvRepository.findByProfilIdOrderByDateUploadDesc(profil.getId())
                .stream().map(this::toResponse).toList();
    }

    public void supprimerCV(Long userId, Long cvId) {
        Profil profil = exigerProfil(userId);
        CV cv = cvRepository.findById(cvId)
                .filter(value -> value.getProfil().getId().equals(profil.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV introuvable"));
        try {
            Files.deleteIfExists(Path.of(cv.getFichierUri()));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de supprimer le fichier CV", exception);
        }
        cvRepository.delete(cv);
    }

    private Profil exigerProfil(Long userId) {
        return profilRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable"));
    }

    private ProfilResponse toResponse(Profil profil) {
        return new ProfilResponse(
                profil.getId(), profil.getUserId(), profil.getTitre(), profil.getLocalisation(),
                profil.getCompetences(), profil.getExperiences(), profil.getBadges(),
                profil.calculerCompletude()
        );
    }

    private CVResponse toResponse(CV cv) {
        return new CVResponse(cv.getId(), cv.getProfil().getId(), cv.getFichierUri(), cv.getDateUpload());
    }
}
