-- À exécuter une fois sur une base existante pour autoriser le dépôt sans CV.
-- Les références CV déjà enregistrées sont conservées.
ALTER TABLE candidatures MODIFY COLUMN cv_id BIGINT NULL;
