# Événements Offres et Candidatures

Les dépendances RabbitMQ étaient déjà dans le template. L'autoconfiguration
est maintenant active dans `application.yml` ; aucun changement Angular requis.

## Déclenchements

| Message / routing key | Déclenchement | File durable |
|---|---|---|
| `OfferPublished` | Une offre passe d'un autre statut à PUBLIEE | `skillmatch.offres.OfferPublished` |
| `ApplicationSubmitted` | Une nouvelle candidature est enregistrée | `skillmatch.offres.ApplicationSubmitted` |
| `StatusChanged` | Le statut d'une candidature change, retrait compris | `skillmatch.offres.StatusChanged` |

Modifier le texte d'une offre publiée, renvoyer le même statut ou modifier une
lettre ne publie pas de nouveau message. La suppression physique ADMIN n'est
pas un changement de statut et ne publie pas `StatusChanged`.

## Comment le code fonctionne

1. Le service métier valide la demande, enregistre les données et prépare un
   `EvenementOffres` avec une copie des informations utiles.
2. `ApplicationEventPublisher` transmet cet événement interne à Spring.
3. `RabbitPublisher`, avec `@TransactionalEventListener(AFTER_COMMIT)`, attend
   la validation de MySQL. Une transaction annulée n'envoie rien.
4. `RabbitTemplate` convertit en JSON et envoie vers l'exchange topic durable
   **`skillmatch.events`**, avec le nom de l'événement comme routing key.
5. RabbitMQ route vers la file liée. Le message est persistant et le producteur
   attend une confirmation du broker ; un message sans destination est signalé.

L'enveloppe JSON contient `eventId` (UUID), `type`, `date` (UTC) et `donnees`.
Pour OfferPublished : identifiants, titre, compétences, localisation, contrat,
salaire et expiration. Pour ApplicationSubmitted : identifiants candidat,
recruteur, offre, candidature et titre. Pour StatusChanged : identifiants,
ancien/nouveau statut et date d'entretien. Aucun JWT, mot de passe, lettre ou
contenu de CV n'est envoyé. Les consommateurs peuvent utiliser `eventId` pour
reconnaître un message déjà traité.

## Démarrer RabbitMQ avec Docker Desktop

RabbitMQ et Erlang sont fournis par l'image Docker officielle
`rabbitmq:4.4.1-management`. Aucune installation RabbitMQ/Erlang sur Windows
ni activation manuelle du plugin management n'est nécessaire.

1. Ouvrir Docker Desktop et attendre que le moteur Linux soit prêt.
2. Docker utilise les ports locaux 5673/15673 pour éviter le conflit avec le broker Windows en 5672/15672. Pour arrêter ultérieurement l'ancien service, ouvrir
   Services Windows (`services.msc`), arrêter **RabbitMQ** et passer son démarrage
   en **Manuel** pour éviter un conflit au prochain redémarrage. Ses données
   restent conservées ; elles ne sont pas transférées automatiquement dans Docker.
3. À la racine du projet, lancer :

```console
docker compose up -d --wait rabbitmq
docker compose ps
docker compose exec rabbitmq rabbitmq-diagnostics -q ping
```

Le premier lancement télécharge l'image. Dans Docker Desktop > Containers,
le projet **skillmatch** contient le service **rabbitmq**. Les lancements suivants
peuvent se faire avec le bouton Start de Docker Desktop.

4. Ouvrir http://localhost:15673 et se connecter avec les identifiants locaux
   par défaut : **skillmatch / skillmatch-local**.
5. Démarrer MySQL, Eureka, Commun, Offres, Gateway puis Angular comme auparavant.
   Redémarrer Offres pour qu'il déclare l'exchange et les trois files sur ce broker.

### Ce que contient compose.yaml

- `image` : version 4.4.1 avec interface management incluse et Erlang intégré.
- `hostname: rabbitmq` : identité stable du nœud, utile pour retrouver ses données.
- `ports` : 5673 vers 5672 pour Spring AMQP et 15673 vers 15672 pour l'interface, publiés sur localhost.
- `environment` : utilisateur, mot de passe et virtual host créés au premier démarrage.
- `volumes` : `rabbitmq_data` conserve les données dans `/var/lib/rabbitmq`.
- `healthcheck` : vérifie régulièrement que le nœud RabbitMQ répond ; `--wait`
  attend que le conteneur soit sain avant de rendre la main.
