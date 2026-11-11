# Template de microservice

Ce dossier est un modèle Spring Boot minimal destiné à être copié par chaque responsable de microservice. Ce n'est pas un microservice du projet.

## Contenu

- `pom.xml` : dépendances Web, JPA, Validation, Actuator, AMQP, Eureka Client et MySQL.
- `TemplateServiceApplication.java` : classe de démarrage.
- `application.yml` : configuration par variables d'environnement.
- `TemplateServiceApplicationTests.java` : test de présence de la classe de démarrage.

## Utilisation

Depuis la racine du dépôt :

```bash
git switch main
git pull
git switch -c feature/<nom-service>

cp -r Backend/_template-service Backend/<nom-service>
```

## Renommage à effectuer

1. Renommer le package `tn.esprit.skillmatch.template` en `tn.esprit.skillmatch.<service>`.
2. Renommer `TemplateServiceApplication` en `<Service>Application`.
3. Modifier l'`artifactId`, le `name` et la `description` dans `pom.xml`.
4. Modifier `spring.application.name`.
5. Modifier le port dans `application.yml`.
6. Modifier `MYSQL_DATABASE` avec le nom de la base du service.
7. Vérifier le nom Eureka du service.
8. Supprimer ou adapter le test fourni.

## Exemple pour le service Offres

```text
Package : tn.esprit.skillmatch.offres
Classe  : OffresServiceApplication
Port    : 8081
Base    : offres_db
Nom     : offres-service
```

## Démarrage

```bash
cd Backend/<nom-service>
mvn spring-boot:run
```

Les variables `MYSQL_*`, `RABBITMQ_*`, `EUREKA_URL` et `SERVER_PORT` peuvent être définies dans l'environnement ou dans un fichier `.env` local non commité.

## Premier commit attendu

```bash
git add Backend/<nom-service>
git commit -m "feat(<service>): initialize <service> module"
```
