package tn.esprit.skillmatch.offres.exception; // Réponses d'erreur cohérentes.

import org.springframework.dao.DataIntegrityViolationException; // Conflit d'intégrité en base.
import org.springframework.http.HttpStatus; // Code 409.
import org.springframework.http.ResponseEntity; // Réponse HTTP avec statut.
import org.springframework.http.converter.HttpMessageNotReadableException; // JSON ou enum invalide.
import org.springframework.web.bind.MethodArgumentNotValidException; // Formulaire invalide.
import org.springframework.web.bind.annotation.ExceptionHandler; // Associe une erreur à une méthode.
import org.springframework.web.bind.annotation.RestControllerAdvice; // Gestion commune aux contrôleurs.
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException; // Identifiant non numérique.
import org.springframework.web.server.ResponseStatusException; // Erreur métier avec code HTTP.
import java.util.stream.Collectors; // Regroupe les messages de validation.

@RestControllerAdvice // Applique ces réponses à l'API du service.
public class GlobalExceptionHandler { // Ne retourne jamais la trace technique au client.
    @ExceptionHandler(ResponseStatusException.class) // Erreurs métier et d'authentification.
    public ResponseEntity<ApiError> metier(ResponseStatusException exception) { // Conserve le code choisi par le service.
        return ResponseEntity.status(exception.getStatusCode()) // Par exemple 401, 403, 404 ou 409.
                .body(new ApiError(exception.getStatusCode().value(), exception.getReason())); // Message lisible par Angular.
    } // Fin des erreurs métier.

    @ExceptionHandler(MethodArgumentNotValidException.class) // Erreurs des annotations @Valid.
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) { // Liste les champs invalides.
        String message = exception.getBindingResult().getFieldErrors().stream() // Parcourt les erreurs du formulaire.
                .map(erreur -> erreur.getField() + " : " + erreur.getDefaultMessage()) // Ajoute le nom du champ.
                .collect(Collectors.joining(", ")); // Construit un seul texte.
        return ResponseEntity.badRequest().body(new ApiError(400, message)); // Retourne 400.
    } // Fin des validations.

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class}) // JSON, enum ou identifiant invalide.
    public ResponseEntity<ApiError> format(Exception exception) { // Masque les détails internes du parseur.
        return ResponseEntity.badRequest().body(new ApiError(400, "JSON, statut, contrat ou identifiant invalide")); // Retourne 400.
    } // Fin des erreurs de format.

    @ExceptionHandler(DataIntegrityViolationException.class) // Protège aussi le cas d'une candidature insérée simultanément.
    public ResponseEntity<ApiError> integrite(DataIntegrityViolationException exception) { // N'expose pas les messages SQL.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(409, "Conflit d'intégrité : vérifier les données ou les candidatures associées")); // Retourne 409.
    } // Fin des conflits.

    public record ApiError(int status, String message) { // Même format simple que le service commun.
    } // Fin du record.
} // Fin du gestionnaire.
