# Offres & Candidatures — Phase 1

Projet Maven indépendant, créé depuis `Backend/_template-service` : Java 17,
Spring Boot 3.3.5, Spring Cloud 2023.0.3. Aucun POM parent dans `Backend`.

## État actuel : entités et CRUD Offre

- Application : `tn.esprit.skillmatch.offres.OffresServiceApplication`.
- Port par défaut : **8081** ; base MySQL : **offres_db**.
- Les entités `Offre` et `Candidature` et le CRUD REST des offres sont présents.
  La recherche simple par titre et le tri des offres sont présents.
  Le CRUD Candidature et les interfaces Angular sont présents.
  Le JWT est vérifié localement, puis l'identité est résolue auprès de
  `commun-service`. Les droits sont appliqués dans les services métier.
- Le client Eureka est actif : démarrer `discovery-service` sur 8761 avant Offres.
  RabbitMQ est maintenant actif pour les trois événements ; aucun Config Server n'est nécessaire.
  Voir [le guide Eureka](../discovery-service/README.md).
  Voir [le guide RabbitMQ](RABBITMQ.md) pour les messages, le lancement et les limites.

## Contrôles de saisie actuels

Les formulaires Angular et l'API Java appliquent les mêmes règles à l'ajout et à la modification.
Les espaces autour des textes ne comptent pas dans les longueurs.

| Offre | Règle |
|---|---|
| Titre | Obligatoire, 3 à 255 caractères |
| Description | Obligatoire, 20 à 10000 caractères |
| Compétences | Au moins une, 1 à 255 caractères chacune |
| Localisation | Obligatoire, 1 à 255 caractères |
| Contrat et statut | Valeurs autorisées obligatoires ; création BROUILLON |
| Salaire | Obligatoire, nombre fini positif ou nul |
| Expiration | Obligatoire, aujourd'hui ou dans le futur, quel que soit le statut |
| Entreprise | Choix obligatoire ; identifiant entier positif |
| Test requis | Choix facultatif, Aucun test transmet null ; identifiant positif si fourni |
| Recruteur | Session RECRUTEUR ; choix obligatoire et vérifié pour une création ADMIN |

| Candidature | Règle |
|---|---|
| Offre et candidat | Offre ouverte et candidat identifié ; sélection obligatoire pour ADMIN |
| Lettre | Obligatoire au dépôt et en modification candidat/ADMIN, 20 à 10000 caractères |
| Référence CV | Facultative, entier positif si fournie ; absente des formulaires |
| Statut | Valeur autorisée selon le rôle ; RECUE automatique au dépôt |
| Date d'entretien | Obligatoire lors du traitement ADMIN/RECRUTEUR, aujourd'hui ou dans le futur |
| Note | Obligatoire lors du traitement ADMIN/RECRUTEUR, entier de 0 à 2147483647 |
| Commentaire | Obligatoire lors du traitement ADMIN/RECRUTEUR, 5 à 10000 caractères |
| Date du dépôt | Générée automatiquement, non modifiable |

Les champs de traitement ne sont pas demandés au candidat ni à la création.
Une ancienne date d'entretien doit être actualisée avant un nouveau traitement.
Les anciennes données restent lisibles ; une modification doit respecter les nouvelles règles.
Le rôle CANDIDAT ne peut pas ajouter ou modifier une offre.

Pour une base existante, exécuter une fois [sql/cv-facultatif.sql](sql/cv-facultatif.sql)
dans offres_db : Hibernate update ne retire pas forcément l'ancienne contrainte NOT NULL.
La base locale a été adaptée ; les anciennes références CV sont conservées.

## Démarrage dans IntelliJ

1. Démarrer MySQL dans XAMPP (3306), RabbitMQ (5672) et `DiscoveryServiceApplication` (8761).
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
| `EUREKA_REGISTER` | `true` |
| `EUREKA_FETCH` | `true` |
| `COMMUN_SERVICE_URL` | `http://localhost:8087` |
| `JWT_SECRET` | obligatoire : même valeur dans les deux services, au moins 32 octets UTF-8 |

