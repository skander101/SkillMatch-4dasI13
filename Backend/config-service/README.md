# Config Server pour Offres et Candidatures

Ce projet Maven indépendant reprend Java 17, Spring Boot 3.3.5 et Spring Cloud
2023.0.3 du dépôt. Il fournit la configuration publique de offres-service par HTTP.

## Fonctionnement

1. ConfigServiceApplication active Spring Cloud Config Server.
2. Le profil native lit src/main/resources/config-repo/offres-service.yml.
3. Au démarrage, Offres demande /offres-service/default sur le port 8888.
4. Les valeurs reçues complètent ou remplacent les valeurs locales d'Offres.
5. Sans serveur disponible, optional:configserver permet de garder le secours local.

Le nom du fichier correspond à spring.application.name=offres-service.
Aucun fichier n'est fourni pour les microservices des collègues.
Pas de nouveau dépôt Git, pas de dépendance à Eureka pour démarrer ce serveur.

## Démarrer dans IntelliJ

1. Importer Backend/config-service/pom.xml comme projet Maven (Java 17).
2. Lancer tn.esprit.skillmatch.config.ConfigServiceApplication.
3. Ouvrir http://localhost:8888/offres-service/default : la réponse est du JSON,
   pas une page graphique. Elle doit contenir info.configuration.origine=config-server.
4. Démarrer MySQL, RabbitMQ, Eureka et Commun comme auparavant, puis Offres,
   Gateway et Angular. JWT_SECRET reste obligatoire dans la configuration de lancement d'Offres.
5. Ouvrir http://localhost:8081/actuator/info (adapter si le port Offres a changé).
   Le résultat attendu est :

```json
{"configuration":{"origine":"config-server"}}
```

Sans Config Server au démarrage d'Offres, origine vaut local. Si on démarre Config
Server ensuite, redémarrer Offres pour charger la configuration distante.
Les connexions ont des délais limités : 2 secondes pour se connecter, 3 pour lire.

## Fichiers et paramètres

| Fichier | Rôle |
|---|---|
| pom.xml | Dépendance spring-cloud-config-server et versions du projet |
| ConfigServiceApplication.java | Active le serveur avec @EnableConfigServer |
| application.yml | Port, profil native, dossier des fichiers, accès local uniquement |
| config-repo/offres-service.yml | MySQL sans identifiants, JPA, RabbitMQ sans identifiants, Eureka et URL Commun |
| ConfigServiceTest.java | Vérifie le JSON, l'absence des secrets et le refus d'un service non configuré |

Variables du serveur : CONFIG_SERVER_PORT (8888) et CONFIG_REPO_LOCATION
(classpath:/config-repo/). Utiliser CONFIG_SERVER_PORT, pas SERVER_PORT, pour le
port de ce serveur. Côté Offres, CONFIG_SERVER_URL vaut http://localhost:8888.

Les mots de passe MySQL/RabbitMQ, noms de connexion et JWT_SECRET restent dans
l'environnement et la configuration locale d'Offres. Ne pas les ajouter au YAML partagé.
L'API publique du serveur ne retourne pas ces clés.

Le YAML d'Offres existant sert de secours : modifier d'abord la configuration du
serveur quand il est utilisé, et garder le secours cohérent si les valeurs changent.
Pour changer le YAML embarqué, reconstruire/redémarrer Config Server puis redémarrer
Offres. Aucun rafraîchissement dynamique ni Spring Cloud Bus n'est ajouté.

On peut éviter de reconstruire le serveur en plaçant offres-service.yml dans un
dossier externe et en définissant, par exemple,
CONFIG_REPO_LOCATION=file:///C:/SkillMatch/config-repo/ avant de lancer le serveur.
Modifier ce fichier puis redémarrer Offres suffit ; conserver le dossier local.

## Vérifications

Dans Backend/config-service : mvn package.
Dans Backend/offres-service : mvn package.
ConfigServerClientTest teste le chargement distant et le secours sur un serveur
HTTP temporaire, sans MySQL. Les autres tests unitaires restent disponibles.

Références : [serveur Spring Cloud Config](https://docs.spring.io/spring-cloud-config/reference/4.1/server.html),
[profil native](https://docs.spring.io/spring-cloud-config/reference/4.1/server/environment-repository/file-system-backend.html),
[client et import facultatif](https://docs.spring.io/spring-cloud-config/reference/4.1/client.html).

Validation locale : 1 test du serveur et 92 tests Offres réussis. Le chargement
distant et le secours local sont testés. Avec les deux vrais JAR démarrés,
Offres indique configuration.origine=config-server et son contrôle de santé est UP
(MySQL et RabbitMQ disponibles). Les deux instances temporaires ont été arrêtées.
