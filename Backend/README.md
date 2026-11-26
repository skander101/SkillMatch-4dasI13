# Backend

Ce dossier contient tous les microservices Spring Boot du projet.

Chaque microservice est un projet Maven indépendant. Il n'y a pas de `pom.xml` parent : cela évite les conflits entre les branches des six membres et permet de démarrer, compiler et déployer chaque service séparément.

## Conventions

- Package racine : `tn.esprit.skillmatch.<service>`
- Java 17 et Spring Boot 3.3.x
- Spring Data JPA avec MySQL
- Une base de données MySQL distincte par service, sous XAMPP
- Aucune clé étrangère, jointure ou requête entre deux bases
- Les services s'échangent uniquement des identifiants
- Les listes sans champs propres sont stockées en colonne JSON ou TEXT
- Les appels synchrones utilisent REST
- Les événements asynchrones utilisent RabbitMQ
- La documentation des API est exposée avec springdoc-openapi et Swagger UI
- Les ports et les noms de bases sont fixés dans le tableau ci-dessous

## Contrats des microservices

| Microservice | Port | Base | Entités | Appels synchrones | Événements publiés | Événements consommés |
|---|---:|---|---|---|---|---|
| `offres-service` | 8081 | `offres_db` | `Offre`, `Candidature` | Profils, Tests | `OfferPublished`, `StatusChanged`, `ApplicationSubmitted` | — |
| `profils-service` | 8082 | `profils_db` | `Profil`, `CV` | — | `ProfileUpdated` | `TestPassed` |
| `entreprises-service` | 8083 | `entreprises_db` | `Entreprise`, `AvisEntreprise` | Utilisateurs (`/skillmatch/auth/me`) | `AvisModere` | `ProfileUpdated` |
| `tests-service` | 8084 | `tests_db` | `TestTechnique`, `ResultatTest` | — | `TestPassed` | — |
| `alertes-service` | 8085 | `alertes_db` | `AlerteEmploi`, `OffreSauvegardee` | — | `AlertMatched` | `OfferPublished` |
| `messagerie-service` | 8086 | `messagerie_db` | `Conversation`, `Message` | — | `MessageSent` | — |
| `commun-service` | 8087 | `commun_db` | `Utilisateur` | Auth JWT | — | — |
| `api-gateway` | 8080 | — | — | Routes auth/utilisateurs vers `commun-service` et entreprises/avis vers `entreprises-service` | — | — |
| `discovery-service` | 8761 | — | — | — | — | — |
| `config-service` | 8888 | — | — | — | — | — |

> Le tableau décrit le contrat cible. État courant sur `skander_entreprise+avis` : `entreprises-service` implémente le CRUD `Entreprise`, la publication d'avis avec unicité, la modération avec recalcul de moyenne, la sécurité JWT par rôle et les événements RabbitMQ `AvisModere` / `ProfileUpdated`.

## Détail par microservice

### Offres & Candidatures

- Entités : `Offre`, `Candidature`
- Le matching n'est pas un microservice séparé. `GET /offers/recommended` calcule un score pondéré dans le code à partir du profil récupéré auprès de `profils-service`.
- L'entretien est représenté par un statut et une date sur `Candidature`.
- Statuts de candidature : `RECUE`, `PRESELECTIONNEE`, `ENTRETIEN_PLANIFIE`, `ACCEPTEE`, `REFUSEE`, `RETIREE`.
- Appels synchrones : `profils-service` et `tests-service`.

### Profils & CV

- Entités : `Profil`, `CV`
- Les compétences, expériences et badges sont stockés en colonnes JSON.
- Le pourcentage de complétion du profil est calculé par le service.
- Consomme `TestPassed` pour ajouter un badge au profil.

### Entreprises & Avis

Implémentation actuelle (commits 1 à 5 sur `skander_entreprise+avis`) :

