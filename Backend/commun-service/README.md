# commun-service

Microservice Spring Boot commun de SkillMatch. Cette version contient uniquement **l'utilisateur et l'authentification JWT**, sans notifications.

## Responsabilités

- Table `utilisateurs` dans la base MySQL `commun_db`.
- Inscription d'un utilisateur avec rôle `CANDIDAT`, `RECRUTEUR` ou `ADMIN`.
- Connexion avec mot de passe haché BCrypt.
- Génération d'un JWT signé avec `jjwt`.

## Port et configuration

- Port : `8087`
- Base : `commun_db`
- Nom Spring : `commun-service`
- Configuration : `src/main/resources/application.yml`

Variables d'environnement possibles :

- `SERVER_PORT`
- `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION_MS`
- `EUREKA_URL`, `EUREKA_REGISTER`, `EUREKA_FETCH`

La base `commun_db` est créée automatiquement si l'utilisateur MySQL a le droit `CREATE`, puis Hibernate crée la table `utilisateurs` avec `ddl-auto: update`.

Les erreurs JSON ont la forme `{"status":409,"message":"Cet email est déjà utilisé"}`.

## Endpoints

| Méthode | Route | Description |
|---|---|---|
| `POST` | `/skillmatch/auth/register` | Créer un compte et retourner un JWT |
| `POST` | `/skillmatch/auth/login` | Se connecter et retourner un JWT |
| `GET` | `/skillmatch/auth/me` | Récupérer l'utilisateur à partir du JWT |

Exemple d'inscription :

```bash
curl -i -X POST http://localhost:8087/skillmatch/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"nom":"Skander","email":"skander@esprit.tn","password":"123456","role":"ADMIN"}'
```

Exemple de connexion :

```bash
curl -i -X POST http://localhost:8087/skillmatch/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"skander@esprit.tn","password":"123456"}'
```

Réponse attendue :

```json
{
  "token": "...",
  "user": {
    "id": 1,
    "nom": "Skander",
    "email": "skander@esprit.tn",
    "role": "ADMIN"
  }
}
```

## Démarrage

```bash
cd Backend/commun-service
mvn spring-boot:run
```

Le service doit être appelé par le frontend via l'API Gateway sur `http://localhost:8080`, pas directement sur `8087`.