Ne pas enregistrer de mot de passe dans Git. Spring Boot ne charge pas
automatiquement un fichier `.env`. Si une configuration IntelliJ ou un ancien
`.env` définit les deux variables Eureka à `false`, les passer à `true` pour
Offres et la Gateway. MySQL reste nécessaire au démarrage normal.

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
La Gateway sur 8080 possède les routes offres et candidatures. Le service peut
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
  pouvoir être conservée en historique. Pour la modifier, actualiser sa date. La validation de la date à la
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
- Toute date saisie doit être aujourd'hui ou ultérieure, y compris pour FERMEE.
  EXPIREE reste un état historique : pour modifier cette offre, choisir un autre statut et actualiser la date.
- Une offre avec candidature ne peut pas être supprimée : réponse 409 ; utiliser
  PUT avec `FERMEE` pour conserver l'historique. La clé étrangère reste une
  protection supplémentaire en cas d'écritures simultanées.
- Codes testés : 200, 201, 204, 400, 401, 403, 404, 409. Les erreurs JSON ont
  la forme `{"status":403,"message":"..."}`, comme dans le service commun.

### Authentification minimale de cette étape

Chaque opération vérifie d'abord la signature, l'expiration, le subject et le
rôle du JWT localement. Elle transmet ensuite le même Bearer à `/skillmatch/auth/me`.
Le service commun retourne l'identifiant et le rôle depuis sa base ; ils ne
sont pas déduits du formulaire, d'un éventuel userId du JWT ou du stockage Angular.
L'email et le rôle retournés doivent correspondre aux claims signés.
Un JWT invalide ou une identité refusée renvoie 401 ; une panne du service commun
renvoie 503. Aucun accès n'est accordé en cas d'échec.

Aucun secret réel n'est enregistré dans Git. La faille existante de
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
  "entrepriseId": 1,
  "testRequisId": 1,
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
le port temporaire de diagnostic. Les routes Gateway sont maintenant présentes ;
les écrans Angular restent à connecter.

### Tests du CRUD

```powershell
mvn test
mvn "-Dtest=OffreCrudIT,EntityPersistenceIT" test
mvn package
```

- `OffreServiceTest` : rôles, propriétaires, dates, brouillon/publication et suppression.
- `OffreControllerTest` : routes HTTP via MockMvc, validation et codes d'erreur.
- `IdentiteServiceTest` : JWT réellement signé et vrai client HTTP vers un
  serveur local simulant commun-service ; vérifie aussi la cohérence des claims.
- `OffreCrudIT` : un seul scénario court avec le vrai service et MySQL : créer,
  lire, modifier et supprimer. L'identité distante est simulée et les données
  sont annulées après le test. Les routes HTTP sont testées dans `OffreControllerTest`.

Les tests `*IT` exigent MySQL et sont lancés explicitement. Aucun compte réel
n'est créé. Les compteurs d'identifiants peuvent avancer malgré le rollback.

Vérifications de cette étape : 49 tests sans MySQL réussis et 5 tests
d'intégration MySQL réussis (3 parcours CRUD et 2 tests du mapping JPA).
Une lecture de contrôle confirme zéro offre et zéro candidature résiduelles
après ces tests. Le login avec les vrais comptes du projet et l'appel au
`commun-service` déployé restent à vérifier manuellement.

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

## CRUD Candidature

Un contrôleur et un service, avec cinq routes :

| Méthode | Route | Utilité |
|---|---|---|
| GET | `/skillmatch/candidatures` | Candidat : les siennes ; recruteur : reçues sur ses offres ; ADMIN : toutes |
| GET | `/skillmatch/candidatures/{id}` | Détail avec le même contrôle d'accès |
| POST | `/skillmatch/candidatures` | Candidat ou ADMIN, offre publiée non expirée |
| PUT | `/skillmatch/candidatures/{id}` | Modification selon le rôle |
| DELETE | `/skillmatch/candidatures/{id}` | Retrait candidat ou suppression physique ADMIN |