- Entités implémentées : `Entreprise` (`id`, `nom`, `secteur`, `ville`, `description`, `noteMoyenne`) et `AvisEntreprise` (`id`, `entreprise`, `utilisateurId`, `nomUtilisateur`, `note`, `commentaire`, `statut`, `dateCreation`).
- CRUD disponible via la gateway sur `http://localhost:8080/skillmatch/entreprises` : `POST`, `GET`, `GET /{id}`, `PUT /{id}` et `DELETE /{id}`.
- Avis disponibles sur `POST /skillmatch/entreprises/{id}/avis` et `GET /skillmatch/entreprises/{id}/avis`.
- Note comprise entre 1 et 5, statut initial `EN_ATTENTE`, unicité par paire entreprise/utilisateur et réponse `409` sur doublon.
- Modération disponible sur `PUT /skillmatch/avis/{id}/moderation` avec `APPROUVE` ou `REJETE`, et suppression sur `DELETE /skillmatch/avis/{id}`.
- `noteMoyenne` est recalculée à partir des seuls avis approuvés après chaque modération ou suppression, arrondie à deux décimales.
- Les `GET /skillmatch/entreprises/**` sont publics ; la création/modification/suppression d'entreprise et la modération/suppression d'avis exigent `ADMIN` ; la publication d'avis exige `CANDIDAT` ou `RECRUTEUR`.
- Le JWT est validé par `entreprises-service` via `GET /skillmatch/auth/me` sur `commun-service` : `401` sans token valide, `403` avec un rôle incorrect, `503` si `commun-service` est indisponible.
- Un `AvisModere` est publié après changement effectif de statut lors de la modération, sur l'exchange topic `skillmatch.events` avec la clé de routage `AvisModere`.
- Le consommateur `ProfileUpdated` met à jour le `nomUtilisateur` stocké dans les avis ; sa file `skillmatch.entreprises.ProfileUpdated` et son listener sont désactivés par défaut et activables avec `RABBITMQ_LISTENER_ENABLED=true`.
- Documentation Swagger UI : `http://localhost:8083/swagger-ui.html` ; spécification JSON : `http://localhost:8083/v3/api-docs`.
- Erreurs JSON au format `{"status":404,"message":"Entreprise introuvable"}`.
- Détails complets : [`entreprises-service/README.md`](entreprises-service/README.md).

Intégration frontend : le back office `ADMIN` gère les utilisateurs, le CRUD des entreprises et la modération des avis ; le site public affiche l'annuaire et les avis `APPROUVE`, et permet à un `CANDIDAT` ou `RECRUTEUR` connecté de publier un avis. Ces deux flux passent par les routes décrites ci-dessus via la gateway. Voir [`Frontend/README.md`](../Frontend/README.md) et le scénario de vérification complet ci-dessous.

### Tests & Compétences

- Entités : `TestTechnique`, `ResultatTest`
- Les questions d'un test sont stockées en colonne JSON.
- La correction est automatique.
- Un candidat dispose de trois tentatives maximum.
- Publie `TestPassed` lorsqu'un test est réussi.

### Alertes & Favoris

- Entités : `AlerteEmploi`, `OffreSauvegardee`
- Une alerte contient les critères de recherche sauvegardés par le candidat.
- Consomme `OfferPublished` et publie `AlertMatched` lorsqu'une offre correspond aux critères.

### Messagerie

- Entités : `Conversation`, `Message`
- Les identifiants des participants sont stockés en colonne JSON.
- Seuls les participants d'une conversation peuvent lire ses messages.
- Publie `MessageSent`.

### Service commun

- Entité actuellement implémentée : `Utilisateur`.
- Rôle de l'utilisateur : `CANDIDAT`, `RECRUTEUR` ou `ADMIN`.
- Authentification avec mot de passe haché BCrypt et émission de JWT signés.
- Les notifications font partie de la conception globale mais ne sont pas encore développées.

Endpoints implémentés :

