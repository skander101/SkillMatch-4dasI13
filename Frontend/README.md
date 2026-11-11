# Frontend

Interface web unique de SkillMatch pour les candidats, les recruteurs et les administrateurs.

Ce dossier est un emplacement réservé. Le responsable du layout commun et du login y ajoutera l'application sur sa branche `feature/frontend-common`.

## Stack prévue

- React
- Vite
- TypeScript
- React Router
- Client HTTP vers l'API Gateway

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

## Démarrage prévu

```bash
npm install
npm run dev
```

Le serveur de développement utilise le port `5173`.

## Variables d'environnement

Créer un fichier `.env.local` non commité :

```text
VITE_API_URL=http://localhost:8080
```