Création : `{"offreId":1,"lettre":"Ma lettre de motivation pour ce poste."}`.
Le candidat vient du JWT via le service commun. Pour ADMIN, ajouter `userId` :
son existence et son rôle CANDIDAT sont vérifiés côté serveur.

Le candidat peut modifier `lettre` et `cvId` seulement au statut `RECUE`.
Son DELETE passe au statut `RETIREE`, sans effacer l'historique. Le recruteur
ne peut ni créer ni supprimer : il traite uniquement les candidatures de ses
offres (`PRESELECTIONNEE`, `ENTRETIEN_PLANIFIE`, `ACCEPTEE`, `REFUSEE`),
avec une note, un commentaire et une date d'entretien obligatoires. La date
doit être aujourd'hui ou dans le futur. Une candidature retirée ne peut plus être traitée
par le recruteur. ADMIN peut modifier les données et tous les statuts.

Exemples PUT : candidat `{"lettre":"Je souhaite rejoindre votre équipe Java."}` ;
recruteur `{"statut":"PRESELECTIONNEE","noteRecruteur":8,"commentaireRecruteur":"Profil intéressant","dateEntretien":"2030-12-31T10:00:00"}` (adapter la date).
Un champ absent conserve sa valeur ; les champs candidat/offre et la date du
dépôt ne sont pas modifiables par cette route. La création initialise toujours
`RECUE` et la date courante. Le JSON de réponse contient `offreId`, pas l'entité
Offre complète.

Règle de doublon volontairement simple : une candidature par candidat et offre,
même après retrait. Une contrainte MySQL protège aussi les demandes simultanées.
Le doublon renvoie 409. Une suppression physique par ADMIN libère cette paire.
Le `cvId` reste une référence positive non vérifiée auprès de Profils : aucune
fausse validation de CV, aucun stockage de fichier ajouté.

Les trois petits DTO sont `CandidatureRequest` (création),
`CandidatureUpdate` (modification) et `CandidatureResponse` (réponse JSON).
Pas de notifications ou de matching dans cette étape. Les routes Gateway sont
présentes ; les écrans Angular ne sont pas encore branchés aux CRUD.

Vérification :

```powershell
mvn test
mvn "-Dtest=CandidatureCrudIT,OffreCrudIT,EntityPersistenceIT" test
```

Les tests MySQL annulent leurs données. L'identité distante y est simulée ;
les tests cryptographiques séparés utilisent des JWT réellement signés.

Résultat de cette étape : 59 tests sans MySQL et 5 tests d'intégration MySQL
réussis, JAR généré. La contrainte anti-doublon est présente dans MySQL et
les deux tables sont revenues à zéro ligne après les tests.

## Recherche et tri simples des candidatures

La liste `GET /skillmatch/candidatures` accepte deux paramètres :

- `search` : texte contenu dans la lettre, sans tenir compte de la casse.
- `sort=dateCandidature` : anciennes d'abord.
- `sort=dateCandidatureDesc` : récentes d'abord, valeur par défaut.

Exemple : `/skillmatch/candidatures?search=java&sort=dateCandidatureDesc`.
La petite méthode `rechercherEtTrier` utilise uniquement les candidatures déjà
autorisées par le rôle. Comme pour les offres, la recherche et le tri se font
en Java sur cette liste. Aucun filtre supplémentaire ni nouvelle classe.

## Vérification locale du JWT

`JwtVerifier` utilise JJWT 0.12.5, comme commun-service, et la même clé HMAC
encodée en UTF-8. Il refuse les JWT non signés, expirés, malformés, signés avec
une autre clé, sans email/expiration ou avec un rôle inconnu. `IdentiteService`
récupère ensuite le vrai userId via `/auth/me` ; si l'email ou le rôle a changé,
l'utilisateur doit se reconnecter. Les contrôles de propriétaire et de rôle
des deux CRUD restent dans leurs services.

### Configuration obligatoire pour lancer les deux services

