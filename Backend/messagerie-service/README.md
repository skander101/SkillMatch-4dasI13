# Messagerie Service — SkillMatch

## État actuel

Microservice indépendant de conversations et de messages entre utilisateurs SkillMatch. Le périmètre de ce service est limité à `Backend/messagerie-service` et au composant Angular `Frontend/src/app/messagerie/`.

- Java 17, Spring Boot 3.3.5, Maven.
- MySQL/XAMPP : base `messagerie_db`, port 3306.
- Port HTTP : 8086.
- Nom Spring : `messagerie-service`.
- Package racine : `tn.esprit.skillmatch.messagerie`.
- Préfixe API : `/skillmatch/messagerie`.
- Événement RabbitMQ : `MessageSent` (publication best-effort).
- Aucun appel synchrone à un autre microservice pour vérifier un destinataire.
- Seul appel interservice conservé : `GET /skillmatch/auth/me` de commun-service en secours pour obtenir l'identifiant numérique lorsque le JWT ne contient ni `id` ni `userId`.

## Informations

| Champ | Valeur |
|---|---|
| Responsable | Responsable du microservice Messagerie |
| Branche | `feature/messagerie-service` (à vérifier dans Git local) |
| Package | `tn.esprit.skillmatch.messagerie` |
| Port | `8086` |
| Base | `messagerie_db` |
| Nom Spring | `messagerie-service` |
| Préfixe API | `/skillmatch/messagerie` |

## Architecture

Couches séparées : `controller`, `service`, `repository`, `entity`, `dto`, `config`, `exception`.

Les identifiants des participants sont stockés en JSON dans la colonne `participant_ids`. Aucune clé étrangère n'est créée vers une autre base ou un autre microservice. Les listes de participants sont persistées comme JSON.

## Entités

- **Conversation** : identifiant, liste JSON des identifiants participants, identifiant du créateur, date du dernier message.
- **Message** : identifiant, conversation, expéditeur, contenu, date et état lu.

## Endpoints

Toutes les routes de messagerie nécessitent `Authorization: Bearer <JWT>`.

| Méthode | Endpoint | Description | Réponse attendue |
|---|---|---|---|
| POST | `/skillmatch/messagerie/conversations` | Créer une conversation | 201 |
| GET | `/skillmatch/messagerie/conversations` | Lister les conversations accessibles | 200 |
| GET | `/skillmatch/messagerie/conversations/{id}` | Consulter une conversation | 200 |
| PUT | `/skillmatch/messagerie/conversations/{id}` | Modifier les participants (créateur/ADMIN) | 200 |
| DELETE | `/skillmatch/messagerie/conversations/{id}` | Supprimer conversation et messages | 204 |
| GET | `/skillmatch/messagerie/conversations/{id}/messages` | Consulter l'historique | 200 |
| POST | `/skillmatch/messagerie/conversations/{id}/messages` | Envoyer un message | 201 |
| GET | `/skillmatch/messagerie/messages/{id}` | Consulter un message | 200 |
| PUT | `/skillmatch/messagerie/messages/{id}` | Modifier son propre message | 200 |
| DELETE | `/skillmatch/messagerie/messages/{id}` | Supprimer son propre message | 204 |
| PATCH | `/skillmatch/messagerie/messages/{id}/lu` | Marquer un message reçu comme lu | 200 |

## Règles de sécurité et métier

- Le JWT est vérifié localement : signature HMAC et date d'expiration obligatoire.
- Si `userId` ou `id` existe dans le JWT, cet identifiant est utilisé directement.
- Sinon, le fallback `GET /skillmatch/auth/me` résout l'identifiant et met le résultat en cache mémoire 10 minutes. Ce fallback doit être retiré lorsque commun-service ajoutera l'identifiant numérique dans le JWT.
- Un JWT absent, invalide ou expiré retourne 401.
- Si commun-service est injoignable pendant le fallback, le service retourne 503.
- Seuls les participants peuvent lire une conversation et son historique (403 sinon).
- Le créateur ou ADMIN peut gérer les participants et supprimer une conversation.
- Seul l'auteur peut modifier ou supprimer son message.
- Les identifiants participants doivent être positifs, distincts, et inclure le créateur.
- Le backend renvoie les placeholders `Utilisateur #id`. Le frontend résout les noms via l'annuaire de la Gateway.
- Aucun appel synchrone de Messagerie vers l'annuaire n'est effectué.

## Événement RabbitMQ `MessageSent`

Après l'enregistrement du message, le service tente de publier un événement pour chaque destinataire (hors expéditeur).

- Exchange : `skillmatch.events`
- Queue : `skillmatch.messagerie.message-sent`
- Routing key : `messagerie.message.sent`
- Champs : `messageId`, `conversationId`, `senderId`, `recipientId`, `contenu`, `date`

La publication est asynchrone et best-effort : une indisponibilité RabbitMQ est journalisée et ne doit pas annuler l'enregistrement du message ni provoquer une réponse 500.

## Documentation Swagger

- Swagger UI : `http://localhost:8086/swagger-ui.html`
- OpenAPI : `http://localhost:8086/v3/api-docs`
- Santé : `http://localhost:8086/actuator/health`

## Exemples curl

Créer une conversation :

```bash
curl -X POST http://localhost:8086/skillmatch/messagerie/conversations \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"participantIds":[1,2]}'
```

Lister les conversations :

```bash
curl http://localhost:8086/skillmatch/messagerie/conversations \
  -H "Authorization: Bearer $TOKEN"
```

Envoyer un message :

```bash
curl -X POST http://localhost:8086/skillmatch/messagerie/conversations/CONVERSATION_ID/messages \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"contenu":"Bonjour"}'
```

## Démarrage PowerShell

1. Démarrer MySQL depuis XAMPP.
2. Démarrer RabbitMQ (facultatif pour le CRUD, nécessaire pour vérifier l'événement) :
   `docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management`
3. Démarrer commun-service sur 8087.
4. Dans ce dossier, fournir le secret partagé sans le commiter :
   `$env:JWT_SECRET = "<même valeur que commun-service>"`
5. Lancer le service :
   `mvn spring-boot:run` (ou `mvnw.cmd` si le wrapper est présent).

## Variables d'environnement

| Variable | Défaut local | Description |
|---|---|---|
| `SERVER_PORT` | `8086` | Port HTTP |
| `MYSQL_HOST` | `localhost` | Hôte MySQL |
| `MYSQL_PORT` | `3306` | Port MySQL |
| `MYSQL_DATABASE` | `messagerie_db` | Base dédiée |
| `MYSQL_USER` | `root` | Utilisateur MySQL |
| `MYSQL_PASSWORD` | vide | Mot de passe MySQL |
| `RABBITMQ_HOST` | `localhost` | Hôte RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Port RabbitMQ |
| `RABBITMQ_USER` | `guest` | Utilisateur RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Mot de passe RabbitMQ |
| `JWT_SECRET` | **obligatoire, sans défaut** | Secret HMAC partagé avec commun-service |
| `COMMUN_SERVICE_URL` | `http://localhost:8087` | URL du fallback `/skillmatch/auth/me` |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | URL Eureka |
| `EUREKA_REGISTER` | `false` | Enregistrement Eureka |
| `EUREKA_FETCH` | `false` | Lecture du registre Eureka |

Ne jamais commiter `.env`, JWT_SECRET, mots de passe ou tokens. Spring Boot ne charge pas automatiquement un fichier `.env`.
