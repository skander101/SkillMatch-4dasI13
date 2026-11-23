-- Migration SQL pour messagerie_db (MySQL 8+)
-- Ajoute les colonnes de gestion des groupes sur conversations et les types/pièces jointes sur messages.

ALTER TABLE conversations
    MODIFY COLUMN participant_ids TEXT NOT NULL,
    MODIFY COLUMN candidat_user_id BIGINT NOT NULL;

-- Si la colonne historique 'type' existe déjà sans 'type_conversation', décommenter :
-- ALTER TABLE conversations CHANGE COLUMN type type_conversation VARCHAR(10) NOT NULL DEFAULT 'DIRECT';
ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS type_conversation VARCHAR(10) NOT NULL DEFAULT 'DIRECT',
    ADD COLUMN IF NOT EXISTS nom_groupe VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS date_creation DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS ordre_participants TEXT NULL;

UPDATE conversations
SET type_conversation = 'DIRECT'
WHERE type_conversation IS NULL OR type_conversation = '';

UPDATE conversations
SET date_creation = COALESCE(dernier_message_at, NOW())
WHERE date_creation IS NULL;

UPDATE conversations
SET ordre_participants = participant_ids
WHERE ordre_participants IS NULL OR ordre_participants = '';

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS type_message VARCHAR(20) NOT NULL DEFAULT 'texte',
    ADD COLUMN IF NOT EXISTS fichier_nom VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS fichier_url VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS fichier_taille BIGINT NULL;

UPDATE messages
SET type_message = 'texte'
WHERE type_message IS NULL OR type_message = '';
