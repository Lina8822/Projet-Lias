-- One-time migration: preserve multiple membership periods for the same team.
-- Existing rows and foreign keys are retained. Run after a database backup.
ALTER TABLE membre_equipe
  ADD COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
  ADD INDEX idx_membre_equipe_utilisateur (utilisateur_id);
ALTER TABLE membre_equipe DROP PRIMARY KEY, ADD PRIMARY KEY (id);
