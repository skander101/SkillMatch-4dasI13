# Microservice Entreprises & Avis

Microservice métier responsable des entreprises et des avis publiés par les candidats.

## État actuel

Le service fournit le CRUD complet de l'entité `Entreprise`, la publication d'avis avec contrainte d'unicité, la consultation des avis d'une entreprise, leur modération et le recalcul automatique de `noteMoyenne`. L'appel synchrone à `commun-service`, les événements et le routage via la gateway sont prévus dans les étapes suivantes du plan.

## Informations

| Élément | Valeur |
|---|---|
| Responsable | Skander |
| Branche | `skander_entreprise+avis` |
| Package | `tn.esprit.skillmatch.entreprises` |
| Port | `8083` |
| Base de données | `entreprises_db` |
| Nom Spring | `entreprises-service` |
| Préfixes API | `/skillmatch/entreprises` et `/skillmatch/avis` |

## Entités implémentées

- `Entreprise` : `id`, `nom`, `secteur`, `ville`, `description`, `noteMoyenne`.
- `AvisEntreprise` : `id`, `entreprise`, `utilisateurId`, `nomUtilisateur`, `note`, `commentaire`, `statut`, `dateCreation`.
- `StatutModeration` : `EN_ATTENTE`, `APPROUVE`, `REJETE`.
- `noteMoyenne` est initialisée à `0.0`, puis recalculée à chaque modération ou suppression d'avis à partir des seuls avis `APPROUVE` ; le résultat est arrondi à deux décimales et revient à `0.0` s'il ne reste aucun avis approuvé.
- Un utilisateur ne peut publier qu'un seul avis par entreprise : la contrainte unique `uk_avis_entreprise_utilisateur` couvre `entreprise_id` et `utilisateur_id`.
- Les identifiants d'utilisateurs sont stockés sous forme de `Long`, sans clé étrangère inter-services. Tant que la validation JWT n'est pas branchée (étape 4), `utilisateurId` et `nomUtilisateur` sont fournis dans le corps de la requête.

## Endpoints

| Méthode | Route | Description |
|---|---|---|
| `POST` | `/skillmatch/entreprises` | Créer une entreprise |
| `GET` | `/skillmatch/entreprises` | Lister les entreprises |
| `GET` | `/skillmatch/entreprises/{id}` | Consulter une entreprise |
| `PUT` | `/skillmatch/entreprises/{id}` | Modifier une entreprise |
| `DELETE` | `/skillmatch/entreprises/{id}` | Supprimer une entreprise |
| `POST` | `/skillmatch/entreprises/{id}/avis` | Publier un avis sur une entreprise |
| `GET` | `/skillmatch/entreprises/{id}/avis` | Lister les avis d'une entreprise |
| `PUT` | `/skillmatch/avis/{id}/moderation` | Approuver ou rejeter un avis |
| `DELETE` | `/skillmatch/avis/{id}` | Supprimer un avis |

Codes de réponse : `200`, `201`, `204`, `400`, `404`, `409`.

Les erreurs JSON ont la forme `{"status":404,"message":"Entreprise introuvable"}`. Un doublon répond `{"status":409,"message":"Un avis a déjà été publié par cet utilisateur pour cette entreprise"}` et un statut `EN_ATTENTE` envoyé à la modération répond `{"status":400,"message":"Le statut EN_ATTENTE n'est pas valide pour la modération"}`.

## Documentation Swagger / OpenAPI

Une fois le service démarré :

| Ressource | URL |
|---|---|
| Interface Swagger UI | `http://localhost:8083/swagger-ui.html` |
| Spécification OpenAPI JSON | `http://localhost:8083/v3/api-docs` |

La documentation est générée avec `springdoc-openapi` et décrit les tags `Entreprises`, `Avis` et `Modération`, les endpoints CRUD, avis et modération, les DTOs et le schéma de sécurité JWT `bearerAuth` qui sera utilisé lors des prochaines étapes.

Exemple de création :

```bash
curl -i -X POST http://localhost:8083/skillmatch/entreprises \
  -H 'Content-Type: application/json' \
  -d '{"nom":"TechNova","secteur":"Informatique","ville":"Tunis","description":"Recrutement tech"}'
```

Exemple de publication d'avis, en remplaçant `1` par l'identifiant de l'entreprise créée :

```bash
curl -i -X POST http://localhost:8083/skillmatch/entreprises/1/avis \
  -H 'Content-Type: application/json' \
  -d '{"utilisateurId":10,"nomUtilisateur":"Alice Martin","note":5,"commentaire":"Très bonne expérience"}'
```

Un second appel avec le même `utilisateurId` sur la même entreprise répond `409`. Une note inférieure à `1` ou supérieure à `5` répond `400`.

Exemple de modération, en remplaçant `2` par l'identifiant de l'avis créé :

```bash
curl -i -X PUT http://localhost:8083/skillmatch/avis/2/moderation \
  -H 'Content-Type: application/json' \
  -d '{"statut":"APPROUVE"}'
```

Un avis rejeté avec `{"statut":"REJETE"}` n'est pas inclus dans la moyenne. La suppression d'un avis se fait avec :

```bash
curl -i -X DELETE http://localhost:8083/skillmatch/avis/2
```

Après chaque modération ou suppression, `GET /skillmatch/entreprises/{id}` renvoie la `noteMoyenne` recalculée.

## Démarrage

La base `entreprises_db` est créée automatiquement au premier démarrage si l'utilisateur MySQL a le droit `CREATE`. Hibernate crée les tables `entreprises` et `avis_entreprises` avec `ddl-auto: update`.

```bash
cd Backend/entreprises-service
mvn spring-boot:run
```

Vérification de santé :

```text
http://localhost:8083/actuator/health
```

Tests :

```bash
mvn test
```

## Variables d'environnement

- `SERVER_PORT` : port du service, `8083` par défaut.
- `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`.
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`.
- `EUREKA_URL`, `EUREKA_REGISTER`, `EUREKA_FETCH` ; l'enregistrement est désactivé par défaut pour le démarrage local.

Spring Boot ne lit pas automatiquement le fichier `.env` : exporter les variables ou les définir dans l'IDE.
