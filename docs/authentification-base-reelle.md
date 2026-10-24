# Authentification réparée sur la base réelle — 10 septembre 2026

## Résultat réel

Serveur interrogé : Tomcat Eclipse 11.0.22 / Java 26.0.1, `http://localhost:8080/Lias-Jee`.
Le POST `/LoginServlet` renvoie 302 vers `dashboard.jsp`. Avec les cookies de cette réponse, GET `/dashboard` et `/dashboard.jsp` renvoient 200. Les attributs de session ont été lus via le connecteur JMX local du Tomcat existant : `id=4`, `role=DIRECTEUR`, `statut=PERMANENT`. Aucune route de diagnostic n'a été ajoutée à l'application.

Compte réellement créé :
- email : `directeur.test@lias.example.test`
- mot de passe : fourni localement via `LIAS_TEST_DIRECTOR_PASSWORD` ; ancienne valeur retirée avant publication. Ce compte de développement doit changer de mot de passe s’il existe encore.
- identifiant utilisateur : 4
- rôle effectif : DIRECTEUR
- statut : PERMANENT
- compte_statut : ACTIF
- mandat : id 3, DIRECTEUR, ACTIF, début 2026-09-10, sans date de fin.

Le cookie JWT est HttpOnly, signé et valide ; ses claims contiennent l'identifiant, DIRECTEUR et PERMANENT. Le cookie JSESSIONID existe. Le dashboard répond aussi 200 avec la session seule après retrait du cookie JWT.

## Causes

1. `director1788985651828@example.test` n'existe pas dans `lias_data_base`. Il provenait des scénarios sur une base isolée et n'était pas un identifiant utilisable sur la base Eclipse.
2. Les anciens mots de passe étaient en texte clair. Passwords.verify n'acceptait que PBKDF2-HMAC-SHA256, pas BCrypt. Les vrais comptes étaient donc refusés avant même leur chargement complet.
3. La table réelle utilisateur ne contenait pas encore compte_statut et plusieurs autres colonnes attendues. Des tables nécessaires au dashboard manquaient aussi.
4. Le mandat existant du directeur utilisait le libellé `Directeur`, alors que les contrôles Java attendent `DIRECTEUR`.

## Connexion et schéma

Base effective : `lias_data_base`, sur MySQL/MariaDB local :3306. Utilisateur JDBC observé : `root@localhost`. Mot de passe JDBC configuré par défaut : vide. Pilote : `com.mysql.cj.jdbc.Driver`, mysql-connector-j-9.7.0.jar.
URL : `jdbc:mysql://localhost:3306/lias_data_base?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`.
Aucune surcharge LIAS_DB_URL dans les propriétés de la JVM Tomcat. La création du compte dans cette base puis sa connexion HTTP réussie confirme que l'application lit cette base. Le fichier de connexions MySQL Workbench n'était pas disponible ; le schéma sélectionné dans son interface n'a pas été observé.

Table : utilisateur. Colonnes d'identité : id, nom, prenom, email, mot_de_passe. Colonnes utilisées pour les droits : statut_actuel, role_actuel, compte_statut, publication_autorisee. Les rôles effectifs proviennent des mandats actifs et en cours (table mandat : id_membre, type_mandat, date_debut, date_fin, statut).

## Corrections

