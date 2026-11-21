# Offres & Candidatures — Phase 1

Projet Maven indépendant, créé depuis `Backend/_template-service` : Java 17,
Spring Boot 3.3.5, Spring Cloud 2023.0.3. Aucun POM parent dans `Backend`.

## État actuel : entités et CRUD Offre

- Application : `tn.esprit.skillmatch.offres.OffresServiceApplication`.
- Port par défaut : **8081** ; base MySQL : **offres_db**.
- Les entités `Offre` et `Candidature` et le CRUD REST des offres sont présents.
  La recherche simple par titre et le tri des offres sont présents.
  Le CRUD Candidature et les interfaces Angular restent à faire.
  L'identité est vérifiée auprès de `commun-service` ; la vérification locale
  des signatures JWT dans `offres-service` reste prévue à l'étape sécurité.
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
| `COMMUN_SERVICE_URL` | `http://localhost:8087` |

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

Pour utiliser le CRUD, `commun-service` doit également tourner sur 8087.
La Gateway sur 8080 sera utilisée après ajout de ses routes. Le service peut
toujours démarrer seul avec MySQL ; ses opérations métier nécessitent une
authentification valide auprès de `commun-service`.

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
  Entreprise, Test ou CV. À l'étape 3, une création administrative vérifie
  l'existence et le rôle du recruteur auprès du service commun. Les références
  Entreprise, Test et CV ne sont pas encore vérifiées.
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
  création/publication est maintenant réalisée dans le service CRUD.
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

La première commande exécute les tests sans MySQL. La seconde exige
MySQL/XAMPP et vérifie réellement le JSON, les dates automatiques et la clé
étrangère dans `offres_db`. Elle utilise les paramètres MySQL du service et
`ddl-auto: update`. Les données de test sont annulées à la fin des transactions ;
les tables restent créées et les compteurs d'identifiants peuvent avancer.
Les identifiants utilisateur/CV utilisés sont des références de test, sans
prétendre vérifier leur existence dans les autres services.

Résultat de l'étape 2 : `mvn package` réussi (9 tests unitaires), puis
`mvn "-Dtest=EntityPersistenceIT" test` réussi (2 tests de persistance XAMPP).

## Étape 3 : CRUD Offre

Le chemin d'une requête est simple : **Controller → Service → Repository → MySQL**.
Les nouveaux fichiers Java sont commentés pour expliquer chaque ligne utile.

| Méthode | Route | Résultat |
|---|---|---|
| GET | `/skillmatch/offres` | ADMIN : toutes ; RECRUTEUR : les siennes ; CANDIDAT : publiées et valides |
| GET | `/skillmatch/offres/mes-offres` | Offres du gestionnaire connecté ; interdit au candidat |
| GET | `/skillmatch/offres/{id}` | Détail selon les mêmes droits |
| POST | `/skillmatch/offres` | Création d'un brouillon, réponse 201 |
| PUT | `/skillmatch/offres/{id}` | Modification ou publication, réponse 200 |
| DELETE | `/skillmatch/offres/{id}` | Suppression sans candidature, réponse 204 |

### Propriété et règles

- RECRUTEUR : propriétaire défini par sa session ; jamais par un identifiant
  libre envoyé par Angular. Ses modifications et suppressions sont limitées à
  ses offres. Il ne peut pas transférer une offre à un autre recruteur.
- ADMIN : CRUD global. Pour créer, `recruteurId` est obligatoire et vérifié
  via `/skillmatch/utilisateurs/{id}` avec son JWT ADMIN. Le propriétaire reste
  inchangé lors d'un PUT.
- CANDIDAT : lecture des seules offres `PUBLIEE` dont l'expiration n'est pas
  passée ; aucune écriture.
- POST exige `BROUILLON`. Pour publier, envoyer ensuite un PUT avec `PUBLIEE`.
  Un PUT remplace les champs du formulaire : renvoyer toutes les valeurs utiles.
- BROUILLON et PUBLIEE exigent une date aujourd'hui ou ultérieure. EXPIREE
  exige une date passée. FERMEE peut conserver une ancienne date d'expiration.
- Une offre avec candidature ne peut pas être supprimée : réponse 409 ; utiliser
  PUT avec `FERMEE` pour conserver l'historique. La clé étrangère reste une
  protection supplémentaire en cas d'écritures simultanées.
- Codes testés : 200, 201, 204, 400, 401, 403, 404, 409. Les erreurs JSON ont
  la forme `{"status":403,"message":"..."}`, comme dans le service commun.

### Authentification minimale de cette étape

Chaque opération transmet `Authorization: Bearer ...` à `/skillmatch/auth/me`.
Le service commun vérifie réellement le JWT et retourne l'identifiant et le rôle
depuis sa base ; ils ne sont pas déduits du formulaire ou du stockage Angular.
Une réponse auth HTTP en erreur est traitée comme un refus 401. Cela couvre
aussi le comportement actuel de commun-service qui peut répondre 500 pour un
JWT malformé ; une autre panne HTTP interne de `/me` apparaîtrait donc aussi en
401. Une connexion impossible ou un délai dépassé renvoie 503. Aucun accès
n'est accordé si l'identité ne peut pas être vérifiée.

