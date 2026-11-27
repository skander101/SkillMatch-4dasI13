-- Migration SQL idempotente pour messagerie_db (MySQL 8+)
-- Complète conversations/messages et crée signalements, audit_messagerie, suspensions_envoi.

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS archivee TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS date_archivage DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS signalee TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS date_signalement DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS type_conversation VARCHAR(10) NOT NULL DEFAULT 'DIRECT',
    ADD COLUMN IF NOT EXISTS nom_groupe VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS date_creation DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS ordre_participants TEXT NULL;

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS signalee TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS masque TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS type_message VARCHAR(20) NOT NULL DEFAULT 'texte',
    ADD COLUMN IF NOT EXISTS fichier_nom VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS fichier_url VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS fichier_taille BIGINT NULL;

CREATE TABLE IF NOT EXISTS signalements (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    message_id VARCHAR(36) NOT NULL,
    auteur_signalement_id BIGINT NOT NULL,
    auteur_message_id BIGINT NOT NULL,
    motif VARCHAR(60) NOT NULL,
    commentaire TEXT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    action_admin VARCHAR(40) NULL,
    admin_id BIGINT NULL,
    note_admin TEXT NULL,
    date_signalement DATETIME(6) NOT NULL,
    date_traitement DATETIME(6) NULL,
    INDEX idx_signalements_conv (conversation_id),
    INDEX idx_signalements_statut (statut)
);

CREATE TABLE IF NOT EXISTS audit_messagerie (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    conversation_id VARCHAR(36) NULL,
    message_id VARCHAR(36) NULL,
    signalement_id VARCHAR(36) NULL,
    acteur_id BIGINT NOT NULL,
    acteur_nom VARCHAR(150) NULL,
    acteur_role VARCHAR(30) NULL,
    action VARCHAR(60) NOT NULL,
    details TEXT NULL,
    date_action DATETIME(6) NOT NULL,
    INDEX idx_audit_conv (conversation_id),
    INDEX idx_audit_date (date_action)
);

CREATE TABLE IF NOT EXISTS suspensions_envoi (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(36) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    motif TEXT NULL,
    admin_id BIGINT NOT NULL,
    date_debut DATETIME(6) NOT NULL,
    date_fin DATETIME(6) NULL,
    INDEX idx_suspensions_user (user_id, active)
);
