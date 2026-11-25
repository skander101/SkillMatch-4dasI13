package tn.esprit.skillmatch.commun.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.commun.dto.AuthResponse;
import tn.esprit.skillmatch.commun.dto.LoginRequest;
import tn.esprit.skillmatch.commun.dto.RegisterRequest;
import tn.esprit.skillmatch.commun.dto.UserResponse;
import tn.esprit.skillmatch.commun.entity.Utilisateur;
import tn.esprit.skillmatch.commun.entity.Role;
import tn.esprit.skillmatch.commun.repository.UtilisateurRepository;
import tn.esprit.skillmatch.commun.security.JwtService;

@Service
@Transactional
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La création d'un administrateur n'est pas autorisée depuis l'inscription publique");
        }
        if (utilisateurRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }

        Utilisateur utilisateur = new Utilisateur(
                request.nom(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        return toAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));

        if (!passwordEncoder.matches(request.password(), utilisateur.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }

        return toAuthResponse(utilisateur);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return utilisateurRepository.findByEmail(email)
                .map(this::toUserResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private AuthResponse toAuthResponse(Utilisateur utilisateur) {
        return new AuthResponse(jwtService.generateToken(utilisateur), toUserResponse(utilisateur));
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
