# Frontend

Interface web unique de SkillMatch pour les candidats, les recruteurs et les administrateurs.

Ce dossier est un emplacement réservé. Le responsable du layout commun et du login y ajoutera l'application sur sa branche `feature/frontend-common`.

## Stack prévue

- Angular
- TypeScript
- Angular Router
- `HttpClient` pour les appels vers l'API Gateway

## Responsabilités

- Layout commun : navigation, en-tête, menu selon le rôle.
- Page de connexion et d'inscription.
- Stockage du JWT et ajout du token dans les requêtes.
- Redirection selon le rôle : `CANDIDAT`, `RECRUTEUR`, `ADMIN`.
- Pages métier ajoutées progressivement par les responsables des services.

## Communication avec le backend

Toutes les requêtes passent par l'API Gateway :

```text
http://localhost:8080
```

Le frontend ne contacte jamais directement un microservice.

L'API Gateway doit autoriser l'origine `http://localhost:4200` en CORS.

## Démarrage prévu

```bash
npm install
npm start
```

`npm start` exécute `ng serve`. Le serveur de développement utilise le port `4200`.

## Configuration de l'API

L'URL de l'API Gateway est définie dans `src/environments/environment.development.ts` :

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```
