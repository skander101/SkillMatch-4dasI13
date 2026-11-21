# Frontend SkillMatch

Interface web Angular de SkillMatch pour les candidats, les recruteurs et les administrateurs.

Le template Angular 21 choisi par l'équipe est intégré directement dans ce dossier. Le thème vert foncé/blanc, la recherche d'offres, les favoris et les modales ont été conservés. Un back office Angular est également intégré : il s'ouvre automatiquement après la connexion d'un utilisateur `ADMIN`. Le backend [`entreprises-service`](../Backend/entreprises-service/README.md) fournit déjà le CRUD des entreprises et Swagger, mais le frontend n'y est pas encore connecté.

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
- Entreprises & avis : backend CRUD et Swagger disponibles sur `http://localhost:8083`, mais affichage public, annuaire, modération et publication d'avis encore à brancher dans le frontend (commits 6 et 7 du plan).

Les offres affichées restent fictives tant que `GET /skillmatch/offres/all` n'est pas branché.

## Aperçu statique

Le dossier `preview/` contient `apercu.html`, une version statique non interactive du template. Elle ne remplace pas `npm start`.