| Méthode | Route | Description |
|---|---|---|
| `POST` | `/skillmatch/auth/register` | Créer un compte et retourner un JWT |
| `POST` | `/skillmatch/auth/login` | Se connecter et retourner un JWT |
| `GET` | `/skillmatch/auth/me` | Récupérer l'utilisateur à partir du JWT |
| `GET` | `/skillmatch/utilisateurs` | Lister les utilisateurs (ADMIN) |
| `GET` | `/skillmatch/utilisateurs/{id}` | Récupérer un utilisateur (ADMIN) |
| `POST` | `/skillmatch/utilisateurs` | Créer un utilisateur (ADMIN) |
| `PUT` | `/skillmatch/utilisateurs/{id}` | Modifier un utilisateur (ADMIN) |
| `DELETE` | `/skillmatch/utilisateurs/{id}` | Supprimer un utilisateur (ADMIN) |

### API Gateway

- Point d'entrée public du frontend : `http://localhost:8080`.
- Routes `/skillmatch/auth/**` et `/skillmatch/utilisateurs/**` vers `commun-service` sur `http://localhost:8087`.
- Routes `/skillmatch/entreprises/**` et `/skillmatch/avis/**` vers `entreprises-service` sur `http://localhost:8083`.
- La gateway transmet l'en-tête `Authorization` ; les microservices valident le JWT et les rôles.
- CORS autorisé en développement ; à restreindre en production.

## Scénario de vérification complet (curl)

Le scénario couvre `register` / `login`, la création d'entreprise, la publication d'avis, la modération et la moyenne recalculée. Il suppose `commun-service` (8087), `entreprises-service` (8083) et la gateway démarrés, avec MySQL accessible. Les emails sont uniques grâce à un suffixe temporel.

```bash
GATEWAY="${GATEWAY:-http://localhost:8080}"
SUFFIX="$(date +%s)"
ADMIN_EMAIL="admin.verif.${SUFFIX}@example.com"
CANDIDAT_EMAIL="candidat.verif.${SUFFIX}@example.com"

curl -sS -o /tmp/admin-register.json -w 'register ADMIN: %{http_code}\n' -X POST "$GATEWAY/skillmatch/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"nom\":\"Admin Verification\",\"email\":\"$ADMIN_EMAIL\",\"password\":\"123456\",\"role\":\"ADMIN\"}"
curl -sS -o /tmp/admin-login.json -w 'login ADMIN: %{http_code}\n' -X POST "$GATEWAY/skillmatch/auth/login" \
  -H 'Content-Type: application/json' -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"123456\"}"
ADMIN_TOKEN=$(python3 -c 'import json; print(json.load(open("/tmp/admin-login.json"))["token"])')

curl -sS -o /tmp/candidat-register.json -w 'register CANDIDAT: %{http_code}\n' -X POST "$GATEWAY/skillmatch/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"nom\":\"Candidat Verification\",\"email\":\"$CANDIDAT_EMAIL\",\"password\":\"123456\",\"role\":\"CANDIDAT\"}"
curl -sS -o /tmp/candidat-login.json -w 'login CANDIDAT: %{http_code}\n' -X POST "$GATEWAY/skillmatch/auth/login" \
  -H 'Content-Type: application/json' -d "{\"email\":\"$CANDIDAT_EMAIL\",\"password\":\"123456\"}"
CANDIDAT_TOKEN=$(python3 -c 'import json; print(json.load(open("/tmp/candidat-login.json"))["token"])')

curl -sS -o /tmp/entreprise.json -w 'create entreprise: %{http_code}\n' -X POST "$GATEWAY/skillmatch/entreprises" \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"nom":"Annuaire Verification","secteur":"Informatique","ville":"Tunis","description":"Entreprise de test curl"}'
ID_ENTREPRISE=$(python3 -c 'import json; print(json.load(open("/tmp/entreprise.json"))["id"])')

curl -sS -o /tmp/entreprises.json -w 'list entreprises (public): %{http_code}\n' "$GATEWAY/skillmatch/entreprises"
curl -sS -o /tmp/avis.json -w 'publish avis: %{http_code}\n' -X POST "$GATEWAY/skillmatch/entreprises/$ID_ENTREPRISE/avis" \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $CANDIDAT_TOKEN" \
  -d '{"note":4,"commentaire":"Bonne equipe et entretien clair"}'
ID_AVIS=$(python3 -c 'import json; print(json.load(open("/tmp/avis.json"))["id"])')

curl -sS -o /tmp/entreprise-avant.json -w 'note avant moderation: %{http_code}\n' "$GATEWAY/skillmatch/entreprises/$ID_ENTREPRISE"
python3 -c 'import json; print("noteMoyenne avant:", json.load(open("/tmp/entreprise-avant.json"))["noteMoyenne"])'

curl -sS -o /tmp/moderation.json -w 'moderation APPROUVE: %{http_code}\n' -X PUT "$GATEWAY/skillmatch/avis/$ID_AVIS/moderation" \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"statut":"APPROUVE"}'
curl -sS -o /tmp/entreprise-apres.json -w 'note apres moderation: %{http_code}\n' "$GATEWAY/skillmatch/entreprises/$ID_ENTREPRISE"
python3 -c 'import json; print("noteMoyenne apres:", json.load(open("/tmp/entreprise-apres.json"))["noteMoyenne"])'
curl -sS -o /tmp/avis-list.json -w 'avis publics: %{http_code}\n' "$GATEWAY/skillmatch/entreprises/$ID_ENTREPRISE/avis"
python3 -c 'import json; print("statuts:", [a["statut"] for a in json.load(open("/tmp/avis-list.json"))])'
```

