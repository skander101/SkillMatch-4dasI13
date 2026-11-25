# api-gateway

Passerelle Spring Cloud Gateway de SkillMatch.

## Rôle

- Point d'entrée HTTP du frontend : `http://localhost:8080`.
- Route les appels d'authentification vers `commun-service` sur `http://localhost:8087`.
- Route les appels entreprises et avis vers `entreprises-service` sur `http://localhost:8083`.
- Autorise les requêtes CORS en développement ; à restreindre en production.

## Routes actuelles

| Route | Destination |
|---|---|
| `/skillmatch/auth/**` | `commun-service` |
| `/skillmatch/utilisateurs/**` | `commun-service` |
| `/skillmatch/entreprises/**` | `entreprises-service` |
| `/skillmatch/avis/**` | `entreprises-service` |

Les routes entreprises et avis sont disponibles via la gateway depuis le commit 4 du plan. Les endpoints protégés exigent un JWT ; la gateway ne valide pas les tokens elle-même, elle transmet l'en-tête `Authorization` aux services.

## Configuration

- Port : `8080`
- Nom Spring : `api-gateway`
- Fichier : `src/main/resources/application.yml`

Variables possibles :

- `SERVER_PORT`
- `COMMUN_SERVICE_URL`
- `ENTREPRISES_SERVICE_URL`
- `EUREKA_URL`, `EUREKA_REGISTER`, `EUREKA_FETCH`

## Démarrage

```bash
cd Backend/api-gateway
mvn spring-boot:run
```

Si le port `8080` est déjà occupé, l'arrêter ou lancer la passerelle avec :

```bash
SERVER_PORT=8090 mvn spring-boot:run
```

Puis adapter `Frontend/src/environments/environment.development.ts`.

La validation JWT est faite dans `commun-service` via le endpoint `/skillmatch/auth/me` et un intercepteur ADMIN sur `/skillmatch/utilisateurs/**`. `entreprises-service` appelle aussi `/skillmatch/auth/me` pour valider le JWT et appliquer ses propres règles de rôle. La gateway ne valide pas encore les tokens elle-même.

Le port `8080` doit être libre avant le démarrage ; tout autre serveur (par exemple Apache) doit être arrêté ou la passerelle lancée sur un autre port via `SERVER_PORT`.
