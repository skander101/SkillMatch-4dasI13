# Document technique

Document technique complet du projet. Il doit contenir tout le contenu de la présentation, avec plus de détail.

## Fichier actuel

- `SkillMatch_Dossier_Technique_Design_Final_v2.pdf`

La source Word ou LaTeX pourra être ajoutée pour faciliter les modifications.

## Plan attendu

1. **Présentation du sujet**
   - Problématique
   - Storytelling et ODD
   - Acteurs
   - Fonctionnalités principales

2. **Architecture globale**
   - Diagramme d'architecture
   - Rôle de l'API Gateway
   - Sécurité JWT
   - Découverte de services avec Eureka
   - Configuration centralisée
   - Données et communications

3. **Découpage en microservices**
   - Responsabilités de chaque service
   - Choix des bases de données
   - Règles métier
   - Contrats REST
   - Événements RabbitMQ

4. **Données par service**
   - Entités
   - Colonnes JSON
   - Identifiants partagés entre services
   - Absence de clés étrangères inter-services

5. **Interactions**
   - Appels synchrones
   - Événements asynchrones
   - Diagrammes de séquence

6. **Frontend**
   - Layout commun
   - Authentification
   - Gestion des rôles

7. **Technologies**
   - Spring Boot, Spring Cloud, MySQL, RabbitMQ
   - Angular, TypeScript
   - Docker Compose et CI

8. **Organisation du travail**
   - Répartition entre les membres
   - Organisation Git
   - Gestion de projet
   - Planning et risques

9. **Tests et validation**
   - Stratégie de tests
   - Scénarios principaux

10. **Conclusion et perspectives**

## Annexes

- Contrats d'API.
- Collections Postman.
- Scripts SQL de création des bases.
