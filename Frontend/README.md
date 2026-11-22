# Frontend SkillMatch

Interface web Angular de SkillMatch pour les candidats, les recruteurs et les administrateurs.

Le template Angular 21 choisi par l'équipe est intégré directement dans ce dossier. Le thème vert foncé/blanc, les cartes et les modales sont réutilisés. Un back office Angular s'ouvre automatiquement après la connexion d'un utilisateur `ADMIN`.

## Prérequis

- Node.js `20.19+`, `22.12+` ou `24+`
- npm
- API Gateway démarrée sur `http://localhost:8080`

## Démarrage

```bash
cd Frontend
npm install
npm start
```

`npm start` lance `ng serve` sur `http://localhost:4200`.

Pour un build de production :

```bash
npm run build
```

## Stack

- Angular 21
- TypeScript
- Angular Signals
- `HttpClient` pour les appels vers l'API Gateway
- FormsModule pour les formulaires utilisateur

## Structure

```text
src/
├── app/
│   ├── app.ts                  logique de la page, auth et bascule back office
│   ├── app.html                interface publique + conteneur back office
│   ├── app.css                 thème vert foncé et responsive
│   ├── backoffice/
│   │   ├── backoffice.ts       panneau admin et CRUD utilisateurs
│   │   ├── backoffice.html     interface du back office
│   │   ├── backoffice.css      thème du back office
│   │   └── nav.data.ts         navigation et notifications de démonstration
│   └── user/
│       ├── auth.service.ts     login, register, logout et session
│       ├── auth.interceptor.ts ajout automatique du JWT
│       ├── user.model.ts       User, Role et requêtes
│       └── user.service.ts     CRUD utilisateurs via l'API Gateway
├── environments/
│   ├── environment.ts
│   └── environment.development.ts
├── index.html
├── main.ts
└── styles.css
```

## Fonctionnalité utilisateur

La gestion des utilisateurs est connectée au microservice commun via l'API Gateway. Le backend correspondant est implémenté dans [`Backend/commun-service/`](../Backend/commun-service/README.md) et routé par [`Backend/api-gateway/`](../Backend/api-gateway/README.md).

| Action | Endpoint | Méthode |
|---|---|---|
| Connexion | `/skillmatch/auth/login` | `POST` |
| Inscription | `/skillmatch/auth/register` | `POST` |

Inscription publique : `CANDIDAT` ou `RECRUTEUR`. Les comptes `ADMIN` existants
se connectent normalement ; l'inscription ADMIN est refusée côté backend (403).

Le formulaire d'inscription envoie :

```json
{
  "nom": "Nom complet",
  "email": "vous@exemple.com",
  "password": "motDePasse",
  "role": "CANDIDAT"
}
```

Le formulaire de connexion envoie :

```json
{
  "email": "vous@exemple.com",
  "password": "motDePasse"
}
```

Réponse attendue :

```json
{
  "token": "<jwt>",
  "user": {
    "id": 1,
    "nom": "Nom complet",
    "email": "vous@exemple.com",
    "role": "CANDIDAT"
  }
}
```

Le JWT est stocké dans `localStorage` sous `skillmatch.jwt` et l'utilisateur sous `skillmatch.user`.

L'intercepteur `auth.interceptor.ts` ajoute automatiquement l'en-tête :

```text
Authorization: Bearer <jwt>
```

L'en-tête affiche le nom et le rôle de l'utilisateur connecté, avec un bouton de déconnexion. Les boutons « Se connecter » et « S'inscrire » sont affichés uniquement lorsque aucun utilisateur n'est connecté.

Le bouton « Postuler » dépose une candidature sur `POST /skillmatch/candidatures`.

## Offres candidat et recruteur

Démarrer MySQL, discovery-service (Eureka sur 8761), commun-service,
offres-service et la Gateway avant Angular.
Pour publier les événements, démarrer aussi RabbitMQ avant Offres :
[guide RabbitMQ](../Backend/offres-service/RABBITMQ.md).
Les services Commun et Offres doivent utiliser la même variable `JWT_SECRET`.
La configuration et les ports sont détaillés dans [offres-service](../Backend/offres-service/README.md).

- CANDIDAT : consulter les offres publiées non expirées et leurs détails.
- RECRUTEUR : consulter ses offres, créer un brouillon, modifier, publier, fermer et supprimer après confirmation.
- Recherche simple par titre ; tri par salaire ou expiration, dans les deux sens.
- Les erreurs, listes vides et chargements sont affichés ; le JWT existant accompagne chaque requête.

Le dossier `src/app/offres/` contient le modèle (`offre.model.ts`), les appels HTTP
(`offre.service.ts`), la liste et les actions (`offres.ts`, `offres.html`),
le formulaire (`offre-form.ts`, `offre-form.html`) et le style (`offres.css`).
Les composants réutilisent aussi `app.css`.

