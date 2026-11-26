# Frontend SkillMatch

Interface web Angular de SkillMatch pour les candidats, les recruteurs et les administrateurs.

Le template Angular 21 choisi par l'équipe est intégré directement dans ce dossier. Le thème vert foncé/blanc, la recherche d'offres, les favoris et les modales ont été conservés. Un back office Angular est également intégré : il s'ouvre automatiquement après la connexion d'un utilisateur `ADMIN`. Le back office est connecté au service commun pour les utilisateurs et à [`entreprises-service`](../Backend/entreprises-service/README.md) pour le CRUD des entreprises et la modération des avis.

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
│   │   ├── backoffice.ts       panneau admin, CRUD utilisateurs/entreprises et modération
│   │   ├── backoffice.html     interface du back office
│   │   ├── backoffice.css      thème du back office
│   │   └── nav.data.ts         navigation et notifications de démonstration
│   ├── entreprises/
│   │   ├── entreprise.model.ts Entreprise, Avis et requêtes associées
│   │   └── entreprise.service.ts appels HTTP entreprises et avis via la gateway
│   ├── shared/
│   │   └── confirm-dialog/     modale de confirmation réutilisable et thémée
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

Rôles disponibles : `CANDIDAT`, `RECRUTEUR`, `ADMIN`.

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

Le bouton « Postuler » exige une connexion. Le dépôt effectif de candidature reste à brancher sur `POST /skillmatch/candidatures/add`.

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

La rubrique « Entreprises » affiche les entreprises renvoyées par l'API et permet de créer, modifier et supprimer une fiche. La suppression passe par la modale de confirmation partagée `shared/confirm-dialog`.

| Action | Endpoint | Méthode |
|---|---|---|
| Lister | `/skillmatch/entreprises` | `GET` |
| Récupérer | `/skillmatch/entreprises/{id}` | `GET` |
| Créer | `/skillmatch/entreprises` | `POST` |
| Modifier | `/skillmatch/entreprises/{id}` | `PUT` |
| Supprimer | `/skillmatch/entreprises/{id}` | `DELETE` |

Le corps de création/modification contient `nom`, `secteur`, `ville` et `description`. La réponse contient aussi `id` et `noteMoyenne`.

La rubrique « Avis à modérer » liste les avis `EN_ATTENTE` de toutes les entreprises (un `GET /skillmatch/entreprises/{id}/avis` par entreprise) et propose Approuver / Rejeter. Après chaque décision, la liste des avis et celle des entreprises sont rafraîchies pour afficher la `noteMoyenne` recalculée.

| Action | Endpoint | Méthode |
|---|---|---|
| Avis d'une entreprise | `/skillmatch/entreprises/{id}/avis` | `GET` |
| Modérer un avis | `/skillmatch/avis/{id}/moderation` | `PUT` |
| Supprimer un avis | `/skillmatch/avis/{id}` | `DELETE` |

Le corps de modération est `{"statut":"APPROUVE"}` ou `{"statut":"REJETE"}`. Le service expose aussi `POST /skillmatch/entreprises/{id}/avis` avec `{"note":5,"commentaire":"..."}` pour la publication, qui sera utilisée par l'interface publique au commit 7.

Pour essayer le panneau :

```bash
curl -X POST http://localhost:8080/skillmatch/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"nom":"Admin BO","email":"admin.bo.test@skillmatch.tn","password":"123456","role":"ADMIN"}'
```

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

- Recherche par compétence, domaine et ville : fonctionnelle sur les données fictives.
- Liste des offres, favoris et détails d'offre : démonstration locale.
- Authentification et inscription : connectées à l'API.
- Profils, tests, alertes et messagerie : à brancher progressivement sur les microservices.
- Entreprises & avis : CRUD entreprises et modération des avis connectés dans le back office via la gateway sur `http://localhost:8080`. L'annuaire public, l'affichage des avis approuvés et la publication d'un avis depuis l'UI restent à brancher dans le frontend (commit 7 du plan).

Les offres affichées restent fictives tant que `GET /skillmatch/offres/all` n'est pas branché.

## Aperçu statique

Le dossier `preview/` contient `apercu.html`, une version statique non interactive du template. Elle ne remplace pas `npm start`.
