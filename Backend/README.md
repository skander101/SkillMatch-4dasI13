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
- Les ports et les noms de bases sont fixés dans le tableau ci-dessous

## Contrats des microservices

| Microservice | Port | Base | Entités | Appels synchrones | Événements publiés | Événements consommés |
|---|---:|---|---|---|---|---|
| `offres-service` | 8081 | `offres_db` | `Offre`, `Candidature` | Profils, Tests | `OfferPublished`, `StatusChanged`, `ApplicationSubmitted` | — |
| `profils-service` | 8082 | `profils_db` | `Profil`, `CV` | — | `ProfileUpdated` | `TestPassed` |
| `entreprises-service` | 8083 | `entreprises_db` | `Entreprise`, `AvisEntreprise` | — | — | — |
| `tests-service` | 8084 | `tests_db` | `TestTechnique`, `ResultatTest` | — | `TestPassed` | — |
| `alertes-service` | 8085 | `alertes_db` | `AlerteEmploi`, `OffreSauvegardee` | — | `AlertMatched` | `OfferPublished` |
| `messagerie-service` | 8086 | `messagerie_db` | `Conversation`, `Message` | — | `MessageSent` | — |
| `commun-service` | 8087 | `commun_db` | `Utilisateur` | Auth JWT | — | — |
| `api-gateway` | 8080 | — | — | Route `/skillmatch/auth/**` vers `commun-service` | — | — |
| `discovery-service` | 8761 | — | — | — | — | — |
| `config-service` | 8888 | — | — | — | — | — |

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

- Entités : `Entreprise`, `AvisEntreprise`
- Un utilisateur ne peut publier qu'un seul avis par entreprise.
- Les avis possèdent un statut de modération.
- La moyenne des notes est calculée par le service.

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
- Route `/skillmatch/auth/**` vers `commun-service` sur `http://localhost:8087`.
- CORS autorisé en développement ; à restreindre en production.

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
- `MessageSent` : un message est envoyé.

Prévu mais non implémenté : le service commun consommera `StatusChanged`, `AlertMatched`, `TestPassed` et `MessageSent` pour générer les notifications.
