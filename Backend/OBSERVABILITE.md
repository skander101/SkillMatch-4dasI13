# Observabilité avec Spring Boot Actuator

Actuator est configuré dans les cinq services présents : Offres, Commun, Gateway,
Discovery et Config Server. Le modèle `_template-service` reprend ces réglages.
La configuration centralisée d'Offres et sa configuration locale sont alignées.

## Accès après redémarrage

| Service | Adresse de base |
|---|---|
| Offres | http://localhost:8081/actuator |
| Commun | http://localhost:8087/actuator |
| Gateway | http://localhost:8080/actuator |
| Discovery | http://localhost:8761/actuator |
| Config | http://localhost:8889/actuator |

Ces ports sont ceux par défaut. Les endpoints sont consultés directement sur chaque
service ; `/actuator` sur la Gateway surveille la Gateway elle-même.

## Que consulter ?

- `/health` : état global `UP` ou `DOWN` et état des composants. Pour Offres,
  Spring vérifie notamment MySQL (`db`), RabbitMQ (`rabbit`) et le disque.
  Un composant requis indisponible peut donner HTTP 503.
- `/health/liveness` : vérifie que l'application est vivante.
- `/health/readiness` : indique si l'application accepte du trafic selon son état
  Spring ; cette sonde ne vérifie pas MySQL ou RabbitMQ. Utiliser `/health` pour eux.
- `/info` : nom du service ; Offres indique aussi l'origine de sa configuration.
- `/metrics` : liste des mesures disponibles.
- `/metrics/jvm.memory.used` : mémoire utilisée par la JVM, en octets.
- `/metrics/process.cpu.usage` : utilisation CPU du processus.
- `/metrics/http.server.requests` : nombre et durée des requêtes HTTP. Effectuer
  une requête HTTP avant de consulter cette mesure si elle n'existe pas encore.

Les métriques portent le tag `application` avec le nom du service. On peut filtrer
une mesure avec les tags proposés dans sa réponse, par exemple
`/metrics/http.server.requests?tag=status:200`.

Exemple PowerShell :

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health | ConvertTo-Json -Depth 10
Invoke-RestMethod http://localhost:8081/actuator/info
Invoke-RestMethod http://localhost:8081/actuator/metrics/jvm.memory.used
```

## Périmètre

Les détails techniques de santé sont masqués ; les noms et statuts des composants
restent visibles. Seuls `health`, `info` et `metrics` sont ajoutés à l'exposition HTTP.
L'endpoint `gateway` déjà exposé est conservé. `/env`, `/beans`, `/heapdump` et
`/shutdown` ne sont pas ajoutés. L'activation de `management.info.env.enabled`
sert à publier les propriétés `info.*`, pas les variables d'environnement.

Ces endpoints de supervision sont accessibles sans authentification dans la
configuration actuelle du projet ; limiter leur accès au réseau de supervision
lors d'un déploiement public.

Les métriques sont consultables en JSON, sans historique persistant ni tableau de
bord. Prometheus, Grafana et le traçage distribué ne sont pas installés.

## Vérification automatisée

Depuis `Backend/offres-service`, lancer `mvn -Dtest=ActuatorEndpointsTest test`.
Le test démarre un vrai serveur sur un port libre et vérifie santé, sondes,
informations et métriques HTTP/JVM, ainsi que l'absence d'exposition de `/env`.
Il ne nécessite pas MySQL ou RabbitMQ et ne valide donc pas leur disponibilité réelle.
