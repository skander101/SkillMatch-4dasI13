# Eureka — annuaire SkillMatch

Ce projet Maven indépendant reprend les versions du template : Java 17,
Spring Boot 3.3.5 et Spring Cloud 2023.0.3. Il ne nécessite pas MySQL.

## Démarrer

Dans IntelliJ, ouvrir `Backend/discovery-service/pom.xml`, choisir Java 17,
puis lancer `DiscoveryServiceApplication`. Ou :

```bash
mvn spring-boot:run
```

Ouvrir http://localhost:8761 pour voir les services inscrits.
`SERVER_PORT` permet de changer le port (8761 par défaut).

## Ordre de lancement

1. MySQL dans XAMPP, puis `discovery-service` (8761).
2. `commun-service` (8087), avec le même `JWT_SECRET` qu'Offres.
3. `offres-service` (8081).
4. `api-gateway` (8080), puis Angular (4200).

Sur le tableau Eureka, `OFFRES-SERVICE` et `API-GATEWAY` doivent apparaître
avec le statut `UP`. L'inscription et la mise à jour des caches peuvent prendre
jusqu'à deux minutes au démarrage, selon les caches. Le service commun conserve sa configuration
actuelle et reste appelé directement sur 8087.

La Gateway utilise `lb://offres-service` pour les routes Offres et Candidatures :
Eureka fournit l'adresse et le port réels du service. Si Offres tourne sur 18081,
la Gateway le découvre sans modification de route. Ne pas définir
`OFFRES_SERVICE_URL` pour ce mode ; cette variable conserve la priorité.
Le JWT et les règles CRUD restent vérifiés dans les services métier.

Les clients utilisent `EUREKA_URL=http://localhost:8761/eureka/` par défaut.
`EUREKA_REGISTER` et `EUREKA_FETCH` valent maintenant `true` dans Offres et
la Gateway. Pour un essai sans Eureka, mettre ces deux variables à `false`
et définir `OFFRES_SERVICE_URL=http://localhost:8081` dans la Gateway.
Attention : le `.env.example` commun conserve les anciennes valeurs `false`.
Si vous les avez copiées dans IntelliJ, les remplacer par `true` uniquement
dans les configurations Offres et Gateway.

## Vérification

`mvn package` compile et teste le registre HTTP sur un port libre.
Une requête sans JWT sur `http://localhost:8080/skillmatch/offres` doit retourner
401 après découverte d'Offres ; 503 signifie qu'aucune instance n'a été trouvée.
Les opérations CRUD exigent toujours une vraie connexion.

Validation effectuée : `mvn package` réussi pour Discovery (1 test), Gateway
(8 tests) et Offres (68 tests). Les deux clients sont apparus `UP` dans le
registre et le tableau web a répondu 200. Sans définir `OFFRES_SERVICE_URL`,
la Gateway a découvert Offres sur 18081 : les routes Offres et Candidatures
ont répondu 401 sans JWT, comme les appels directs. Les processus de test
ont ensuite été arrêtés ; aucune donnée de test n'a été créée.

RabbitMQ, Config Server et Docker ne sont pas activés par cette modification.
