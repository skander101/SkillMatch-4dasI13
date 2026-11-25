# SkillMatch

Plateforme de mise en relation entre candidats et recruteurs, construite en microservices dans le cadre du projet AWD de la filière 4DAS (Data & AI) à ESPRIT.

> Validation intermédiaire — séance 4. Le projet reste en cours : ce dépôt contient la conception, la structure, la répartition du travail et les premières tranches fonctionnelles fusionnées sur `main` et `skander_entreprise+avis`.

## Problématique

Les candidats ont du mal à identifier les offres réellement adaptées à leur profil, tandis que les recruteurs reçoivent de nombreuses candidatures peu qualifiées. SkillMatch centralise les offres, les profils, les tests techniques, les avis d'entreprises, les alertes et la messagerie dans une plateforme unique.

## Acteurs

- **Candidat** : crée son profil et son CV, consulte les offres, postule, passe des tests, sauvegarde des offres et définit des alertes.
- **Recruteur** : publie des offres, consulte les candidatures, planifie des entretiens et échange avec les candidats.
- **Administrateur** : modère les avis et supervise la plateforme.

## Storytelling et ODD

[À COMPLÉTER : storytelling retenu et correspondance avec les ODD 8, 4, 10 et 16.]

## Fonctionnalités principales

- Authentification JWT et gestion des rôles.
- Gestion des offres et des candidatures avec suivi de statut.
- Recommandation d'offres calculée à partir du profil du candidat.
- Gestion du profil, du CV, des compétences et des badges.
- Tests techniques avec correction automatique et limite de tentatives.
- Avis sur les entreprises avec modération.
- Alertes emploi et offres sauvegardées.
- Messagerie entre candidats et recruteurs.
- Notifications prévues dans le service commun (non implémentées pour l'instant).

## Architecture globale

L'application suit une architecture microservices :

- un **API Gateway** expose l'API publique et transmet les JWT aux services qui les valident ;
- **Eureka** assure la découverte des services et **Spring Cloud Config** centralise la configuration ;
- six **microservices métier** indépendants ;
- un **service commun** pour l'authentification ; les notifications sont prévues mais non implémentées ;
- **RabbitMQ** pour les événements asynchrones ;
- **MySQL sous XAMPP** pour la persistance, avec une base distincte par service ;
- un **frontend** unique qui consomme l'API via la gateway.

Le schéma d'architecture complet et les autres diagrammes se trouvent dans [`Documentation/Diagrams/`](Documentation/Diagrams/).

## Microservices

| Microservice | Port | Base de données | Responsabilité |
|---|---:|---|---|
| `offres-service` | 8081 | `offres_db` | Offres, candidatures, recommandation et statuts |
| `profils-service` | 8082 | `profils_db` | Profils, CV, compétences, expériences et badges |
| `entreprises-service` | 8083 | `entreprises_db` | Entreprises et avis modérés |
| `tests-service` | 8084 | `tests_db` | Tests techniques, résultats et tentatives |
| `alertes-service` | 8085 | `alertes_db` | Alertes emploi et offres sauvegardées |
| `messagerie-service` | 8086 | `messagerie_db` | Conversations et messages |
| `commun-service` | 8087 | `commun_db` | Utilisateur et authentification JWT (sans notifications pour l'instant) |
| `api-gateway` | 8080 | — | Routage auth/utilisateurs et entreprises/avis ; validation JWT dans les services |
| `discovery-service` | 8761 | — | Serveur Eureka |
| `config-service` | 8888 | — | Configuration centralisée |
| `Frontend` | 4200 | — | Interface candidat, recruteur et administrateur |

Le détail des contrats, des entités, des appels synchrones et des événements est dans [`Backend/README.md`](Backend/README.md).

État d'intégration : `commun-service` (utilisateur + auth JWT + CRUD ADMIN, sans notifications) et `api-gateway` sont implémentés ; le frontend consomme les endpoints `/skillmatch/auth/*` et `/skillmatch/utilisateurs/*` via la gateway. Un back office s'ouvre automatiquement après connexion avec le rôle `ADMIN`.

Sur `skander_entreprise+avis`, le service `entreprises-service` fournit le CRUD de l'entité `Entreprise`, la publication et la modération des avis avec recalcul de la moyenne, ainsi que les événements RabbitMQ `AvisModere` (publié) et `ProfileUpdated` (consommé). Les routes `/skillmatch/entreprises/**` et `/skillmatch/avis/**` passent par la gateway sur `http://localhost:8080` et sont protégées par JWT : lecture publique, publication par `CANDIDAT`/`RECRUTEUR`, modération et CRUD entreprise par `ADMIN`. Swagger reste disponible sur `http://localhost:8083/swagger-ui.html`. Le frontend associé reste à implémenter dans les commits suivants du plan.

## Technologies

- Java 17, Spring Boot 3.3.x, Spring Cloud 2023.x
- Spring Data JPA, Spring Web, Spring AMQP
- MySQL sous XAMPP
- RabbitMQ
- API Gateway, Eureka, Spring Cloud Config
- JWT
- springdoc-openapi et Swagger UI pour la documentation des API
- Angular et TypeScript pour le frontend
- Docker Compose pour RabbitMQ et les outils d'infrastructure
- Git et GitHub pour la gestion de version

## Structure du dépôt

```text
skillmatch-4das16-gX/
├── Backend/
│   ├── README.md
│   ├── _template-service/
│   ├── offres-service/
│   ├── profils-service/
│   ├── entreprises-service/
│   ├── tests-service/
│   ├── alertes-service/
│   ├── messagerie-service/
│   ├── commun-service/
│   ├── api-gateway/
│   ├── discovery-service/
│   └── config-service/
├── Frontend/
├── Documentation/
│   ├── Presentation/
│   ├── Diagrams/
│   └── Technical-Document/
└── README.md
```

Les dossiers de microservices sont créés par leurs responsables sur leurs branches respectives, puis fusionnés dans `main` via des pull requests.

## Prérequis

- JDK 17
- Maven 3.9+
- Node.js 20+ et npm
- Angular CLI
- XAMPP avec MySQL démarré sur le port 3306
- RabbitMQ (via Docker Compose ou installation locale)
- Git

## Démarrage local

1. Démarrer MySQL dans XAMPP.
2. Créer les bases `offres_db`, `profils_db`, `entreprises_db`, `tests_db`, `alertes_db`, `messagerie_db` et `commun_db`.
3. Copier `.env.example` vers `.env` et adapter les valeurs locales.
4. Démarrer RabbitMQ. Le fichier `docker-compose.yml` sera ajouté par le responsable Docker/CI.
5. Démarrer `discovery-service`, `config-service`, les microservices, puis `api-gateway`.
6. Démarrer le frontend avec `npm install` puis `npm start` (ou `ng serve`).

Chaque service est un projet Maven indépendant. Depuis son dossier :

```bash
mvn spring-boot:run
```

### Démarrage minimal : authentification, back office et entreprises

Tant que les autres microservices métier ne sont pas développés, le socle exécutable comprend MySQL, `commun-service`, `api-gateway`, le frontend et, sur la branche entreprises, `entreprises-service`.

```bash
cp .env.example .env
set -a; . ./.env; set +a
```

Puis, dans quatre terminaux distincts :

```bash
cd Backend/commun-service && mvn spring-boot:run
```

```bash
cd Backend/api-gateway && mvn spring-boot:run
```

```bash
cd Frontend && npm install && npm run dev
```

```bash
cd Backend/entreprises-service && mvn spring-boot:run
```

Spring Boot ne lit pas automatiquement le fichier `.env` : les variables doivent être exportées dans chaque terminal ou définies dans l'IDE. Les bases `commun_db` et `entreprises_db` sont créées au premier démarrage si l'utilisateur MySQL dispose du droit `CREATE`.

`entreprises-service` est routé par la gateway sur `http://localhost:8080` ; Swagger et le health restent accessibles directement sur `http://localhost:8083`. Les événements utilisent l'exchange `skillmatch.events` ; le listener `ProfileUpdated` est désactivé par défaut, donc le service démarre sans RabbitMQ et la publication d'`AvisModere` journalise les erreurs broker sans annuler la modération. L'enregistrement Eureka est désactivé par défaut en local.

## Organisation Git

- `main` contient le socle commun et les fonctionnalités validées.
- Chaque membre travaille sur une branche dédiée, par exemple `feature/<service>` ou `skander_entreprise+avis`.
- Les pull requests sont relues avant fusion.
- Chaque microservice possède un premier commit clair créé par son responsable.

Voir [`CONTRIBUTING.md`](CONTRIBUTING.md) pour les conventions détaillées.

## Équipe et répartition

| Membre | Microservice / responsabilité | Branche |
|---|---|---|
| Membre 1 | Offres & Candidatures | `feature/offres-service` |
| Membre 2 | Profils & CV | `feature/profils-service` |
| Membre 3 | Entreprises & Avis | `skander_entreprise+avis` |
| Membre 4 | Tests & Compétences | `feature/tests-service` |
| Membre 5 | Alertes & Favoris | `feature/alertes-service` |
| Membre 6 | Messagerie | `feature/messagerie-service` |

Les six membres se répartissent également les briques du service commun : authentification, notifications, API Gateway, Eureka et configuration, Docker Compose et CI, layout et login du frontend.

## Planning

| Semaine | Objectif |
|---|---|
| S1 | Conception, Git, diagrammes, document technique, présentation de la séance 4 |
| S2–S4 | Développement des microservices métier et du service commun |
| S3–S5 | Développement du frontend |
| S5 | Intégration REST et RabbitMQ |
| S6 | Tests et corrections |
| S7 | Documentation finale, démonstration et soutenance |

## Documentation

- [Documentation](Documentation/README.md)
- [Présentation](Documentation/Presentation/README.md)
- [Diagrammes](Documentation/Diagrams/README.md)
- [Document technique](Documentation/Technical-Document/README.md)
- [commun-service](Backend/commun-service/README.md)
- [entreprises-service](Backend/entreprises-service/README.md)
- [api-gateway](Backend/api-gateway/README.md)
- [Frontend](Frontend/README.md)
