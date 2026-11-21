# Microservice Entreprises & Avis

Microservice métier responsable des entreprises et, dans les prochaines étapes, des avis publiés par les candidats.

## État actuel

Le socle est exécutable : classe de démarrage, configuration Maven, connexion MySQL, configuration Eureka/RabbitMQ et CRUD complet de l'entité `Entreprise`. Les avis, la modération, la moyenne calculée, l'appel synchrone à `commun-service`, les événements et le routage via la gateway sont prévus dans les étapes suivantes du plan.

## Informations

| Élément | Valeur |
|---|---|
| Responsable | Skander |
| Branche | `skander_entreprise+avis` |
| Package | `tn.esprit.skillmatch.entreprises` |
| Port | `8083` |
| Base de données | `entreprises_db` |
| Nom Spring | `entreprises-service` |
| Préfixe API | `/skillmatch/entreprises` |

## Entité implémentée

- `Entreprise` : `id`, `nom`, `secteur`, `ville`, `description`, `noteMoyenne`.
- `noteMoyenne` est initialisée à `0.0` et sera recalculée à l'étape 3 à partir des avis approuvés.
- Aucune clé étrangère inter-services : les identifiants d'utilisateurs seront stockés sous forme de `Long` lors de l'étape 2.

## Endpoints

| Méthode | Route | Description |
|---|---|---|
| `POST` | `/skillmatch/entreprises` | Créer une entreprise |
| `GET` | `/skillmatch/entreprises` | Lister les entreprises |
| `GET` | `/skillmatch/entreprises/{id}` | Consulter une entreprise |
| `PUT` | `/skillmatch/entreprises/{id}` | Modifier une entreprise |
| `DELETE` | `/skillmatch/entreprises/{id}` | Supprimer une entreprise |

Codes de réponse : `200`, `201`, `204`, `400`, `404`.

Les erreurs JSON ont la forme `{"status":404,"message":"Entreprise introuvable"}`.

## Documentation Swagger / OpenAPI

Une fois le service démarré :

| Ressource | URL |
|---|---|
| Interface Swagger UI | `http://localhost:8083/swagger-ui.html` |
| Spécification OpenAPI JSON | `http://localhost:8083/v3/api-docs` |

La documentation est générée avec `springdoc-openapi` et décrit le tag `Entreprises`, les endpoints CRUD, les DTOs et le schéma de sécurité JWT `bearerAuth` qui sera utilisé lors des prochaines étapes.

Exemple de création :

```bash
curl -i -X POST http://localhost:8083/skillmatch/entreprises \
  -H 'Content-Type: application/json' \
  -d '{"nom":"TechNova","secteur":"Informatique","ville":"Tunis","description":"Recrutement tech"}'
```

## Démarrage

La base `entreprises_db` est créée automatiquement au premier démarrage si l'utilisateur MySQL a le droit `CREATE`. Hibernate crée la table `entreprises` avec `ddl-auto: update`.

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
