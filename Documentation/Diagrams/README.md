# Diagrammes

Ce dossier contient les sources Mermaid et les exports des diagrammes du projet.

## Convention de nommage

```text
<type>-<sujet>.mmd
<type>-<sujet>.png
<type>-<sujet>.svg
```

## Diagrammes attendus

| Fichier | Contenu |
|---|---|
| `cas-utilisation-global.mmd` | Cas d'utilisation général et acteurs |
| `architecture-globale.mmd` | Architecture microservices complète |
| `sequence-auth-jwt.mmd` | Séquence d'authentification JWT |
| `classes-offres.mmd` | Diagramme de classes du service Offres |
| `classes-profils.mmd` | Diagramme de classes du service Profils |
| `classes-entreprises.mmd` | Diagramme de classes du service Entreprises |
| `classes-tests.mmd` | Diagramme de classes du service Tests |
| `classes-alertes.mmd` | Diagramme de classes du service Alertes |
| `classes-messagerie.mmd` | Diagramme de classes du service Messagerie |
| `classes-commun.mmd` | Diagramme de classes du service commun |
| `sequence-synchrone.mmd` | Exemple d'appel REST synchrone |
| `sequence-asynchrone.mmd` | Exemple d'événement RabbitMQ |
| `etats-candidature.mmd` | Cycle de vie d'une candidature |
| `planning-gantt.mmd` | Planning du projet |
| `repartition-taches.mmd` | Répartition entre les membres |
| `structure-git.mmd` | Structure du dépôt |
| `strategie-branches.mmd` | Stratégie de branches Git |

## Règles

- Tous les nœuds de base de données du diagramme d'architecture sont des bases MySQL.
- Les diagrammes sont exportés en PNG ou SVG pour les slides et le document technique.
- Le diagramme d'architecture globale est également fourni en fichier image pour Blackboard.