Définir **la même valeur** `JWT_SECRET` dans les configurations IntelliJ de
`CommunServiceApplication` et `OffresServiceApplication` (Environment variables).
Utiliser au moins 32 octets UTF-8. Le service Offres n'a aucun secret par défaut :
une valeur absente ou trop courte empêche son démarrage.
Redémarrer les deux services et se reconnecter pour obtenir un nouveau JWT.
Ne pas utiliser la valeur de développement écrite dans la configuration commune.

Pour générer une valeur locale dans un terminal PowerShell, sans fichier :

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Cette commande ne définit la variable que dans ce terminal et ses processus
enfants. Pour IntelliJ, reporter la même valeur dans les deux configurations ;
ne pas générer deux clés différentes, ne pas les commiter.

### Tests

`mvn test` vérifie les vraies signatures, les trois rôles, l'expiration, les
claims manquants et les erreurs de clé. Les clés de test sont générées en mémoire.
Les tests MySQL `*IT` exigent aussi `JWT_SECRET` dans le terminal, comme un
démarrage normal ; leurs identités distantes restent simulées.

L'inscription publique ADMIN est désormais refusée par `commun-service` (403).
L'inscription reste ouverte aux candidats et recruteurs.

Vérification de cette étape : 68 tests sans MySQL et 5 tests d'intégration MySQL
réussis, JAR généré. Les tests MySQL ont utilisé une clé éphémère dans la variable
d'environnement du processus ; aucune clé réelle n'a été ajoutée aux fichiers.

## Accès par la Gateway

Les mêmes routes sont accessibles via `http://localhost:8080/skillmatch/offres`
et `http://localhost:8080/skillmatch/candidatures`, avec le même Bearer,
les mêmes formulaires, paramètres de recherche et valeurs de tri.
La Gateway transmet la requête ; les contrôles JWT et métier restent dans
offres-service. Démarrer aussi commun-service pour les requêtes authentifiées.

Si Offres tourne temporairement sur 18081, définir
`OFFRES_SERVICE_URL=http://localhost:18081` dans la configuration de la Gateway.
L'interface Angular des offres candidat et recruteur utilise ces routes.
Voir [le guide frontend](../../Frontend/README.md) pour les écrans et leur démarrage.

L'interface des candidatures est aussi connectée : dépôt et suivi candidat,
traitement recruteur, ainsi que les deux CRUD ADMIN dans le back-office existant.
Les contrôles métier restent dans ce service ; aucun nouveau backend ADMIN.
Le formulaire ne demande plus de CV : la lettre suffit. `cvId` est nullable et
facultatif dans l’API pour la future intégration Profils ; les références existantes sont conservées.

## Améliorations des écrans

Les vues Offres et Candidatures sont séparées par la navigation Angular.
La liste des candidatures transmet aussi `offreTitre`, lu depuis la relation
existante, pour éviter les identifiants techniques sur les cartes.
Les règles de doublon restent protégées par le service et la contrainte MySQL ;
Angular masque désormais Postuler dès qu'une candidature existe, même retirée.
Fermer une offre conserve l'historique et bloque les nouvelles candidatures.
La description est limitée à 10000 caractères dans le DTO et le formulaire.

## Contrôles de saisie

Les formulaires d'ajout et modification partagent les contrôles numériques et
de dates, avec messages sous les champs selon le rôle. Les annotations des DTO
et les services restent les contrôles de référence côté API.
`spring.jackson.deserialization.accept-float-as-int=false` refuse un CV, une
référence ou une note décimale (400), au lieu de convertir silencieusement 1.5 en 1.
Le salaire reste un nombre décimal autorisé, positif ou nul.
Les tests `CandidatureControllerTest` couvrent les saisies HTTP incorrectes ;
`CandidatureCrudIT` vérifie aussi cette configuration dans le vrai contexte Spring
avec transaction MySQL annulée après le test. Les tests de service vérifient les
droits et la lettre vide pour CANDIDAT/ADMIN, ainsi que le traitement RECRUTEUR/ADMIN.