Résultat attendu : `201`, `200`, `201`, `200`, `201`, `200`, `201`, `200`, `200`, `200`, `200` ; `noteMoyenne` passe de `0.0` à `4.0` et l'avis est `APPROUVE`. Un second `POST` avec le même candidat répond `409`, un `POST` avec le JWT `ADMIN` répond `403` et un `POST` sans JWT répond `401`.

Si le port `8080` est occupé (par exemple par Apache), démarrer la gateway avec `SERVER_PORT=8090 mvn spring-boot:run` puis lancer le scénario avec `GATEWAY=http://localhost:8090`. La validation réelle a d'ailleurs été effectuée sur `http://localhost:8090` dans cet environnement.

## Créer un microservice

Utiliser le template fourni :

```bash
cp -r Backend/_template-service Backend/<nom-service>
```

Puis adapter le package, l'`artifactId`, le nom de l'application, le port et la base de données selon le tableau ci-dessus.

Consulter [`_template-service/README.md`](_template-service/README.md) pour la procédure détaillée.

## Bases de données

Créer les bases suivantes dans phpMyAdmin sous XAMPP :

```text
offres_db
profils_db
entreprises_db
tests_db
alertes_db
messagerie_db
commun_db
```

Chaque service se connecte uniquement à sa propre base avec les variables d'environnement définies dans `.env.example`. La base est créée automatiquement au premier démarrage si l'utilisateur MySQL a le droit `CREATE` ; Hibernate crée et met à jour les tables avec `ddl-auto: update`.

Spring Boot ne charge pas automatiquement `.env` : exporter les variables (`set -a; . ./.env; set +a`) ou les définir dans l'IDE avant de lancer Maven.

## Événements

Les événements sont nommés de manière métier et publiés via RabbitMQ.

- `OfferPublished` : une offre est publiée.
- `ApplicationSubmitted` : une candidature est déposée.
- `StatusChanged` : le statut d'une candidature change.
- `ProfileUpdated` : le profil d'un candidat est mis à jour.
- `TestPassed` : un test technique est réussi.
- `AlertMatched` : une offre correspond à une alerte.
- `AvisModere` : un avis a été modéré par `entreprises-service`.
- `MessageSent` : un message est envoyé.

Prévu mais non implémenté : le service commun consommera `StatusChanged`, `AlertMatched`, `TestPassed` et `MessageSent` pour générer les notifications.
