# alertes-service

Microservice Spring Boot **Alertes & Favoris** de SkillMatch (responsable : Amir Khemissi).

## Responsabilités

- Table `alertes_emploi` : critères enregistrés par le candidat (mots-clés JSON, localisation, contrat, fréquence, active).
- Table `offres_sauvegardees` : offres mises en favori, `UNIQUE(user_id, offer_id)`.
- Consomme `OfferPublished` et publie `AlertMatched` pour chaque alerte active correspondante.

## Port et configuration

- Port : `8085`
- Base : `alertes_db`
- Nom Spring : `alertes-service`
- Configuration : `src/main/resources/application.yml`

Variables d'environnement possibles :

- `SERVER_PORT`
- `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`
- `JWT_SECRET` (le même que `commun-service`)
- `COMMUN_SERVICE_URL` (défaut `http://localhost:8087`)
- `OFFRES_SERVICE_URL` (défaut `http://localhost:8081`), `OFFRES_VERIFICATION_ENABLED` (défaut `false`)
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `MESSAGING_ENABLED` (défaut `false`)
- `ALERTES_MAX_ACTIVES` (défaut `5`)
- `EUREKA_URL`, `EUREKA_REGISTER`, `EUREKA_FETCH`

La base `alertes_db` est créée automatiquement si l'utilisateur MySQL a le droit `CREATE`, puis Hibernate crée les tables avec `ddl-auto: update`.

Les erreurs JSON ont la forme `{"status":422,"message":"Un candidat ne peut pas avoir plus de 5 alertes actives"}`.

## Sécurité

Comme `AdminInterceptor` dans `commun-service`, un intercepteur vérifie le JWT sur `/skillmatch/alerts/**` et `/skillmatch/favorites/**` :

- pas de jeton ou jeton invalide : `401` ;
- rôle différent de `CANDIDAT` : `403` ;
- l'identifiant de l'utilisateur est obtenu par `GET /skillmatch/auth/me` sur `commun-service` (le JWT actuel contient l'email et le rôle, pas l'id). Si le JWT contient plus tard un claim `userId`, il est utilisé directement.

## Endpoints

| Méthode | Route | Description | Codes |
|---|---|---|---|
| `POST` | `/skillmatch/alerts` | Créer une alerte | 201, 400, 401, 403, 409, 422 |
| `GET` | `/skillmatch/alerts/me` | Mes alertes | 200, 401 |
| `GET` | `/skillmatch/alerts/{id}` | Consulter une alerte | 200, 403, 404 |
| `PUT` | `/skillmatch/alerts/{id}` | Modifier les critères | 200, 400, 403, 404, 409 |
| `PUT` | `/skillmatch/alerts/{id}/active` | Activer / désactiver | 200, 403, 404, 422 |
| `DELETE` | `/skillmatch/alerts/{id}` | Supprimer | 204, 403, 404 |
| `POST` | `/skillmatch/favorites` | Sauvegarder une offre | 201, 400, 404, 409, 422, 503 |
| `GET` | `/skillmatch/favorites/me` | Mes favoris | 200, 401 |
| `DELETE` | `/skillmatch/favorites/{id}` | Retirer un favori | 204, 403, 404 |

## Règles métier

- 5 alertes actives maximum par candidat (`422`), y compris lors d'une réactivation.
- Alerte identique (mêmes mots-clés, localisation et contrat) refusée (`409`).
- Un favori par offre (`409`).
- Seule une offre `PUBLIEE` peut être sauvegardée : appel REST `GET /skillmatch/offers/{id}` vers `offres-service`
  (`404` offre inconnue, `422` offre non publiée, `503` service indisponible). Activé avec `OFFRES_VERIFICATION_ENABLED=true`.

## Événements RabbitMQ (`MESSAGING_ENABLED=true`)

| Sens | Événement | Exchange / clé | File |
|---|---|---|---|
| Consommé | `OfferPublished` | `skillmatch.events` / `offer.published` | `alertes.offer-published` |
| Publié | `AlertMatched` | `skillmatch.events` / `alert.matched` | — |

Format : `{"eventId","type","occurredAt","source","data"}`.

`OfferPublished.data` attendu : `{"offerId":10,"titre":"Développeur Java","competences":["Java","Spring Boot"],"localisation":"Tunis","contrat":"CDI"}`.

`AlertMatched.data` publié : `{"alerteId":1,"userId":3,"offerId":10,"titreOffre":"Développeur Java"}`.

## Démarrage

```bash
cd Backend/alertes-service
mvn spring-boot:run
```

Tests unitaires (sans base de données) :

```bash
mvn test
```

Les requêtes de test sont dans [`requests.http`](requests.http) (IntelliJ HTTP Client).
Le service doit être appelé par le frontend via l'API Gateway sur `http://localhost:8080`.
