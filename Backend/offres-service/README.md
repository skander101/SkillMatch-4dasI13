# Offres & Candidatures — Phase 1

Projet Maven indépendant, créé depuis `Backend/_template-service` : Java 17,
Spring Boot 3.3.5, Spring Cloud 2023.0.3. Aucun POM parent dans `Backend`.

## État actuel : socle et entités

- Application : `tn.esprit.skillmatch.offres.OffresServiceApplication`.
- Port par défaut : **8081** ; base MySQL : **offres_db**.
- Les entités `Offre` et `Candidature` sont présentes. Les CRUD, autorisations
  JWT et interfaces Angular seront ajoutés aux étapes suivantes.
  Aucun endpoint métier n'est encore disponible.
- Les dépendances AMQP et Eureka du template sont conservées. L'autoconfiguration
  RabbitMQ est exclue ; l'enregistrement Eureka et la récupération du registre
  sont désactivés par défaut. Aucun serveur RabbitMQ, Eureka ou Config Server
  n'est nécessaire pour ce socle.

## Démarrage dans IntelliJ

1. Démarrer MySQL dans XAMPP (port 3306).
2. Créer `offres_db` dans phpMyAdmin, ou utiliser un compte autorisé à créer la
   base : l'URL JDBC contient `createDatabaseIfNotExist=true`.
3. Ouvrir **File > Open > Backend/offres-service**, puis importer le `pom.xml`.
4. Choisir le JDK 17 et créer une configuration **Application** avec la classe
   `tn.esprit.skillmatch.offres.OffresServiceApplication`, le classpath du module
   et le dossier `Backend/offres-service` comme répertoire de travail.
5. Définir les variables d'environnement dans cette configuration, puis lancer
   `OffresServiceApplication.main()`.

| Variable | Valeur par défaut |
|---|---|
| `SERVER_PORT` | `8081` |
| `MYSQL_HOST` | `localhost` |
| `MYSQL_PORT` | `3306` |
| `MYSQL_DATABASE` | `offres_db` |
| `MYSQL_USER` | `root` |
| `MYSQL_PASSWORD` | vide, à définir selon votre installation |
| `EUREKA_REGISTER` | `false` |
| `EUREKA_FETCH` | `false` |

Ne pas enregistrer de mot de passe dans Git. Spring Boot ne charge pas
automatiquement un fichier `.env`. Garder les deux variables Eureka à `false`
pendant la phase 1. MySQL reste nécessaire au démarrage normal.

## Commandes

Depuis `Backend/offres-service`, avec Maven disponible dans le PATH :

```powershell
mvn test
mvn package
mvn spring-boot:run
```

Avec MySQL démarré, vérifier le socle :

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Le test adapté du template vérifie uniquement la présence de la classe de
démarrage ; il ne valide ni la connexion MySQL ni un CRUD. Le démarrage réel et
le contrôle de santé doivent être vérifiés séparément avec MySQL disponible.

Pour les étapes d'authentification suivantes, `commun-service` devra également
tourner sur 8087 ; la Gateway sur 8080 sera utilisée après ajout de ses routes.
Ils ne sont pas nécessaires pour lancer ce socle sans endpoints métier.

## Dépannage local

Si le démarrage indique que le port 8081 est occupé, identifier l'application
concernée et libérer ce port avant la démonstration. Le port officiel reste 8081.
Pour un diagnostic isolé, un port temporaire peut être passé en argument sans
modifier la configuration :

```powershell
java -jar target/offres-service-0.0.1-SNAPSHOT.jar --server.port=18081
Invoke-RestMethod http://localhost:18081/actuator/health
```

## Vérification du socle

- `mvn package` : compilation et JAR exécutable réussis ; 1 test passé.
- Démarrage avec XAMPP : connexion JDBC et création de `offres_db` réussies.
- Contrôle HTTP sur le port temporaire 18081 : `200 {"status":"UP"}`.
  Le port 8081 était occupé par une autre application lors de cette vérification.
- XAMPP fournit ici MariaDB 10.4.32. Avec le pilote MySQL du template, Hibernate
  émet un avertissement de version du dialecte. Le socle démarre ; les tests de
  persistance de l'étape 2 passent sur cette installation.

## Étape 2 : entités et relation

- `Offre` devient la table `offres`, `Candidature` devient `candidatures`.
- Chaque candidature possède une offre obligatoire (`offre_id`). Plusieurs
  candidatures peuvent référencer la même offre. Pas de suppression en cascade :
  la clé étrangère protège une offre qui possède des candidatures.
- `recruteurId` identifie le propriétaire de l'offre ; `entrepriseId` reste une
  référence différente. Ces identifiants ne créent pas de tables Utilisateur,
  Entreprise, Test ou CV. Leur existence n'est pas encore vérifiée.
- `competences` est une liste Java stockée en JSON dans une colonne TEXT,
  grâce à `CompetencesConverter`, sans table supplémentaire.
- Les enums reprennent exactement les valeurs UML. Une offre commence en
  `BROUILLON`. À l'insertion, une candidature reçoit la date courante et `RECUE`.
- Validations : titre obligatoire, salaire positif ou nul, contrat, statut,
  date d'expiration et propriétaire obligatoires ; identifiants positifs.
- `estExpiree()` retourne vrai si la date d'expiration est avant aujourd'hui.
  Une offre reste ouverte jusqu'à la fin du jour indiqué.
- La date n'utilise pas `@FutureOrPresent` sur l'entité : une offre expirée doit
  pouvoir être conservée et fermée ultérieurement. La validation de la date à la
  création/publication sera ajoutée au CRUD.
- `calculerScore()` calcule seulement un pourcentage de compétences communes,
  sans tenir compte de la casse ou des doublons. Aucun appel Profils, moteur de
  matching ou endpoint de recommandation n'est ajouté.
- `changerStatut()` refuse une valeur nulle. Les transitions autorisées et les
  contrôles de rôle appartiendront au service métier des étapes suivantes.

### Tests des entités

```powershell
mvn package
mvn "-Dtest=EntityPersistenceIT" test
```

La première commande exécute les tests unitaires sans MySQL. La seconde exige
MySQL/XAMPP et vérifie réellement le JSON, les dates automatiques et la clé
étrangère dans `offres_db`. Elle utilise les paramètres MySQL du service et
`ddl-auto: update`. Les données de test sont annulées à la fin des transactions ;
les tables restent créées et les compteurs d'identifiants peuvent avancer.
Les identifiants utilisateur/CV utilisés sont des références de test, sans
prétendre vérifier leur existence dans les autres services.

Résultat de l'étape 2 : `mvn package` réussi (9 tests unitaires), puis
`mvn "-Dtest=EntityPersistenceIT" test` réussi (2 tests de persistance XAMPP).
