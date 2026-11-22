# api-gateway

Passerelle Spring Cloud Gateway de SkillMatch.

## Rôle

- Point d'entrée HTTP du frontend : `http://localhost:8080`.
- Route les appels d'authentification vers `commun-service` sur `http://localhost:8087`.
- Autorise les requêtes CORS en développement ; à restreindre en production.

## Routes actuelles

| Route | Destination |
|---|---|
| `/skillmatch/auth/**` | `commun-service` |
| `/skillmatch/utilisateurs/**` | `commun-service` |
| `/skillmatch/offres/**` | `lb://offres-service` via Eureka |
| `/skillmatch/candidatures/**` | `lb://offres-service` via Eureka |

## Configuration

- Port : `8080`
- Nom Spring : `api-gateway`
- Fichier : `src/main/resources/application.yml`

Variables possibles :

- `SERVER_PORT`
- `COMMUN_SERVICE_URL`
- `OFFRES_SERVICE_URL` (par défaut `lb://offres-service`, découvert via Eureka)
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

Pour l'authentification et les utilisateurs, la validation JWT est faite dans
`commun-service` via `/skillmatch/auth/me` et un intercepteur ADMIN sur
`/skillmatch/utilisateurs/**`. La Gateway ne valide pas les tokens elle-même.

Pour les offres et candidatures, `offres-service` vérifie localement le JWT,
puis résout l'identité via le service commun. La Gateway transmet le chemin,
les paramètres, le corps et l'en-tête Authorization sans les modifier.
Définir le même `JWT_SECRET` dans les deux services métier ; la Gateway
n'a pas besoin de cette clé. Démarrer MySQL, discovery-service, commun-service, offres-service,
puis la Gateway pour utiliser les CRUD via `http://localhost:8080/skillmatch`.

Eureka découvre aussi le port temporaire 18081 d'Offres : aucune modification
de route n'est nécessaire. Ne pas définir `OFFRES_SERVICE_URL` pour utiliser
la découverte ; cette variable permet encore de forcer une adresse directe.
Les ports officiels restent 8080 et 8081. La Gateway n'utilise pas RabbitMQ ;
Offres publie ses événements selon [ce guide](../offres-service/RABBITMQ.md).
Config Server reste inactif.
Voir [le guide Eureka](../discovery-service/README.md).

Test : `mvn test`. `GatewayRoutesTest` démarre deux petits serveurs simulés
pour vérifier les deux nouvelles routes et les routes communes existantes,
ainsi que les méthodes HTTP, paramètres, corps, codes et en-têtes transmis.

Vérification : `mvn package` réussi, 8 tests passés. Avec les deux vrais JAR,
les routes offres et candidatures ont renvoyé le même 401 sans JWT en direct
sur 18081 et via la Gateway sur 8080. Les instances de vérification ont été arrêtées.

Le port `8080` doit être libre avant le démarrage ; tout autre serveur (par exemple Apache) doit être arrêté ou la passerelle lancée sur un autre port via `SERVER_PORT`.
