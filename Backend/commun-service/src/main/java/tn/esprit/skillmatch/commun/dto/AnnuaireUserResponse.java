package tn.esprit.skillmatch.commun.dto;

import tn.esprit.skillmatch.commun.entity.Role;

/**
 * Projection publique de l'annuaire.
 * Contient uniquement l'id, le nom et le rôle :
 * l'email, le mot de passe et toute autre donnée ne sont jamais exposés.
 */
public record AnnuaireUserResponse(
        Long id,
        String nom,
        Role role
) {
}
