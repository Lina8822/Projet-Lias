# Lias-Jee — Laboratoire LIAS

Application Web de gestion du laboratoire LIAS, Faculté des Sciences Ben M’Sik, Master Data Science & Big Data 2025-2026. Architecture conservée : Jakarta Servlet/JSP, JDBC, MySQL/MariaDB, Java 17 minimum et Tomcat 11 (Servlet 6.1).

## Configuration

Installer un JDK compatible, Maven et Tomcat 11. Le connecteur MySQL 9.7.0 est déjà livré dans `src/main/webapp/WEB-INF/lib`. Configurer l’environnement du processus Tomcat :

```powershell
$env:JAVA_HOME = 'CHEMIN_DU_JDK'
$env:CATALINA_HOME = 'CHEMIN_DE_TOMCAT_11'
$env:LIAS_DB_URL = 'jdbc:mysql://localhost:3306/lias_data_base?connectionTimeZone=UTC&characterEncoding=UTF-8'
$env:LIAS_DB_USER = Read-Host 'Utilisateur MySQL'
$env:LIAS_DB_PASSWORD = Read-Host 'Mot de passe MySQL' -MaskInput
$env:LIAS_UPLOAD_DIR = Join-Path $PWD 'storage/uploads'
```

`-MaskInput` exige PowerShell 7. Sous PowerShell 5.1, fournir le secret dans la configuration locale de Tomcat. `LIAS_JWT_SECRET` peut fournir une clé privée d’au moins 32 caractères ; sans cette variable, une clé aléatoire est générée et les sessions JWT expirent au redémarrage. Ne pas committer les secrets. `LIAS_LEGACY_PASSWORDS=false` désactive la compatibilité temporaire avec les anciens mots de passe en clair.

## Configuration locale et secrets

`.env.example` décrit les variables sans fournir de secret. Copier éventuellement ce fichier vers `.env` pour conserver des notes locales : **Java et Tomcat ne chargent pas automatiquement `.env`**. Définir les variables dans le processus qui lance Tomcat (ou dans sa configuration de service). `.env`, `.env.*` privés, fichiers `*.local.ps1` et identifiants générés dans `build/` sont ignorés par Git.

XAMPP fournit MySQL/MariaDB : démarrer son module MySQL, utiliser le port configuré (3306 par défaut), puis définir `LIAS_DB_URL`, `LIAS_DB_USER` et `LIAS_DB_PASSWORD`. Une installation XAMPP locale neuve utilise souvent `root` sans mot de passe ; si un mot de passe a été configuré, le fournir localement. L’application nécessite Tomcat 11 et Java 17+ comme indiqué ci-dessus.

Les outils de création et de connexion aux comptes de recette exigent `LIAS_TEST_PASSWORD` et, pour le directeur, `LIAS_TEST_DIRECTOR_PASSWORD` (12 caractères minimum). Ils n’ont aucun mot de passe intégré et ne réinitialisent pas les comptes existants. Fournir le mot de passe actuel d’un compte existant ; une rotation éventuelle doit être effectuée séparément. `RepairDevelopmentAuthentication` et `VerifyRealAuthentication` conservent leur entrée existante `LIAS_TEST_PASSWORD` pour le compte qu’ils vérifient. Les scénarios Python utilisant le client MySQL peuvent recevoir son mot de passe via `MYSQL_PWD` dans leur environnement local.

Exemple PowerShell 7, sans mot de passe enregistré dans le fichier :

```powershell
$env:LIAS_DB_PASSWORD = Read-Host 'Mot de passe MySQL local' -MaskInput
$env:LIAS_TEST_PASSWORD = Read-Host 'Mot de passe des comptes de recette' -MaskInput
$env:LIAS_TEST_DIRECTOR_PASSWORD = Read-Host 'Mot de passe du directeur de recette' -MaskInput
```

