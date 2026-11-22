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

## Démarrer localement sous Windows

RabbitMQ nécessite Erlang. Installation autorisée dans un PowerShell administrateur :

```powershell
choco install rabbitmq -y
```

Activer ensuite `rabbitmq_management` avec `rabbitmq-plugins.bat enable rabbitmq_management`
depuis le dossier `sbin` de RabbitMQ. Le service Windows s'appelle `RabbitMQ`.

Ordre : MySQL et RabbitMQ, Eureka, Commun, Offres, Gateway, Angular.
Interface RabbitMQ : http://localhost:15672, compte local `guest` / `guest`.
Le compte guest est prévu pour localhost ; ne pas l'utiliser pour un serveur distant.

Variables Offres, reprises du template : `RABBITMQ_HOST` (localhost),
`RABBITMQ_PORT` (5672), `RABBITMQ_USER` (guest), `RABBITMQ_PASSWORD` (guest),
`RABBITMQ_VHOST` (/). Ne pas commiter de vrais identifiants.

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

Validation locale : RabbitMQ 4.4.1 et Erlang 27.3.4.8 installés ; service Windows démarré.
84 tests unitaires et 2 tests d'intégration réussis (RabbitMQ réel et CRUD MySQL).
Le contrôle de santé d'Offres répond UP et les trois files métier durables existent.