La vérification locale du JWT et ses tests cryptographiques seront ajoutés à
l'étape sécurité. Aucun secret JWT n'est ajouté ici. La faille existante de
l'inscription publique acceptant ADMIN reste à signaler au responsable de
`commun-service` : elle n'est pas corrigée par ce CRUD.

### Exemple de formulaire

```json
{
  "titre": "Développeur Java",
  "description": "Développement d'API REST",
  "competences": ["Java", "Spring Boot"],
  "localisation": "Tunis",
  "contrat": "CDI",
  "salaire": 2000,
  "dateExpiration": "2030-12-31",
  "statut": "BROUILLON",
  "entrepriseId": null,
  "testRequisId": null,
  "recruteurId": null
}
```

Pour ADMIN, remplacer `recruteurId` par l'identifiant réel d'un recruteur.
Adapter la date au jour de la démonstration. Récupérer le JWT par le login
existant puis le placer dans une variable locale, jamais dans Git.

Exemples PowerShell avec le JWT dans `$env:SKILLMATCH_TOKEN` et le JSON ci-dessus
dans un fichier local `offre-exemple.json` :

```powershell
curl.exe -H "Authorization: Bearer $env:SKILLMATCH_TOKEN" http://localhost:8081/skillmatch/offres
curl.exe -X POST -H "Authorization: Bearer $env:SKILLMATCH_TOKEN" -H "Content-Type: application/json" --data-binary "@offre-exemple.json" http://localhost:8081/skillmatch/offres
curl.exe -X PUT -H "Authorization: Bearer $env:SKILLMATCH_TOKEN" -H "Content-Type: application/json" --data-binary "@offre-exemple.json" http://localhost:8081/skillmatch/offres/ID
curl.exe -X DELETE -H "Authorization: Bearer $env:SKILLMATCH_TOKEN" http://localhost:8081/skillmatch/offres/ID
```

Remplacer `ID` par l'identifiant retourné et passer le statut du formulaire à
`PUBLIEE` pour publier. Utiliser 18081 seulement si le service a été lancé avec
le port temporaire de diagnostic. Les routes Gateway et Angular ne sont pas
modifiées à cette étape.

### Tests du CRUD

```powershell
mvn test
mvn "-Dtest=OffreCrudIT,EntityPersistenceIT" test
mvn package
```

- `OffreServiceTest` : rôles, propriétaires, dates, brouillon/publication et suppression.
- `OffreControllerTest` : routes HTTP via MockMvc, validation et codes d'erreur.
- `IdentiteServiceTest` : vrai client HTTP contre un petit serveur local simulant
  le contrat de commun-service ; aucune vraie signature JWT n'est testée ici.
- `OffreCrudIT` : un seul scénario court avec le vrai service et MySQL : créer,
  lire, modifier et supprimer. L'identité distante est simulée et les données
  sont annulées après le test. Les routes HTTP sont testées dans `OffreControllerTest`.

Les tests `*IT` exigent MySQL et sont lancés explicitement. Aucun compte réel
n'est créé. Les compteurs d'identifiants peuvent avancer malgré le rollback.

Vérifications de cette étape : 49 tests sans MySQL réussis et 5 tests
d'intégration MySQL réussis (3 parcours CRUD et 2 tests du mapping JPA).
Une lecture de contrôle confirme zéro offre et zéro candidature résiduelles
après ces tests. La connexion HTTP à un vrai `commun-service` avec de vrais
comptes et les signatures JWT restent à vérifier à l'étape sécurité.

## Recherche et tri simples

Les deux listes `/skillmatch/offres` et `/skillmatch/offres/mes-offres`
acceptent seulement deux paramètres :

- `search` : partie du titre, sans tenir compte de la casse.
- `sort` : `titre` (A-Z, par défaut), `salaire` (croissant, absents à la fin)
  ou `dateExpiration` (date la plus proche d'abord).
- Pour le sens décroissant : `titreDesc` (Z-A), `salaireDesc` (plus grand
  au plus petit, absents à la fin) et `dateExpirationDesc` (date la plus lointaine).

Exemple : `/skillmatch/offres?search=Java&sort=salaire`.
Exemple décroissant : `/skillmatch/offres?search=Java&sort=salaireDesc`.

Le repository charge d'abord uniquement les offres autorisées selon le rôle.
Une petite méthode Java recherche ensuite dans le titre et trie cette liste.
Cette solution simple convient au projet pédagogique ; elle charge les offres
autorisées en mémoire. Il n'y a plus de filtres supplémentaires, de DTO de
recherche ou de Specifications JPA. Les permissions existantes sont conservées.

Tests : `mvn test`, puis
`mvn "-Dtest=OffreCrudIT,EntityPersistenceIT" test` avec MySQL démarré.
Les 51 tests sans MySQL ont été vérifiés. Le test `OffreCrudIT` a ensuite été
raccourci en un seul scénario ; ses règles détaillées restent couvertes par
`OffreServiceTest`, `OffreControllerTest` et `EntityPersistenceIT`.

Dans `OffreService`, `trouverAccessible` regroupe la recherche d'une offre et
ses droits d'accès. `exigerGestionnaire` authentifie et retourne le gestionnaire
autorisé. `verifierDonnees`, `remplir` et `rechercherEtTrier` restent partagées
pour éviter de répéter les mêmes instructions dans les opérations CRUD.