Les valeurs de test unitaires telles que `old-password` ne sont utilisées pour aucun compte persistant. L’archive historique locale `docs/source-original.zip` est exclue de la publication : elle contient une ancienne clé JWT codée en dur. Les sources actuelles restent toutes incluses. Ne pas réutiliser cette ancienne clé ; si elle a servi à un déploiement, la remplacer et invalider ses anciennes sessions.

## Base de données

Sauvegarder la base et les fichiers avant toute migration. Pour une installation vide, créer `lias_data_base` avec `CHARACTER SET utf8mb4`. Depuis la racine du projet, ouvrir le client MySQL puis exécuter :

```sql
SOURCE database/lias_update.sql;
SOURCE database/lias_integrity.sql;
```

Les chemins SOURCE sont relatifs au dossier du client MySQL. La première migration ajoute les tables et colonnes sans supprimer les données. La seconde complète les clés étrangères avec la règle RESTRICT : une référence orpheline doit être examinée et corrigée explicitement, sans supprimer son historique. Elle adapte les types de référence au parent et est réexécutable après succès.

Sur une ancienne base où `membre_equipe` possède encore la clé primaire composée `(utilisateur_id,equipe_id)`, exécuter une seule fois `database/lias_membership_periods.sql` après sauvegarde. Ne pas exécuter cette migration sur une installation neuve, qui possède déjà `id`. Vérifier avec `SHOW CREATE TABLE membre_equipe`. `lias_final_fixes.sql` est un correctif historique déjà intégré dans `lias_update.sql`.

## Compilation et lancement

```powershell
mvn clean test
mvn clean package
```

WAR Maven : **`target/Lias-Jee.war`**. Le copier dans le dossier `webapps` de Tomcat. URL : **http://localhost:8080/Lias-Jee/**. Eclipse doit utiliser le contexte `Lias-Jee`, et compiler les sources dans `target/classes`.

Alternative PowerShell, avec `JAVA_HOME` et `CATALINA_HOME` configurés :

```powershell
./tools/build.ps1
./tools/run.ps1
```

Le build PowerShell nettoie ses dossiers de compilation et produit `dist/Lias-Jee.war`. Le lancement crée une instance indépendante dans `build/tomcat` ; `-Port 8081` change son port. Les fichiers déposés restent hors du WAR ; sauvegarder ensemble ce stockage et la base.

## Initialisation des comptes

Après compilation, le classpath de maintenance est `target/classes;src/main/webapp/WEB-INF/lib/mysql-connector-j-9.7.0.jar` :

```powershell
$liasClassPath = 'target/classes;src/main/webapp/WEB-INF/lib/mysql-connector-j-9.7.0.jar'
& "$env:JAVA_HOME/bin/java.exe" -cp $liasClassPath com.lias.config.DatabaseTool check
& "$env:JAVA_HOME/bin/java.exe" -cp $liasClassPath com.lias.config.DatabaseTool migrate-passwords
& "$env:JAVA_HOME/bin/java.exe" -cp $liasClassPath com.lias.config.DatabaseTool create-admin EMAIL NOM PRENOM
& "$env:JAVA_HOME/bin/java.exe" -cp $liasClassPath com.lias.config.DatabaseTool initial-director ID AAAA-MM-JJ
```

`create-admin` demande un mot de passe masqué de 12 caractères minimum. `initial-director` exige un membre permanent actif et une date réelle ; les successions suivantes passent par Gouvernance. La migration des mots de passe convertit seulement les valeurs reconnues comme texte brut, conserve les hashes existants et n’affiche aucun secret. Les comptes ADMIN ne valident pas les adhésions.

Les fichiers historiques peuvent être repris par `DatabaseTool import-uploads DOSSIER_ORIGINAL` ; les sources restent conservées. Aucun mot de passe de production ni compte administrateur par défaut n’est fourni.

## Parcours et permissions