Les appels utilisent `/skillmatch/offres` et `/skillmatch/offres/{id}` ; la liste
du recruteur utilise `/skillmatch/offres/mes-offres`. Les paramètres sont `search`
et `sort` affichés : `salaire`, `salaireDesc`, `dateExpiration`,
`dateExpirationDesc`. Le serveur vérifie les rôles et la propriété des offres.
ADMIN réutilise ce composant dans le back-office et choisit un recruteur réel à
la création. Le propriétaire d'une offre existante reste inchangé.

## Candidatures et administration

- CANDIDAT : « Postuler » depuis une offre ouverte, consulter ses candidatures,
  modifier sa lettre et retirer une candidature au statut `RECUE`.
- RECRUTEUR : consulter les candidatures de ses offres et modifier le statut,
  la date d'entretien, la note et le commentaire. Aucune suppression recruteur.
- ADMIN : rubriques « Toutes les offres » et « Toutes les candidatures » dans
  le back-office ; création, consultation, modification et suppression réelles.
  Pour créer une candidature, choisir une offre ouverte et un candidat existant.
- Recherche dans la lettre et tri par date de candidature dans les deux sens.

Le dossier `src/app/candidatures/` contient le modèle, le service HTTP, la liste,
le formulaire et leur présentation. Les styles et le login existants sont réutilisés.
Le menu Offres & candidatures ouvre une seule vue à la fois : `#offres` ou
`#candidatures`. Le fragment conserve le choix après actualisation et permet
les boutons Précédent/Suivant du navigateur. Après connexion, le candidat arrive
sur les offres et le recruteur sur ses offres. ADMIN reste dans le back-office,
avec une déconnexion et sans bouton Retour au site.

Les cartes ont un bandeau vert dégradé et des icônes. Les identifiants techniques
sont masqués : les candidatures affichent le titre réel de l'offre. Les offres
fermées/expirées et les candidatures indisponibles prennent un fond gris.
Avant d'afficher Postuler, la liste charge les candidatures du candidat ; le
bouton disparaît dès qu'un dépôt existe, même après retrait ou actualisation.
Le refus des doublons et la contrainte MySQL restent actifs côté backend.

Le formulaire Offre affiche les erreurs sous les champs : titre obligatoire
sans espaces seuls, salaire positif ou nul, expiration obligatoire et cohérente
avec le statut, références numériques entières positives. Les champs description,
compétences et localisation restent facultatifs selon le contrat backend existant.
Fermer conserve l'offre et ses candidatures et arrête les nouveaux dépôts.

