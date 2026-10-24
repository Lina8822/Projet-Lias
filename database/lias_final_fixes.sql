-- Recette finale : partenaire et convention sont désormais deux entités distinctes.
-- Les anciennes valeurs restent intactes ; un partenaire peut exister avant signature.
ALTER TABLE partenaire MODIFY COLUMN type_convention VARCHAR(100) NULL,
                       MODIFY COLUMN date_signature DATE NULL;
