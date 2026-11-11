# Microservice Entreprises & Avis

Microservice métier responsable des entreprises et des avis publiés par les candidats.

## État actuel

Squelette vide et exécutable : classe de démarrage, configuration Maven, connexion MySQL et configuration Eureka/RabbitMQ. Aucune entité, aucun endpoint et aucune règle métier ne sont encore implémentés.

## Informations

| Élément | Valeur |
|---|---|
| Responsable | Skander |
| Branche | `skander_entreprise+avis` |
| Package | `tn.esprit.skillmatch.entreprises` |
| Port | `8083` |
| Base de données | `entreprises_db` |
| Nom Eureka | `entreprises-service` |
| Préfixe API via Gateway | `/entreprises` |

## Entités prévues

- `Entreprise` : informations de l'entreprise.
- `AvisEntreprise` : avis d'un candidat sur une entreprise, avec statut de modération et note.

## Règles métier prévues

- Un utilisateur ne peut publier qu'un seul avis par entreprise.
- Les avis possèdent un statut de modération.
- La moyenne des notes est calculée par le service.
- Les identifiants des utilisateurs et des entreprises sont référencés sans clé étrangère inter-services.

## Contrat REST prévu

| Méthode | Endpoint | Rôle |
|---|---|---|
| `POST` | `/entreprises` | Créer une entreprise |
| `GET` | `/entreprises` | Lister les entreprises |
| `GET` | `/entreprises/{id}` | Consulter une entreprise |
| `PUT` | `/entreprises/{id}` | Modifier une entreprise |
| `DELETE` | `/entreprises/{id}` | Supprimer une entreprise |
| `POST` | `/entreprises/{id}/avis` | Publier un avis |
| `GET` | `/entreprises/{id}/avis` | Lister les avis d'une entreprise |
| `PUT` | `/avis/{id}/moderation` | Modérer un avis |
| `DELETE` | `/avis/{id}` | Supprimer un avis |

Codes prévus : `200`, `201`, `204`, `400`, `403`, `404`, `409`.

## Communications

- Appels synchrones : aucun.
- Événements publiés : aucun.
- Événements consommés : aucun.

## Structure prévue du code

```text
src/main/java/tn/esprit/skillmatch/entreprises/
├── api/
├── config/
├── domain/
├── dto/
├── event/
├── repository/
└── service/
```

## Démarrage

Créer la base `entreprises_db` dans phpMyAdmin sous XAMPP, puis :

```bash
cd Backend/entreprises-service
mvn spring-boot:run
```

Vérifier la santé du service :

```text
http://localhost:8083/actuator/health
```

## Variables d'environnement

- `SERVER_PORT` : port du service, `8083` par défaut.
- `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD` : connexion MySQL.
- `MYSQL_DATABASE` : `entreprises_db` par défaut.
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` : connexion RabbitMQ.
- `EUREKA_URL` : `http://localhost:8761/eureka/` par défaut.