- Visiteur : présentation, équipes, annuaire public, événements, publications, candidature avec CV et suivi.
- Permanent : accès selon responsabilité, profil, archives, demandes de matériel et messagerie.
- Associé : consultation, profil, calendrier et participation aux événements par invitation ; publications uniquement après autorisation. Aucun accès aux modules internes.
- Doctorant : profil, ajout et consultation des publications ; modules internes refusés.
- Directeur en mandat actif : adhésions, gouvernance, supervision, matériel et rapport annuel.
- Vice-directeur : supervision et décisions matériel ; adhésions refusées.
- ADMIN : comptes, date du laboratoire, paramètres, gouvernance et audit technique.
- Retraité : compte gelé. Ancien membre : compte désactivé, réactivation possible. Les affiliations et mandats sont clôturés et conservés.

Le rapport annuel est généré côté serveur en HTML et PDF pour l’année choisie, à partir des mêmes requêtes. Les caractères français Windows-1252 sont pris en charge ; les écritures hors de cet alphabet nécessitent une police Unicode. Les notifications sont internes et les décisions de candidature consultables dans leur suivi ; aucun SMTP ni WebSocket n’est requis par le cahier des charges.

## Tests reproductibles et démonstration

Les tests JUnit de base n’exigent pas MySQL. Les six tests JDBC supplémentaires exigent une base dédiée **`lias_test*`**, migrée avec les scripts ci-dessus, et `LIAS_DB_URL`. Ils annulent leurs modifications :

```powershell
$env:LIAS_DB_URL = 'jdbc:mysql://127.0.0.1:33307/lias_test_final_20261005'
mvn clean test
mvn clean package
```

Sans base isolée configurée, ces six tests sont ignorés explicitement ; cela ne constitue pas une validation métier. Le driver livré est ajouté au classpath Surefire.

Pour une base isolée neuve et vide : compiler `tools/SeedIsolated.java` et `tools/CreateTestAccounts.java` avec `javac -encoding UTF-8 -cp target/classes -d build/test-classes`, puis exécuter leurs classes avec le classpath `build/test-classes;target/classes;src/main/webapp/WEB-INF/lib/mysql-connector-j-9.7.0.jar`. `SeedIsolated` refuse toute base non dédiée ou déjà peuplée. `CreateTestAccounts --development-lias_data_base` refuse de réinitialiser un compte existant.

Les identifiants de démonstration générés sont exclusivement de développement, dans `build/test-accounts.properties` (ignoré par Git). Directeur de recette : `directeur.test@lias.example.test`. Autres profils : `recette.admin`, `recette.vice`, `recette.chef`, `recette.member`, `recette.associe`, `recette.doctorant`, `recette.retraite`, `recette.ancien`, suivis de `@lias.example.test`. Ne pas installer ces fixtures en production.

Sur l’application Tomcat pointant vers cette même base, lancer :

```powershell
./tools/verify-current.ps1 -BaseUrl 'http://127.0.0.1:18080/Lias-Jee/' -Database 'lias_test_final_20261005' -DatabasePort 33307 -Python 'CHEMIN_PYTHON'
```

Les scénarios HTTP ajoutent et conservent des données de recette dans la base dédiée. Python nécessite `pypdf` pour la vérification du PDF. Les preuves sont dans `build/current-verification` ; les résultats détaillés et les limites sont dans `docs/FINAL_VERIFICATION.md`.

## Structure

- `src/main/java/com/lias` : configuration, contrôleurs, DAO, modèles, sécurité et services.
- `src/main/webapp/WEB-INF/views` : vues protégées ; `includes` : composants JSP.
- `src/main/webapp/assets` : CSS, JavaScript et logo existants.
- `src/test/java/com/lias` : tests JUnit et test d’intégration Java existant.
- `database` : migrations additives ; `tools` : maintenance, builds et recette.
- `docs/CONFORMITE_2026-10-05.md` : comparaison au PDF et inventaire ; `docs/FINAL_VERIFICATION.md` : preuves finales.

La recette du 5 octobre 2026 utilise une base isolée et Tomcat dédié. Elle ne prouve pas l’application des migrations sur une autre base ni la publication du WAR dans un serveur Eclipse extérieur à ce projet.
