# Tests et compétences

Microservice Maven indépendant de SkillMatch. Il utilise Java 17, le port `8084` et sa propre base MySQL `tests_db` sous XAMPP.

## Démarrage

Depuis `Backend/tests-service`, démarrer MySQL puis exécuter `mvn spring-boot:run`. La configuration reprend les variables `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER` et `MYSQL_PASSWORD` du gabarit. Le port d’écoute se règle avec `SERVER_PORT`.

Le point de contrôle est `GET /actuator/health`. Les appels métier transmettent le jeton SkillMatch avec `Authorization: Bearer …`. Le service commun confirme l’identité numérique et le rôle via `GET /skillmatch/auth/me`. Configurer son URL avec `COMMUN_SERVICE_URL` si elle diffère de `http://localhost:8087`.

`mvn verify` démarre l’application sur un port temporaire et vérifie sa réponse HTTP. H2 est une dépendance de test. Le JAR utilise MySQL. Le profil de vérification isole aussi le démarrage d’Eureka et de RabbitMQ.
