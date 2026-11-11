# Guide de contribution

## Règles générales

- Ne jamais pousser directement sur `main`.
- Une fonctionnalité ou un microservice correspond à une branche.
- Chaque membre est responsable de son dossier de microservice.
- Tout changement passe par une pull request relue par au moins un autre membre.
- Les secrets, mots de passe et fichiers `.env` ne sont jamais commités.

## Branches

| Branche | Usage |
|---|---|
| `main` | Socle commun stable et fonctionnalités validées |
| `feature/<service>` | Développement d'un microservice |
| `fix/<sujet>` | Correction ciblée |
| `docs/<sujet>` | Documentation et diagrammes |

Exemples : `feature/offres-service`, `feature/api-gateway`, `feature/frontend-common`, `docs/technical-document`.

## Premier commit d'un microservice

Chaque responsable crée son dossier à partir du template fourni :

```bash
git switch main
git pull
git switch -c feature/offres-service

cp -r Backend/_template-service Backend/offres-service
```

Adapter ensuite :

- le nom du package `tn.esprit.skillmatch.offres` ;
- l'`artifactId` et le nom de l'application ;
- le port ;
- le nom de la base de données ;
- le nom Eureka du service.

Puis :

```bash
git add Backend/offres-service
git commit -m "feat(offres): initialize offres-service module"
```

## Messages de commit

Format recommandé :

```text
<type>(<scope>): <description courte>
```

Types : `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`.

Exemples :

```text
feat(offres): add offer and application entities
feat(profils): expose profile update endpoint
docs(diagrams): add global architecture diagram
fix(messagerie): restrict conversation access to participants
```

## Pull requests

Une pull request doit contenir :

- un titre explicite ;
- une description courte du changement ;
- les tests ou vérifications effectués ;
- une capture ou un lien vers le diagramme si le contrat change.

La fusion est autorisée après :

- revue d'au moins un membre ;
- compilation réussie ;
- absence de conflit avec `main`.

## Définition de terminé

- Le code compile.
- Les endpoints ou événements prévus sont documentés.
- Les règles métier sont testées ou vérifiées manuellement.
- Le README du service est à jour.
- Les identifiants de bases et de files d'attente sont configurables par variables d'environnement.
- Aucun fichier généré, secret ou fichier IDE n'est commité.