Vérification de cette révision : build Angular réussi ; `mvn package` réussi
pour Offres (68 tests) et Commun (4 tests, dont 3 sur l'inscription). Les essais
HTTP/MySQL confirment les quatre tris, les validations, le titre des candidatures,
le refus des doublons avant/après retrait et l'inscription ADMIN refusée en 403.
Les essais ont utilisé des ports séparés et des données temporaires nettoyées.
Le navigateur de vérification n'a pas pu ouvrir sa fenêtre : le rendu et les
interactions de navigation restent à contrôler visuellement dans votre navigateur.
Redémarrer Commun et Offres pour charger les nouveaux contrôles et le titre.

Les appels utilisent `/skillmatch/candidatures` (`GET`, `POST`) et
`/skillmatch/candidatures/{id}` (`PUT`, `DELETE`). Le backend fixe le périmètre
selon le JWT : le navigateur ne choisit pas l'identité du candidat connecté.
`search` cherche dans la lettre ; `sort` vaut `dateCandidature` ou
`dateCandidatureDesc` (récentes d'abord par défaut).

Le dépôt nécessite seulement la lettre de motivation : aucun champ CV, aucun
envoi de fichier ni connexion à Profils. cvId reste facultatif pour une future intégration. Un retrait conserve la candidature et interdit
un nouveau dépôt sur la même offre. Une suppression ADMIN est définitive.
Une offre ayant des candidatures doit être fermée ; sa suppression est refusée.
Les champs de traitement sont obligatoires dans le formulaire ADMIN/RECRUTEUR.
Le tableau de bord et les autres modules de démonstration du collègue restent
distincts des rubriques Offres/Candidatures connectées.

### Contrôles des formulaires (ajout et modification)

- Offre RECRUTEUR/ADMIN : titre de 3 à 255 caractères, description de 20 à 10000,
  compétences, localisation, contrat, salaire et références obligatoires ; date actuelle ou future.
- Candidature CANDIDAT/ADMIN : lettre obligatoire de 20 à 10000 caractères,
  sans CV ; choix de l'offre ouverte et du candidat pour l'ajout ADMIN.
- Traitement RECRUTEUR/ADMIN : statut, date actuelle ou future, note entière positive
  ou nulle et commentaire de 5 à 10000 caractères obligatoires.

Les champs invalides sont signalés après avoir quitté le champ ou cliqué sur
Enregistrer ; aucune requête d'enregistrement ne part tant que le formulaire
est invalide. Le validateur `saisie-valide.ts` est partagé par les deux formulaires.
Les champs restent limités au rôle : le candidat ne traite pas une candidature,
le recruteur ne modifie pas sa lettre/CV et ne crée pas de candidature.
Le backend maintient les mêmes validations et refuse aussi les nombres décimaux
pour les identifiants et les notes, plutôt que de les tronquer.

Commandes : `npm run test:forms` (5 tests du validateur), puis `npm run build`.
Ces tests vérifient les nombres/dates et la saisie native invalide ; ils ne
remplacent pas une vérification visuelle des messages dans le navigateur.

### Vérification de cette étape

`npm run build` réussit. Avec MySQL et les trois services locaux, le navigateur
a vérifié la connexion, la création, la modification, la publication, les détails,
la recherche, le tri décroissant et l'annulation de suppression. Le candidat
voit l'offre publiée sans boutons de gestion. Les six tris, la fermeture et la
suppression ont aussi été vérifiés par HTTP via la Gateway. Les deux comptes et
les deux offres temporaires ont été supprimés après le test.
`npm run test:forms` vérifie le validateur Angular partagé avec le lanceur Node,
sans dépendance supplémentaire. Les interactions de navigateur ne sont pas automatisées.

### Vérification candidatures et ADMIN

Avec trois comptes locaux temporaires et MySQL, le navigateur a vérifié : dépôt
candidat, mise à jour de la lettre, erreur de doublon, traitement recruteur avec
entretien/note/commentaire, création et publication d'offre ADMIN, création et
modification de candidature ADMIN, recherche et tri, confirmations et annulations.
La rubrique Utilisateurs existante reste accessible. Par HTTP : retrait logique,
suppression définitive ADMIN (puis lecture 404), refus de création recruteur (403),
protection d'offre liée et de candidature traitée (409), les deux tris par date.
Les données et les trois comptes de test ont été nettoyés. Aucun secret n'est commité.

Pour refaire une démonstration : créer/publier une offre avec RECRUTEUR, se
connecter avec CANDIDAT et postuler, puis revenir au RECRUTEUR pour la traiter.
Avec un compte ADMIN existant, ouvrir « Offres & Candidatures » dans le menu
latéral du back-office. La suppression définitive des candidatures est réservée
à ADMIN ; le candidat utilise le retrait.

## Back office ADMIN

Après `POST /skillmatch/auth/login` avec un compte `ADMIN`, le composant `BackOffice` remplace la page publique. Le bouton « Back office » dans l'en-tête permet d'y revenir après l'avoir quitté. Le bouton « Retour au site » du panneau republie l'interface publique sans déconnecter l'utilisateur.

La rubrique « Utilisateurs & rôles » est connectée au service commun :

| Action | Endpoint | Méthode |
|---|---|---|
| Lister | `/skillmatch/utilisateurs` | `GET` |
| Créer | `/skillmatch/utilisateurs` | `POST` |
| Modifier | `/skillmatch/utilisateurs/{id}` | `PUT` |
| Supprimer | `/skillmatch/utilisateurs/{id}` | `DELETE` |

Toutes ces requêtes passent par l'API Gateway et portent l'en-tête `Authorization: Bearer <jwt>` ajouté par l'intercepteur. Le backend vérifie le rôle `ADMIN` et répond `401` ou `403` si nécessaire.

Pour essayer le panneau, utiliser un compte ADMIN provisionné dans la base
avec un mot de passe BCrypt. L'inscription publique ne permet plus de créer ADMIN.

## Configuration de l'API

L'URL de l'API Gateway est définie dans `src/environments/environment.development.ts` :

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

En développement, l'API Gateway autorise toutes les origines (`allowedOriginPatterns: "*"`). Si le port `8080` est déjà occupé, arrêter le processus concerné ou adapter `environment.development.ts`.

## État du template

- Recherche par titre, tri, liste et détails des offres : connectés à l'API.
- CRUD des offres recruteur : connecté à l'API.
- Authentification et inscription : connectées à l'API.
- Profils, tests, alertes, entreprises et messagerie : à brancher progressivement sur les microservices.

Les offres affichées proviennent de MySQL via la Gateway ; les anciennes cartes fictives ont été retirées.

## Aperçu statique

Le dossier `preview/` contient `apercu.html`, une version statique non interactive du template. Elle ne remplace pas `npm start`.
