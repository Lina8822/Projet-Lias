-- Run after lias_update.sql and a backup. No data is deleted.
-- Existing orphan references must be reviewed before applying; never remove history.
-- RESTRICT (default) prevents deletion of referenced institutional records.
DROP PROCEDURE IF EXISTS lias_add_fk;
DELIMITER $$
CREATE PROCEDURE lias_add_fk(IN t VARCHAR(64), IN c VARCHAR(64), IN parent_t VARCHAR(64), IN parent_c VARCHAR(64))
BEGIN
 DECLARE parent_type VARCHAR(100);
 DECLARE nullable_column VARCHAR(3);
 IF NOT EXISTS (SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=t AND COLUMN_NAME=c AND REFERENCED_TABLE_NAME=parent_t AND REFERENCED_COLUMN_NAME=parent_c) THEN
  SELECT COLUMN_TYPE INTO parent_type FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=parent_t AND COLUMN_NAME=parent_c;
  SELECT IS_NULLABLE INTO nullable_column FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=t AND COLUMN_NAME=c;
  SET @ddl=CONCAT('ALTER TABLE `',t,'` MODIFY COLUMN `',c,'` ',parent_type,IF(nullable_column='YES',' NULL',' NOT NULL'));
  PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  SET @ddl=CONCAT('ALTER TABLE `',t,'` ADD FOREIGN KEY (`',c,'`) REFERENCES `',parent_t,'` (`',parent_c,'`)');
  PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
 END IF;
END$$
DELIMITER ;
CALL lias_add_fk('utilisateur','equipe_id','equipe','id');
CALL lias_add_fk('membre_equipe','utilisateur_id','utilisateur','id');
CALL lias_add_fk('membre_equipe','equipe_id','equipe','id');
CALL lias_add_fk('mandat','id_membre','utilisateur','id');
CALL lias_add_fk('mandat','equipe_id','equipe','id');
CALL lias_add_fk('historique_affiliation','id_membre','utilisateur','id');
CALL lias_add_fk('publication','id_auteur','utilisateur','id');
CALL lias_add_fk('publication','equipe_id','equipe','id');
CALL lias_add_fk('publication','document_id','document','id_doc');
CALL lias_add_fk('document','id_deposeur','utilisateur','id');
CALL lias_add_fk('document','evenement_id','evenement','id_event');
CALL lias_add_fk('document','reunion_id','reunion','id_reunion');
CALL lias_add_fk('document','precedent_id','document','id_doc');
CALL lias_add_fk('demande_materiel','id_membre','utilisateur','id');
CALL lias_add_fk('demande_materiel','materiel_id','materiel','id');
CALL lias_add_fk('message','id_expediteur','utilisateur','id');
CALL lias_add_fk('message','destinataire_id','utilisateur','id');
CALL lias_add_fk('message','equipe_id','equipe','id');
CALL lias_add_fk('message','evenement_id','evenement','id_event');
CALL lias_add_fk('notification','id_membre','utilisateur','id');
CALL lias_add_fk('reunion','id_organisateur','utilisateur','id');
CALL lias_add_fk('distribution_materiel','acteur_id','utilisateur','id');
CALL lias_add_fk('audit_log','acteur_id','utilisateur','id');
CALL lias_add_fk('demandes_adhesion','decision_par','utilisateur','id');
DROP PROCEDURE lias_add_fk;
