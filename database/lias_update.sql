-- LIAS : extension additive. Sauvegarder la base avant exécution. MySQL 8+.
-- Ne contient aucune suppression de table, de colonne ou de donnée.
SET NAMES utf8mb4;
DELIMITER $$
CREATE PROCEDURE lias_add_column(IN t VARCHAR(64), IN c VARCHAR(64), IN d TEXT) BEGIN IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=t AND column_name=c) THEN SET @ddl=CONCAT('ALTER TABLE `',t,'` ADD COLUMN `',c,'` ',d); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END IF; END$$
DELIMITER ;
CREATE TABLE IF NOT EXISTS `utilisateur` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `nom` VARCHAR(120),
 `prenom` VARCHAR(120),
 `email` VARCHAR(254),
 `mot_de_passe` VARCHAR(255),
 `statut_actuel` VARCHAR(30) DEFAULT 'PERMANENT',
 `role_actuel` VARCHAR(30) DEFAULT 'MEMBRE_EQUIPE',
 `compte_statut` VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
 `photo` VARCHAR(255),
 `etablissement` VARCHAR(255),
 `laboratoire` VARCHAR(255),
 `biographie` TEXT,
 `centre_interet` TEXT,
 `date_naissance` DATE,
 `date_embauche` DATE,
 `date_affiliation` DATE,
 `publication_autorisee` BOOLEAN NOT NULL DEFAULT FALSE,
 `profil_public` BOOLEAN NOT NULL DEFAULT TRUE,
 `equipe_id` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('utilisateur','nom','VARCHAR(120)');
CALL lias_add_column('utilisateur','prenom','VARCHAR(120)');
CALL lias_add_column('utilisateur','email','VARCHAR(254)');
CALL lias_add_column('utilisateur','mot_de_passe','VARCHAR(255)');
CALL lias_add_column('utilisateur','statut_actuel','VARCHAR(30) DEFAULT ''PERMANENT''');
CALL lias_add_column('utilisateur','role_actuel','VARCHAR(30) DEFAULT ''MEMBRE_EQUIPE''');
CALL lias_add_column('utilisateur','compte_statut','VARCHAR(20) NOT NULL DEFAULT ''ACTIF''');
CALL lias_add_column('utilisateur','photo','VARCHAR(255)');
CALL lias_add_column('utilisateur','etablissement','VARCHAR(255)');
CALL lias_add_column('utilisateur','laboratoire','VARCHAR(255)');
CALL lias_add_column('utilisateur','biographie','TEXT');
CALL lias_add_column('utilisateur','centre_interet','TEXT');
CALL lias_add_column('utilisateur','date_naissance','DATE');
CALL lias_add_column('utilisateur','date_embauche','DATE');
CALL lias_add_column('utilisateur','date_affiliation','DATE');
CALL lias_add_column('utilisateur','publication_autorisee','BOOLEAN NOT NULL DEFAULT FALSE');
CALL lias_add_column('utilisateur','profil_public','BOOLEAN NOT NULL DEFAULT TRUE');
CALL lias_add_column('utilisateur','equipe_id','INT NULL');
CREATE TABLE IF NOT EXISTS `equipe` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `nom` VARCHAR(200),
 `description` TEXT,
 `date_creation` DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('equipe','nom','VARCHAR(200)');
CALL lias_add_column('equipe','description','TEXT');
CALL lias_add_column('equipe','date_creation','DATE');
CREATE TABLE IF NOT EXISTS `membre_equipe` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `utilisateur_id` INT,
 `equipe_id` INT,
 `date_debut` DATE,
 `date_fin` DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('membre_equipe','utilisateur_id','INT');