- `restart: unless-stopped` : redémarre le conteneur lorsque Docker redémarre,
  sauf s'il a été arrêté volontairement.

### Connexion Spring Boot

Dans `Backend/offres-service/src/main/resources/application.yml` :

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5673}
    username: ${RABBITMQ_USER:skillmatch}
    password: ${RABBITMQ_PASSWORD:skillmatch-local}
    virtual-host: ${RABBITMQ_VHOST:/}
```

Offres reste démarré dans IntelliJ et contacte le port local publié par Docker.
Le code Java des événements, les routing keys et les noms des files sont conservés.
Les identifiants par défaut ont été alignés dans le modèle de service également.
Config Server conserve la configuration publique ; les identifiants restent locaux.

`.env.example` documente les variables. Le fichier `.env` à la racine, s'il existe,
est lu par Compose. Spring Boot lancé dans IntelliJ ne lit pas ce fichier
automatiquement : définir les mêmes valeurs dans les variables d'environnement
IntelliJ si elles diffèrent des valeurs par défaut. Vérifier en particulier
qu'aucune ancienne variable `RABBITMQ_USER=guest` ou `RABBITMQ_PORT=5672` ne reste dans la configuration.
Les identifiants fournis sont réservés au développement local.

Les variables `RABBITMQ_DEFAULT_*` initialisent une nouvelle base RabbitMQ ;
changer `.env` après création du volume ne modifie pas les utilisateurs existants.
Les anciens messages du service Windows ne se trouvent pas dans ce nouveau volume.

### Arrêt et diagnostic

```console
docker compose logs --tail=100 rabbitmq
docker compose stop rabbitmq
docker compose start rabbitmq
```

`docker compose down` supprime le conteneur mais conserve le volume nommé.
Ne pas ajouter `-v` si l'on veut conserver les messages.

## Démonstration

1. Redémarrer Offres après RabbitMQ ; ouvrir Queues and Streams dans l'interface RabbitMQ.
2. Publier un brouillon depuis le compte recruteur : consulter la file OfferPublished.
3. Postuler depuis un candidat : consulter ApplicationSubmitted.
4. Traiter la candidature ou la retirer : consulter StatusChanged.
5. Utiliser Get messages avec remise en file (`requeue=true`) pour lire le JSON
   sans retirer le message. Les files restent remplies car aucun consommateur
   métier n'est encore développé dans cette étape.

Les collègues pourront lier leurs propres files au même exchange pour les alertes
ou notifications. Cette étape publie les messages ; elle ne crée pas ces fonctions.

## Tests et limites

`mvn package` exécute les tests unitaires sans nécessiter RabbitMQ.
Les tests vérifient les déclenchements, l'absence de doublon sur un statut inchangé,
le commit/rollback et la conservation du résultat CRUD en cas de panne du broker.

Pour le test réel, avec RabbitMQ, MySQL et JWT_SECRET définis :

```powershell
$env:RABBITMQ_IT = 'true'
mvn '-Dtest=RabbitPublisherIT' test
```

Ce test crée un exchange et des files `skillmatch.test.<UUID>` puis les supprime.
Il ne lit ni ne purge les files métier. `skillmatch.events.exchange` et
`skillmatch.events.queue-prefix` permettent cette isolation et ne doivent pas
être changés pour une démonstration normale.

MySQL et RabbitMQ ne partagent pas une transaction globale. En cas de panne après
commit, le CRUD reste enregistré ; après trois tentatives de connexion, l'échec
est journalisé `RabbitMQ NON ENVOYE` avec l'eventId. Il n'y a pas de stockage
outbox ni de rejeu automatique après redémarrage : la livraison n'est pas garantie
si le broker reste indisponible ou si le processus s'arrête entre commit et envoi.
Cette limite est volontaire pour garder cette première intégration simple.

Validation de cette modification : Compose valide ; conteneur RabbitMQ 4.4.1 sain ; ping réussi ; interface management accessible sur 15673. Le test RabbitPublisherIT a réussi contre Docker sur localhost:5673 avec MySQL local : les trois JSON sont reçus après commit et aucun après rollback. Les files temporaires du test sont supprimées à la fin. Redémarrer normalement Offres après Config Server pour créer les trois files métier sur le broker Docker.