- `src/main/java/com/lias/security/Passwords.java` : vérification temporaire des anciens mots de passe en clair, comparaison à temps constant. Les formats de hash reconnaissables sont exclus de cette compatibilité ; un hash n'est jamais accepté comme mot de passe en clair.
- `src/main/java/com/lias/dao/AccountDAO.java` : sélection paramétrée par email avec verrou transactionnel, vérification du mot de passe puis du compte actif, rehash PBKDF2 uniquement après une authentification réussie. Normalisation en mémoire des anciens libellés de mandat (Directeur, Chef d'équipe…). Aucune responsabilité existante supprimée ou réattribuée.
- `src/test/java/com/lias/LaunchRegressionTest.java` : test supplémentaire de la compatibilité legacy et de la vérification PBKDF2.
- `tools/RepairDevelopmentAuthentication.java` : création transactionnelle du seul compte de test demandé, avec contrôle préalable du schéma, hash PBKDF2, mandat actif et entrée dans audit_log. Ne modifie aucun compte existant.
- `tools/VerifyRealAuthentication.java` : tests du vrai serveur, contrôle JWT/session, mauvaises informations et anciens comptes, sans afficher les mots de passe existants.

Les corrections Java sont synchronisées dans le workspace et dans `C:/Users/ELECTRO-RIMO/Downloads/Lias-Jee`, véritable dossier du projet Eclipse. Les nouvelles classes sont publiées dans le contexte Eclipse tmp0/wtpwebapps/Lias-Jee. Le WAR est régénéré dans target/Lias-Jee.war.

LoginServlet, MembreDAO, DBConnection, JwtUtil, Membre, le formulaire login.jsp, JwtFilter et Access ont été contrôlés. Le formulaire envoie bien email/password à LoginServlet ; MembreDAO.authentifier délègue à AccountDAO ; LoginServlet vérifie Access.active, crée la session, renouvelle son identifiant, renseigne user/id/role/statut, crée le JWT puis redirige. Ces éléments n'avaient pas besoin d'un nouveau contournement.

La compatibilité legacy peut être désactivée avec `LIAS_LEGACY_PASSWORDS=false` après migration des comptes restants.

## Opérations SQL exécutées

Après accord explicite de l'utilisateur, sauvegarde complète par mysqldump, puis exécution de `database/lias_update.sql`. Ce fichier contient les instructions exactes : CREATE TABLE IF NOT EXISTS, ajouts de colonnes absentes, adaptations de types, clés étrangères compatibles, reprise des affiliations/conventions/lectures et affectations non ambiguës. Aucune instruction de suppression de données utilisateur. Le journal schema-migration.log est sans erreur.

Création du test avec paramètres JDBC :

```sql
SELECT id,email,mot_de_passe,statut_actuel,role_actuel,compte_statut FROM utilisateur LIMIT 0;
SELECT id_mandat,id_membre,type_mandat,date_debut,date_fin,statut FROM mandat LIMIT 0;
SELECT id FROM utilisateur WHERE email=?;
INSERT INTO utilisateur(nom,prenom,email,mot_de_passe,statut_actuel,role_actuel,compte_statut,date_affiliation)
VALUES (?,?,?,?,'PERMANENT','DIRECTEUR','ACTIF',CURRENT_DATE);
INSERT INTO mandat(id_membre,type_mandat,date_debut,statut)
VALUES (?,'DIRECTEUR',CURRENT_DATE,'ACTIF');
INSERT INTO audit_log(action,entite,entite_id,apres)
VALUES ('COMPTE_TEST_DEVELOPPEMENT','utilisateur',?,'Compte test directeur demandé par utilisateur');
```

Les paramètres de l'utilisateur créé sont Test / Directeur / directeur.test@lias.example.test / hash PBKDF2 (jamais le mot de passe en clair). Le mandat et le journal référencent l'identifiant 4.

À chaque connexion :

```sql
SELECT * FROM utilisateur WHERE email=? FOR UPDATE;
SELECT * FROM utilisateur WHERE id=?;
SELECT type_mandat FROM mandat
WHERE id_membre=? AND statut='ACTIF' AND date_debut<=CURRENT_DATE
AND (date_fin IS NULL OR date_fin>=CURRENT_DATE)
ORDER BY FIELD(type_mandat,'DIRECTEUR','VICE_DIRECTEUR','CHEF_EQUIPE','MEMBRE_EQUIPE');
-- Uniquement après vérification réussie d'un ancien mot de passe et du compte actif :
UPDATE utilisateur SET mot_de_passe=? WHERE id=?;
```

Les UPDATE de mot de passe ont concerné les comptes 1 et 2 lors de leurs connexions HTTP réussies. Les nouveaux hashes ont été revérifiés avec leurs mots de passe précédents en mémoire, sans les afficher ni les changer. Le compte 3, dépourvu de statut et rôle, reste refusé et n'a pas été modifié. Les trois comptes originaux et leurs deux mandats sont conservés ; seul le compte de test et son mandat ont été ajoutés.

La conversion globale initialement envisagée a été refusée par l'approbation automatique et n'a pas été exécutée. Elle a été remplacée par la migration à la connexion explicitement prévue dans la demande.

## Vérifications

- Maven clean/test/package : succès ; 7 tests, zéro échec et zéro erreur.
- POST test valide : 302 ; Location dashboard.jsp.
- GET dashboard et dashboard.jsp : 200.
- Session réelle : id=4, rôle DIRECTEUR, statut PERMANENT.
- JWT signé valide, HttpOnly ; session seule également utilisable.
- Mauvais mot de passe : 200 sur formulaire avec « Email ou mot de passe incorrect, ou compte restreint. », aucune 500.
- Ancien identifiant de base isolée : message de refus, aucune 500.
- Comptes réels préexistants 1 et 2 : POST 302 et dashboard 200, rehash à la connexion confirmé.

Journaux : `build/verification/auth-real/http-real.log`, `test-account.log`, `schema-migration.log`, `maven.log`. Sauvegarde locale : before-approved.sql, dans le même répertoire.

Dans Eclipse, F5 actualise les sources modifiées extérieurement. Le serveur a déjà rechargé la correction ; aucune migration supplémentaire n'est nécessaire pour le compte de test indiqué.