CALL lias_add_column('membre_equipe','equipe_id','INT');
CALL lias_add_column('membre_equipe','date_debut','DATE');
CALL lias_add_column('membre_equipe','date_fin','DATE');
CREATE TABLE IF NOT EXISTS `mandat` (
 `id_mandat` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `id_membre` INT,
 `type_mandat` VARCHAR(30),
 `date_debut` DATE,
 `date_fin` DATE,
 `statut` VARCHAR(20) DEFAULT 'ACTIF',
 `equipe_id` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('mandat','id_membre','INT');
CALL lias_add_column('mandat','type_mandat','VARCHAR(30)');
CALL lias_add_column('mandat','date_debut','DATE');
CALL lias_add_column('mandat','date_fin','DATE');
CALL lias_add_column('mandat','statut','VARCHAR(20) DEFAULT ''ACTIF''');
CALL lias_add_column('mandat','equipe_id','INT NULL');
CREATE TABLE IF NOT EXISTS `historique_affiliation` (
 `id_historique` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `id_membre` INT,
 `ancien_statut` VARCHAR(30),
 `ancien_role` VARCHAR(30),
 `affiliation` VARCHAR(255),
 `date_debut` DATE,
 `date_fin` DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('historique_affiliation','id_membre','INT');
CALL lias_add_column('historique_affiliation','ancien_statut','VARCHAR(30)');
CALL lias_add_column('historique_affiliation','ancien_role','VARCHAR(30)');
CALL lias_add_column('historique_affiliation','affiliation','VARCHAR(255)');
CALL lias_add_column('historique_affiliation','date_debut','DATE');
CALL lias_add_column('historique_affiliation','date_fin','DATE');
CREATE TABLE IF NOT EXISTS `demandes_adhesion` (
 `id_demande` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `nom` VARCHAR(120),
 `prenom` VARCHAR(120),
 `email` VARCHAR(254),
 `motivation` TEXT,
 `date_soumission` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 `etat_demande` VARCHAR(20) DEFAULT 'EN_ATTENTE',
 `etablissement` VARCHAR(255),
 `laboratoire` VARCHAR(255),
 `statut_demande` VARCHAR(30) DEFAULT 'PERMANENT',
 `cv` VARCHAR(255),
 `password_hash` VARCHAR(255),
 `decision_date` TIMESTAMP NULL,
 `decision_par` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('demandes_adhesion','nom','VARCHAR(120)');
CALL lias_add_column('demandes_adhesion','prenom','VARCHAR(120)');
CALL lias_add_column('demandes_adhesion','email','VARCHAR(254)');
CALL lias_add_column('demandes_adhesion','motivation','TEXT');
CALL lias_add_column('demandes_adhesion','date_soumission','TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL lias_add_column('demandes_adhesion','etat_demande','VARCHAR(20) DEFAULT ''EN_ATTENTE''');
CALL lias_add_column('demandes_adhesion','etablissement','VARCHAR(255)');
CALL lias_add_column('demandes_adhesion','laboratoire','VARCHAR(255)');
CALL lias_add_column('demandes_adhesion','statut_demande','VARCHAR(30) DEFAULT ''PERMANENT''');
CALL lias_add_column('demandes_adhesion','cv','VARCHAR(255)');
CALL lias_add_column('demandes_adhesion','password_hash','VARCHAR(255)');
CALL lias_add_column('demandes_adhesion','decision_date','TIMESTAMP NULL');
CALL lias_add_column('demandes_adhesion','decision_par','INT NULL');
CREATE TABLE IF NOT EXISTS `evenement` (
 `id_event` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `titre` VARCHAR(255),
 `type_event` VARCHAR(40),
 `date_event` DATETIME,
 `date_fin` DATETIME,
 `lieu` VARCHAR(255),
 `description` TEXT,
 `edition` VARCHAR(100),
 `statut` VARCHAR(20) DEFAULT 'PLANIFIE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('evenement','titre','VARCHAR(255)');
CALL lias_add_column('evenement','type_event','VARCHAR(40)');
CALL lias_add_column('evenement','date_event','DATETIME');
CALL lias_add_column('evenement','date_fin','DATETIME');
CALL lias_add_column('evenement','lieu','VARCHAR(255)');
CALL lias_add_column('evenement','description','TEXT');
CALL lias_add_column('evenement','edition','VARCHAR(100)');
CALL lias_add_column('evenement','statut','VARCHAR(20) DEFAULT ''PLANIFIE''');
CREATE TABLE IF NOT EXISTS `publication` (
 `id_pub` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `id_auteur` INT,
 `titre` VARCHAR(255),
 `auteurs` TEXT,
 `journal_conference` VARCHAR(255),
 `annee` INT,
 `lien_pdf` VARCHAR(1000),
 `doi` VARCHAR(255),
 `equipe_id` INT NULL,
 `document_id` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('publication','id_auteur','INT');
CALL lias_add_column('publication','titre','VARCHAR(255)');
CALL lias_add_column('publication','auteurs','TEXT');
CALL lias_add_column('publication','journal_conference','VARCHAR(255)');
CALL lias_add_column('publication','annee','INT');
CALL lias_add_column('publication','lien_pdf','VARCHAR(1000)');
CALL lias_add_column('publication','doi','VARCHAR(255)');
CALL lias_add_column('publication','equipe_id','INT NULL');
CALL lias_add_column('publication','document_id','INT NULL');
CREATE TABLE IF NOT EXISTS `document` (
 `id_doc` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `titre` VARCHAR(255),
 `type_doc` VARCHAR(40),
 `nom_fichier` VARCHAR(255),
 `id_deposeur` INT,
 `date_depot` DATETIME DEFAULT CURRENT_TIMESTAMP,
 `evenement_id` INT NULL,
 `reunion_id` INT NULL,
 `version` INT NOT NULL DEFAULT 1,
 `precedent_id` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('document','titre','VARCHAR(255)');
CALL lias_add_column('document','type_doc','VARCHAR(40)');
CALL lias_add_column('document','nom_fichier','VARCHAR(255)');
CALL lias_add_column('document','id_deposeur','INT');
CALL lias_add_column('document','date_depot','DATETIME DEFAULT CURRENT_TIMESTAMP');
CALL lias_add_column('document','evenement_id','INT NULL');
CALL lias_add_column('document','reunion_id','INT NULL');
CALL lias_add_column('document','version','INT NOT NULL DEFAULT 1');
CALL lias_add_column('document','precedent_id','INT NULL');
CREATE TABLE IF NOT EXISTS `demande_materiel` (
 `id_demande_mat` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `id_membre` INT,
 `nom_materiel` VARCHAR(255),
 `quantite` INT,
 `description` TEXT,
 `etat_demande` VARCHAR(20) DEFAULT 'EN_ATTENTE',
 `date_demande` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 `materiel_id` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('demande_materiel','id_membre','INT');
CALL lias_add_column('demande_materiel','nom_materiel','VARCHAR(255)');
CALL lias_add_column('demande_materiel','quantite','INT');
CALL lias_add_column('demande_materiel','description','TEXT');
CALL lias_add_column('demande_materiel','etat_demande','VARCHAR(20) DEFAULT ''EN_ATTENTE''');
CALL lias_add_column('demande_materiel','date_demande','TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL lias_add_column('demande_materiel','materiel_id','INT NULL');
CREATE TABLE IF NOT EXISTS `partenaire` (
 `id_partenaire` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `nom_institution` VARCHAR(255),
 `type_convention` VARCHAR(100),
 `date_signature` DATE,
 `description` TEXT,
 `nom` VARCHAR(255),
 `pays` VARCHAR(120),
 `contact` VARCHAR(255),
 `email` VARCHAR(254),
 `telephone` VARCHAR(40)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('partenaire','nom_institution','VARCHAR(255)');
CALL lias_add_column('partenaire','type_convention','VARCHAR(100)');
CALL lias_add_column('partenaire','date_signature','DATE');
CALL lias_add_column('partenaire','description','TEXT');
CALL lias_add_column('partenaire','nom','VARCHAR(255)');
CALL lias_add_column('partenaire','pays','VARCHAR(120)');
CALL lias_add_column('partenaire','contact','VARCHAR(255)');
CALL lias_add_column('partenaire','email','VARCHAR(254)');
CALL lias_add_column('partenaire','telephone','VARCHAR(40)');
CREATE TABLE IF NOT EXISTS `reunion` (
 `id_reunion` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `titre` VARCHAR(255),
 `date_reunion` DATETIME,
 `lieu` VARCHAR(255),
 `ordre_du_jour` TEXT,
 `contenu` TEXT,
 `nom_fichier_pv` VARCHAR(255),
 `id_organisateur` INT,
 `date_creation` DATE,
 `statut` VARCHAR(20) DEFAULT 'PLANIFIE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('reunion','titre','VARCHAR(255)');
CALL lias_add_column('reunion','date_reunion','DATETIME');
CALL lias_add_column('reunion','lieu','VARCHAR(255)');
CALL lias_add_column('reunion','ordre_du_jour','TEXT');
CALL lias_add_column('reunion','contenu','TEXT');
CALL lias_add_column('reunion','nom_fichier_pv','VARCHAR(255)');
CALL lias_add_column('reunion','id_organisateur','INT');
CALL lias_add_column('reunion','date_creation','DATE');
CALL lias_add_column('reunion','statut','VARCHAR(20) DEFAULT ''PLANIFIE''');
CREATE TABLE IF NOT EXISTS `message` (
 `id_message` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `id_expediteur` INT,
 `contenu` TEXT,
 `date_envoi` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 `destinataire_id` INT NULL,
 `equipe_id` INT NULL,
 `evenement_id` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('message','id_expediteur','INT');
CALL lias_add_column('message','contenu','TEXT');
CALL lias_add_column('message','date_envoi','TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL lias_add_column('message','destinataire_id','INT NULL');
CALL lias_add_column('message','equipe_id','INT NULL');
CALL lias_add_column('message','evenement_id','INT NULL');
CREATE TABLE IF NOT EXISTS `notification` (
 `id_notif` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `id_membre` INT NULL,
 `message` TEXT,
 `type_notif` VARCHAR(40),
 `date_creation` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 `est_lu` BOOLEAN DEFAULT FALSE,
 `lien` VARCHAR(255) DEFAULT 'dashboard.jsp'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('notification','id_membre','INT NULL');
CALL lias_add_column('notification','message','TEXT');
CALL lias_add_column('notification','type_notif','VARCHAR(40)');
CALL lias_add_column('notification','date_creation','TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL lias_add_column('notification','est_lu','BOOLEAN DEFAULT FALSE');
CALL lias_add_column('notification','lien','VARCHAR(255) DEFAULT ''dashboard.jsp''');
CREATE TABLE IF NOT EXISTS `convention` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `titre` VARCHAR(255),
 `partenaire_id` INT,
 `date_debut` DATE,
 `date_fin` DATE,
 `description` TEXT,
 `document_id` INT NULL,
 `etat` VARCHAR(20) DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('convention','titre','VARCHAR(255)');
CALL lias_add_column('convention','partenaire_id','INT');
CALL lias_add_column('convention','date_debut','DATE');
CALL lias_add_column('convention','date_fin','DATE');
CALL lias_add_column('convention','description','TEXT');
CALL lias_add_column('convention','document_id','INT NULL');
CALL lias_add_column('convention','etat','VARCHAR(20) DEFAULT ''ACTIVE''');
CREATE TABLE IF NOT EXISTS `convention_activite` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `convention_id` INT,
 `titre` VARCHAR(255),
 `date_activite` DATE,
 `description` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('convention_activite','convention_id','INT');
CALL lias_add_column('convention_activite','titre','VARCHAR(255)');
CALL lias_add_column('convention_activite','date_activite','DATE');
CALL lias_add_column('convention_activite','description','TEXT');
CREATE TABLE IF NOT EXISTS `materiel` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `nom` VARCHAR(255),
 `quantite` INT NOT NULL DEFAULT 0,
 `etat` VARCHAR(30) DEFAULT 'DISPONIBLE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('materiel','nom','VARCHAR(255)');
CALL lias_add_column('materiel','quantite','INT NOT NULL DEFAULT 0');
CALL lias_add_column('materiel','etat','VARCHAR(30) DEFAULT ''DISPONIBLE''');
CREATE TABLE IF NOT EXISTS `arrivage` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `materiel_id` INT,
 `quantite` INT,
 `date_arrivage` DATETIME DEFAULT CURRENT_TIMESTAMP,
 `acteur_id` INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('arrivage','materiel_id','INT');
CALL lias_add_column('arrivage','quantite','INT');
CALL lias_add_column('arrivage','date_arrivage','DATETIME DEFAULT CURRENT_TIMESTAMP');
CALL lias_add_column('arrivage','acteur_id','INT');
CREATE TABLE IF NOT EXISTS `distribution_materiel` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `materiel_id` INT,
 `membre_id` INT,
 `quantite` INT,
 `date_distribution` DATETIME DEFAULT CURRENT_TIMESTAMP,
 `acteur_id` INT,
 `demande_id` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('distribution_materiel','materiel_id','INT');
CALL lias_add_column('distribution_materiel','membre_id','INT');
CALL lias_add_column('distribution_materiel','quantite','INT');
CALL lias_add_column('distribution_materiel','date_distribution','DATETIME DEFAULT CURRENT_TIMESTAMP');
CALL lias_add_column('distribution_materiel','acteur_id','INT');
CALL lias_add_column('distribution_materiel','demande_id','INT NULL');
CREATE TABLE IF NOT EXISTS `evenement_membre` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `evenement_id` INT,
 `membre_id` INT,
 `responsabilite` VARCHAR(30),
 UNIQUE KEY (`evenement_id`,`membre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('evenement_membre','evenement_id','INT');
CALL lias_add_column('evenement_membre','membre_id','INT');
CALL lias_add_column('evenement_membre','responsabilite','VARCHAR(30)');
CREATE TABLE IF NOT EXISTS `reunion_participant` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `reunion_id` INT,
 `membre_id` INT,
 UNIQUE KEY (`reunion_id`,`membre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('reunion_participant','reunion_id','INT');
CALL lias_add_column('reunion_participant','membre_id','INT');
CREATE TABLE IF NOT EXISTS `lecture_message` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `message_id` INT,
 `membre_id` INT,
 `date_lecture` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY (`message_id`,`membre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('lecture_message','message_id','INT');
CALL lias_add_column('lecture_message','membre_id','INT');
CALL lias_add_column('lecture_message','date_lecture','TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CREATE TABLE IF NOT EXISTS `notification_lecture` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `notification_id` INT,
 `membre_id` INT,
 `date_lecture` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY (`notification_id`,`membre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('notification_lecture','notification_id','INT');
CALL lias_add_column('notification_lecture','membre_id','INT');
CALL lias_add_column('notification_lecture','date_lecture','TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CREATE TABLE IF NOT EXISTS `notification_candidat` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `demande_id` INT,
 `email` VARCHAR(254),
 `message` TEXT,
 `date_creation` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('notification_candidat','demande_id','INT');
CALL lias_add_column('notification_candidat','email','VARCHAR(254)');
CALL lias_add_column('notification_candidat','message','TEXT');
CALL lias_add_column('notification_candidat','date_creation','TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CREATE TABLE IF NOT EXISTS `audit_log` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `acteur_id` INT NULL,
 `action` VARCHAR(120),
 `entite` VARCHAR(60),
 `entite_id` INT,
 `avant` LONGTEXT,
 `apres` LONGTEXT,
 `date_creation` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('audit_log','acteur_id','INT NULL');
CALL lias_add_column('audit_log','action','VARCHAR(120)');
CALL lias_add_column('audit_log','entite','VARCHAR(60)');
CALL lias_add_column('audit_log','entite_id','INT');
CALL lias_add_column('audit_log','avant','LONGTEXT');
CALL lias_add_column('audit_log','apres','LONGTEXT');
CALL lias_add_column('audit_log','date_creation','TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CREATE TABLE IF NOT EXISTS `parametre` (
 `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 `cle` VARCHAR(100),
 `valeur` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lias_add_column('parametre','cle','VARCHAR(100)');
CALL lias_add_column('parametre','valeur','TEXT');
CALL lias_add_column('convention','legacy_partenaire_id','INT NULL');
DROP PROCEDURE lias_add_column;
ALTER TABLE utilisateur MODIFY COLUMN mot_de_passe VARCHAR(255);
ALTER TABLE historique_affiliation MODIFY COLUMN date_debut DATE NULL, MODIFY COLUMN date_fin DATE NULL;
INSERT INTO historique_affiliation(id_membre,ancien_statut,ancien_role,affiliation,date_debut) SELECT id,statut_actuel,role_actuel,'LIAS',date_affiliation FROM utilisateur u WHERE statut_actuel IN ('PERMANENT','ASSOCIE','DOCTORANT') AND NOT EXISTS (SELECT 1 FROM historique_affiliation h WHERE h.id_membre=u.id);
-- Les dates inconnues restent NULL : aucune date historique n'est inventée.
-- Initialiser les mandats réels et corriger les anciens libellés via tools/DatabaseTool avant ouverture.

-- Harmonisation de types nécessaires pour les nouveaux statuts et les horaires.
ALTER TABLE utilisateur MODIFY COLUMN statut_actuel VARCHAR(30) DEFAULT 'PERMANENT';
ALTER TABLE utilisateur MODIFY COLUMN role_actuel VARCHAR(30) DEFAULT 'MEMBRE_EQUIPE';
ALTER TABLE utilisateur MODIFY COLUMN compte_statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF';
ALTER TABLE mandat MODIFY COLUMN type_mandat VARCHAR(30);
ALTER TABLE mandat MODIFY COLUMN statut VARCHAR(20) DEFAULT 'ACTIF';
ALTER TABLE evenement MODIFY COLUMN date_event DATETIME;
ALTER TABLE reunion MODIFY COLUMN date_reunion DATETIME;
ALTER TABLE demandes_adhesion MODIFY COLUMN etat_demande VARCHAR(20) DEFAULT 'EN_ATTENTE';
ALTER TABLE demande_materiel MODIFY COLUMN etat_demande VARCHAR(20) DEFAULT 'EN_ATTENTE';

-- Reprise des conventions présentes dans l'ancien modèle partenaire/convention combiné.
-- La date de fin inconnue reste NULL. La clé de reprise évite les doublons au second passage.
INSERT INTO convention(titre,partenaire_id,date_debut,description,etat,legacy_partenaire_id)
SELECT p.type_convention,p.id_partenaire,p.date_signature,p.description,'ACTIVE',p.id_partenaire
FROM partenaire p WHERE p.type_convention IS NOT NULL AND p.type_convention<>'' AND p.date_signature IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM convention c WHERE c.legacy_partenaire_id=p.id_partenaire);
-- Reprise uniquement des affectations non ambiguës ; les autres lignes historiques restent intactes.
UPDATE utilisateur u JOIN (SELECT utilisateur_id,MIN(equipe_id) equipe_id FROM membre_equipe WHERE date_fin IS NULL GROUP BY utilisateur_id HAVING COUNT(DISTINCT equipe_id)=1) m ON m.utilisateur_id=u.id SET u.equipe_id=m.equipe_id WHERE u.equipe_id IS NULL;
-- Conservation des états de lecture personnels existants.
INSERT INTO notification_lecture(notification_id,membre_id)
SELECT id_notif,id_membre FROM notification WHERE est_lu=TRUE AND id_membre IS NOT NULL
ON DUPLICATE KEY UPDATE membre_id=VALUES(membre_id);

-- Les bases Eclipse historiques utilisent parfois BIGINT pour utilisateur.id.
-- Conserver les clés étrangères en alignant le type des références sur le parent.
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
CALL lias_add_fk('convention','partenaire_id','partenaire','id_partenaire');
CALL lias_add_fk('convention','document_id','document','id_doc');
CALL lias_add_fk('convention_activite','convention_id','convention','id');
CALL lias_add_fk('arrivage','materiel_id','materiel','id');
CALL lias_add_fk('arrivage','acteur_id','utilisateur','id');
CALL lias_add_fk('distribution_materiel','materiel_id','materiel','id');
CALL lias_add_fk('distribution_materiel','membre_id','utilisateur','id');
CALL lias_add_fk('distribution_materiel','demande_id','demande_materiel','id_demande_mat');
CALL lias_add_fk('evenement_membre','evenement_id','evenement','id_event');
CALL lias_add_fk('evenement_membre','membre_id','utilisateur','id');
CALL lias_add_fk('reunion_participant','reunion_id','reunion','id_reunion');
CALL lias_add_fk('reunion_participant','membre_id','utilisateur','id');
CALL lias_add_fk('lecture_message','message_id','message','id_message');
CALL lias_add_fk('lecture_message','membre_id','utilisateur','id');
CALL lias_add_fk('notification_lecture','notification_id','notification','id_notif');
CALL lias_add_fk('notification_lecture','membre_id','utilisateur','id');
CALL lias_add_fk('notification_candidat','demande_id','demandes_adhesion','id_demande');
DROP PROCEDURE lias_add_fk;


-- Compatibilité ancien partenaire/convention combiné : conserver les données, autoriser un partenaire sans convention.
ALTER TABLE partenaire MODIFY COLUMN type_convention VARCHAR(100) NULL, MODIFY COLUMN date_signature DATE NULL;
