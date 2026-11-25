package tn.esprit.skillmatch.commun.controller;

import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.commun.dto.AuthResponse;
import tn.esprit.skillmatch.commun.dto.LoginRequest;
import tn.esprit.skillmatch.commun.dto.RegisterRequest;
import tn.esprit.skillmatch.commun.dto.UserResponse;
import tn.esprit.skillmatch.commun.security.JwtService;
import tn.esprit.skillmatch.commun.service.AuthService;

@RestController
@RequestMapping("/skillmatch/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }

        try {
            String token = authorization.substring(7);
            return authService.currentUser(jwtService.extractEmail(token));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT invalide ou expiré");
        }
    }
}
