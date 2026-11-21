package tn.esprit.skillmatch.commun.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.commun.dto.CreateUtilisateurRequest;
import tn.esprit.skillmatch.commun.dto.UpdateUtilisateurRequest;
import tn.esprit.skillmatch.commun.dto.UserResponse;
import tn.esprit.skillmatch.commun.entity.Utilisateur;
import tn.esprit.skillmatch.commun.repository.UtilisateurRepository;

import java.util.List;

@Service
@Transactional
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return utilisateurRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return utilisateurRepository.findById(id)
                .map(this::toUserResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    public UserResponse create(CreateUtilisateurRequest request) {
        if (utilisateurRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }
        Utilisateur utilisateur = new Utilisateur(
                request.nom(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        return toUserResponse(utilisateurRepository.save(utilisateur));
    }

    public UserResponse update(Long id, UpdateUtilisateurRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        utilisateurRepository.findByEmail(request.email())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
                });

        utilisateur.setNom(request.nom());
        utilisateur.setEmail(request.email());
        utilisateur.setRole(request.role());
        if (request.password() != null && !request.password().isBlank()) {
            utilisateur.setPassword(passwordEncoder.encode(request.password()));
        }
        return toUserResponse(utilisateurRepository.save(utilisateur));
    }

    public void delete(Long id) {
        if (!utilisateurRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
        }
        utilisateurRepository.deleteById(id);
    }

    private UserResponse toUserResponse(Utilisateur utilisateur) {
        return new UserResponse(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getEmail(),
                utilisateur.getRole()
        );
    }
}
