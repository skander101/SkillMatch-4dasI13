# Tests et compétences

`tests-service` gère le catalogue de tests techniques, leur correction et les preuves de réussite consultées par le service Offres. Le projet Maven suit le gabarit SkillMatch avec Java 17, Spring Boot 3.3.5, le port `8084` et la base MySQL `tests_db` sous XAMPP.

## Démarrage local

1. Démarrer MySQL dans XAMPP et créer `tests_db` si le compte MySQL ne possède pas le droit `CREATE DATABASE`.
2. Démarrer le service commun sur `8087` pour les appels métier et RabbitMQ sur `5672` pour la publication des réussites.
3. Depuis `Backend/tests-service`, exécuter `mvn spring-boot:run`. Dans IntelliJ, ouvrir le `pom.xml` du service sous JDK 17 puis lancer `TestsServiceApplication`.
4. Vérifier `GET http://localhost:8084/actuator/health`. Consulter le point d’accès de santé sans jeton, puis utiliser un jeton SkillMatch pour les routes métier.
5. Pour utiliser les interfaces Angular, démarrer aussi `Backend/api-gateway` sur `8080` et `Frontend` sur `4200`. La passerelle transmet les routes des tests et des résultats à `tests-service`. Renseigner `TESTS_SERVICE_URL` dans la passerelle si le service utilise une autre adresse.

| Variable | Valeur locale par défaut |
| --- | --- |
| `SERVER_PORT` | `8084` |
| `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE` | `localhost`, `3306`, `tests_db` |
| `MYSQL_USER`, `MYSQL_PASSWORD` | `root`, vide |
| `COMMUN_SERVICE_URL` | `http://localhost:8087` |
| `RABBITMQ_HOST`, `RABBITMQ_PORT` | `localhost`, `5672` |
| `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | `guest`, `guest` |
| `RABBITMQ_EXCHANGE`, `RABBITMQ_PUBLICATION_ENABLED` | `skillmatch.events`, `true` |
| `EUREKA_URL`, `EUREKA_REGISTER`, `EUREKA_FETCH` | `http://localhost:8761/eureka/`, `false`, `false` |

## Contrat REST

Suivre le dossier technique pour les rôles et les codes HTTP. Les routes sont préfixées par `/api`. Envoyer `Authorization: Bearer <jeton>`. Le service commun confirme l’identité numérique et le rôle via `GET /skillmatch/auth/me`.

| Méthode et route | Rôle | Résultat principal |
| --- | --- | --- |
| `POST /api/tests` | ADMIN | `201` avec en-tête `Location` |
| `GET /api/tests?competence=Java` | Connecté | `200`, liste avec filtre de compétence facultatif |
| `GET /api/tests/{id}` | Connecté | `200` avec le QCM, ou `404` |
| `GET /api/tests/{id}/definition` | ADMIN | `200` avec le corrigé pour l’édition, ou `404` |
| `PUT /api/tests/{id}` | ADMIN | `200`, ou `409` après une première tentative |
| `DELETE /api/tests/{id}` | ADMIN | `204`, ou `409` si des résultats existent |
| `POST /api/tests/{id}/submit` | CANDIDAT | `200` avec le résultat, ou `409` si le test est réussi ou les trois essais épuisés |
| `GET /api/results/me` | CANDIDAT | `200`, historique de l’utilisateur connecté |
| `GET /api/tests/{id}/results` | ADMIN | `200`, résultats du test, ou `404` |
| `GET /api/results/check?userId=42&testId=7` | RECRUTEUR, ADMIN | `200` avec la réussite cherchée, ou `404` |

Une entrée invalide renvoie `400`. Un jeton absent ou refusé renvoie `401`. Un rôle insuffisant renvoie `403`. Le contrôle de réussite est destiné à Offres, qui peut transmettre le jeton du recruteur lors de la décision de candidature.

**Définition d’un test**

```json
{
  "titre": "Bases de Java",
  "competence": "Java",
  "dureeMinutes": 10,
  "seuilReussite": 50,
  "questions": [
    {
      "id": "q1",
      "enonce": "Quel mot-clé rend une variable non réassignable ?",
      "options": ["final", "super"],
      "indexBonneReponse": 0
    }
  ]
}
```

Le candidat soumet `{"reponses":{"q1":0}}`. Les indices commencent à zéro. Une question omise vaut zéro point. Une soumission vide, une question inconnue ou un indice hors limites renvoie `400` sans consommer de tentative. Le corrigé est accessible uniquement sur la route de définition réservée au rôle `ADMIN`. Le score entier est calculé sur 100 et le seuil est inclus. L’identifiant du candidat provient du jeton validé.

## Données et événement

`TestTechnique` stocke les questions dans une colonne JSON. `ResultatTest` stocke chaque tentative et référence le test localement. L’identifiant d’utilisateur est une référence externe. Un verrou sur le test et une contrainte d’unicité du numéro de tentative protègent contre les requêtes concurrentes.

Après validation de la transaction de la première réussite, le service publie `TestPassed` avec la clé de routage `TestPassed` sur l’échange topic `skillmatch.events`. L’enveloppe JSON contient `eventId`, `type`, `occurredAt`, `source` et `data`. `data` contient notamment l’identifiant du résultat, celui du candidat, le test et la compétence. Profils et Notifications gèrent leurs propres abonnements.

La tentative reste enregistrée si le broker est indisponible. L’échec d’envoi est journalisé et la reprise automatique est hors du périmètre de cette version. Les consommateurs peuvent dédupliquer sur `eventId`.

## Vérification

`mvn verify` compile, assemble le JAR et exécute les tests. Ils démarrent un serveur HTTP sur un port aléatoire, avec H2 uniquement sur le classpath de test. Ils couvrent la persistance JSON, les autorisations, le calcul du score, les essais concurrents, le contrat de consultation et le message RabbitMQ. Le profil de test n’exige aucun service externe.
