# profils-service

Microservice Spring Boot « Profils & CV » de SkillMatch. Il gère les profils des candidats, leurs CV PDF, les compétences, expériences et badges, ainsi que le pourcentage de complétude calculé à la volée.

## Identité

| Élément | Valeur |
|---|---|
| Port | 8082 |
| Base MySQL (XAMPP) | `profils_db` |
| Package racine | `tn.esprit.skillmatch.profils` |
| Événement consommé | `TestPassed` (ajout d'un badge) |
| Événement publié | `ProfileUpdated` |

## Démarrage

Créer la base `profils_db` dans phpMyAdmin (ou laisser Hibernate la créer au premier démarrage), puis :

```bash
cd Backend/profils-service
mvn spring-boot:run
```

Variables d'environnement utiles : `SERVER_PORT`, `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_DATABASE`, `JWT_SECRET`, `COMMUN_SERVICE_URL` (par défaut `http://localhost:8080`, le gateway), `RABBITMQ_HOST`, `RABBITMQ_PORT`, `EUREKA_URL`, `EUREKA_REGISTER`, `EUREKA_FETCH`, `MESSAGING_ENABLED`, `PROFILS_CV_DIRECTORY`, `CV_MAX_SIZE_BYTES`. Aucun secret n'est commité.

Le JWT émis par `commun-service` contient `email` et `role`. Le profil.service résout l'`userId` en appelant `GET /skillmatch/auth/me` via le gateway ; si le token contient déjà un claim `userId`, aucun appel n'est nécessaire.

Pour désactiver RabbitMQ pendant un test local : `MESSAGING_ENABLED=false`.

## Contrat REST

Toutes les routes sont protégées par un JWT signé avec la même clé que `commun-service`. Le JWT doit porter `userId`, `email` et `role`.

| Méthode | Route | Accès | Codes |
|---|---|---|---|
| `POST` | `/skillmatch/profiles` | CANDIDAT | 201, 400, 409 |
| `GET` | `/skillmatch/profiles/me` | CANDIDAT | 200, 401, 404 |
| `PUT` | `/skillmatch/profiles/me` | CANDIDAT | 200, 400, 401, 404 |
| `GET` | `/skillmatch/profiles/me/completeness` | CANDIDAT | 200, 401, 404 |
| `POST` | `/skillmatch/profiles/me/cv` | CANDIDAT | 201, 400, 401, 413 |
| `GET` | `/skillmatch/profiles/me/cv` | CANDIDAT | 200, 401 |
| `DELETE` | `/skillmatch/profiles/me/cv/{cvId}` | CANDIDAT | 204, 401, 403, 404 |
| `GET` | `/skillmatch/profiles/{userId}` | RECRUTEUR, interne | 200, 401, 403, 404 |
| `GET` | `/skillmatch/profiles/{userId}/cv/{cvId}` | interne | 200, 401, 404 |

`POST /skillmatch/profiles` accepte un corps JSON :

```json
{
  "userId": 12,
  "titre": "Développeur Java",
  "localisation": "Tunis",
  "competences": ["Java", "SQL"],
  "experiences": ["Stage chez Example"],
  "badges": []
}
```

`POST /skillmatch/profiles/me/cv` accepte `multipart/form-data` avec un champ `file` PDF de 5 Mo maximum. Les fichiers sont stockés dans `PROFILS_CV_DIRECTORY` et seul leur chemin est conservé en base.

La complétude est calculée : titre 20 %, localisation 20 %, compétences 25 %, expériences 20 %, badges 15 %. Elle n'est jamais stockée.

## Événements RabbitMQ

Exchange topic `skillmatch.events`, une file par consommateur, enveloppe `{eventId, type, occurredAt, source, data}`.

- Consommé : `TestPassed` sur `profils.test-passed` (clé `test.passed`). Le badge est ajouté une seule fois : l'`eventId` est mémorisé en mémoire pour garantir l'idempotence pendant l'exécution du service.
- Publié : `ProfileUpdated` sur la clé `profile.updated`, avec `userId`, `completude` et les compétences.

## Appels synchrones

`offres-service` récupère `GET /skillmatch/profiles/{userId}` pour calculer le score de recommandation (60 % compétences, 30 % badges, 10 % localisation). Aucun accès direct à `profils_db` n'est autorisé.

## Tests

```bash
mvn test
```

Les tests unitaires couvrent le calcul de complétude, l'ajout de badge, le refus de doublon, la sérialisation JSON et la configuration RabbitMQ. Pour tester le flux asynchrone, démarrer RabbitMQ et publier un `TestPassed` dans `skillmatch.events` avec la clé `test.passed`.

## Test manuel rapide

Le service est démarré sur `http://localhost:8082`. En l'absence de gateway authentifié, générer un JWT de test avec la même clé que `JWT_SECRET` :

```powershell
$secret = 'skillmatch-local-development-secret-key-change-me'
function B64($s) { [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($s)).TrimEnd('=').Replace('+','-').Replace('/','_') }
$data = "$(B64 '{"alg":"HS256"}').$(B64 '{"sub":"test@skillmatch.tn","role":"CANDIDAT","userId":42,"exp":1895628000}')"
$hmac = [Security.Cryptography.HMACSHA256]::new()
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($secret)
$token = "$data.$([Convert]::ToBase64String($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($data))).TrimEnd('=').Replace('+','-').Replace('/','_'))"
$h = @{ Authorization = "Bearer $token" }
$body = @{ userId = 42; titre = 'Dev'; localisation = 'Tunis'; competences = @('Java','SQL'); experiences = @('Stage'); badges = @() } | ConvertTo-Json -Compress
Invoke-RestMethod -Uri 'http://localhost:8082/skillmatch/profiles' -Method Post -Headers $h -Body $body -ContentType 'application/json'
Invoke-RestMethod -Uri 'http://localhost:8082/skillmatch/profiles/me' -Headers $h
Invoke-RestMethod -Uri 'http://localhost:8082/skillmatch/profiles/me/completeness' -Headers $h
```

Résultat attendu : `201` à la création, puis `200` avec `completude: 85` (titre, localisation, compétences et expériences renseignés, aucun badge).
