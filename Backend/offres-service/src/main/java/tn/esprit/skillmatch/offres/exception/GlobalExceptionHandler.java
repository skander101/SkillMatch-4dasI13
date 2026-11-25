
package tn.esprit.skillmatch.offres.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Collectors;


// ========== GESTION GLOBALE DES ERREURS ==========

// Permet de gérer les erreurs de tous les contrôleurs REST.
@RestControllerAdvice

// Classe qui centralise les erreurs du microservice.
public class GlobalExceptionHandler {


    // ========== 1. ERREURS MÉTIER ==========

    // Intercepte les erreurs métier comme un accès interdit
    // ou une offre introuvable.
    @ExceptionHandler(ResponseStatusException.class)

    // Prépare une réponse HTTP pour une erreur métier.
    public ResponseEntity<ApiError> metier(ResponseStatusException exception) {

        // Récupère le code HTTP de l'erreur.
        return ResponseEntity.status(exception.getStatusCode())

            // Retourne le code et le message d'erreur à Angular.
            .body(new ApiError(exception.getStatusCode().value(), exception.getReason()));
    }


    // ========== 2. ERREURS DE VALIDATION ==========

    // Intercepte les erreurs des champs vérifiés avec @Valid.
    @ExceptionHandler(MethodArgumentNotValidException.class)

    // Récupère les messages des champs invalides.
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {

        // Récupère et parcourt les erreurs de validation.
        String message = exception.getBindingResult().getFieldErrors().stream()

            // Ajoute le nom du champ et son message d'erreur.
            .map(erreur -> erreur.getField() + " : " + erreur.getDefaultMessage())

            // Regroupe tous les messages dans un seul texte.
            .collect(Collectors.joining(", "));

        // Retourne une erreur HTTP 400 avec les messages de validation.
        return ResponseEntity.badRequest().body(new ApiError(400, message));
    }


    // ========== 3. ERREURS DE FORMAT ==========

    // Intercepte les erreurs de JSON ou de paramètres invalides.
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})

    // Prépare une réponse pour les données au mauvais format.
    public ResponseEntity<ApiError> format(Exception exception) {

        // Retourne une erreur HTTP 400 avec un message simple.
        return ResponseEntity.badRequest().body(new ApiError(400, "JSON, statut, contrat ou identifiant invalide"));
    }


    // ========== 4. ERREURS D'INTÉGRITÉ MYSQL ==========

    // Intercepte les conflits liés aux contraintes de MySQL.
    // Exemple : une candidature déjà enregistrée.
    @ExceptionHandler(DataIntegrityViolationException.class)

    // Prépare une réponse en cas de conflit dans la base de données.
    public ResponseEntity<ApiError> integrite(DataIntegrityViolationException exception) {

        // Retourne une erreur HTTP 409 sans exposer les détails SQL.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(409, "Conflit d'intégrité : vérifier les données ou les candidatures associées"));
    }


    // ========== 5. FORMAT DES MESSAGES D'ERREUR ==========

    // Définit le format des erreurs envoyées au frontend Angular.
    // Contient le code HTTP et le message d'erreur.
    public record ApiError(int status, String message) {
    }

}
