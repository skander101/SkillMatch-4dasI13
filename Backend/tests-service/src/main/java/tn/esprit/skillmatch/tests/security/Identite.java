package tn.esprit.skillmatch.tests.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

public final class Identite {

    private Identite() {
    }

    public static Long candidatId(OAuth2AuthenticatedPrincipal principal) {
        return principal.getAttribute("userId");
    }
}
