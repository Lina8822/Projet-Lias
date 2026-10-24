# Correction du lancement Eclipse / Tomcat — 10 septembre 2026

Les fichiers du workspace sont corrigés. La compilation Java et JSP et le déploiement du WAR en environnement de test sont validés. L'application de la migration sur la base réelle `lias_data_base` attend l'accord de l'utilisateur ; la validation sur le serveur Eclipse :8080 reste donc à faire après migration et republication.

## Cause de la HTTP 500

`PageServlet.render`, ligne 10, appelait `JwtFilter.module(String)`, méthode absente. Aucun appel `module(...)` n'existait dans le filtre lui-même. Eclipse avait publié une classe contenant une erreur de compilation différée (`java.lang.Error: Unresolved compilation problem`).

La résolution des pages appartient au routage, pas à l'authentification. L'appel est remplacé par `PageRoutes.key`, qui accepte exclusivement les pages connues, avec ou sans `.jsp`, et associe la racine à `index`. Les autres appels `module(...)` trouvés sont valides : méthode de rendu de PageServlet et méthode abstraite sans argument d'ActionServlet, implémentée par CompteServlet et NotificationServlet.

## Fichiers et logiques modifiés

- `src/main/java/com/lias/controller/PageServlet.java`, lignes 4 et 10 : mappings des URL sans extension et de la racine ; résolution explicite ; page inconnue 404 ; refus d'accès 403 avant les accès SQL et le forward. Le forward reste limité aux vues fixes `/WEB-INF/views/index.jsp`, `login.jsp`, `candidature.jsp` et `portal.jsp`. GET et POST conservent le rendu existant.
- `src/main/java/com/lias/security/PageRoutes.java` : registre des 21 pages et résolution des URL.
- `src/main/java/com/lias/security/JwtFilter.java`, lignes 17–68 : routes publiques conformes à Access, contrôle JWT, identité positive, utilisateur rechargé par AccountDAO, compte actif et mandat courant, synchronisation complète de session, autorisation des pages et actions, traitement des tokens invalides et erreurs JDBC. Les ressources statiques publiques sont limitées au répertoire assets. Les droits ne viennent plus d'un objet partiel reconstitué depuis des claims anciens.
- `src/main/java/com/lias/security/JwtUtil.java`, lignes 98–104 : conservation des segments vides pour rejeter les JWT mal formés et validation de l'en-tête HS256 attendu. Signature et expiration sont conservées.
- `src/main/java/com/lias/security/Access.java`, ligne 6 : un statut null ne déclenche plus NullPointerException.
- `src/main/java/com/lias/model/Membre.java`, lignes 10–12 : propriété publicationAutorisee et accesseurs attendus par AccountDAO et Access. Il s'agit de la permission existante en base, pas d'un contournement.
- `src/main/java/com/lias/config/DBConnection.java`, lignes 5–13 : résolution des paramètres système/environnement, dont LIAS_UPLOAD_DIR via FilesService ; connexion JDBC non null ou SQLException explicite. Les valeurs par défaut Eclipse sont conservées.
- `src/main/java/com/lias/dao/MembreDAO.java`, méthode authentifier : délégation à AccountDAO au lieu de la comparaison SQL de mots de passe en clair et d'une connexion non fermée.
- `src/main/java/com/lias/controller/LoginServlet.java`, lignes 40–42 et 85 : authentification par le DAO actuel, contrôle du compte actif, erreur SQL 503 et renouvellement de l'identifiant de session.
- `.settings/org.eclipse.wst.common.component` : sortie corrigée de `/LaboJEE/build/classes` vers `/Lias-Jee/target/classes` ; retrait des sources de test du déploiement web.
- `pom.xml` : ajout de JUnit Jupiter et Surefire pour exécuter réellement les tests de régression. Servlet 6.1 reste fourni par Tomcat. Le pilote MySQL 9.7.0 déjà présent dans WEB-INF/lib est inclus dans le WAR.
- `src/test/java/com/lias/LaunchRegressionTest.java` : cinq tests couvrant routes, mappings, JWT, statuts, permissions et filtre.
- `tools/verify-launch.ps1` : vérification HTTP reproductible avec les comptes de test ; URL du serveur paramétrable.
- `database/lias_update.sql` : migration compatible avec les identifiants BIGINT de la base Eclipse ; types des références alignés sur leurs parents avant création des clés étrangères ; dates d'affiliation inconnues autorisées sans inventer d'historique. Aucune table ni fonctionnalité supprimée.

## Vérifications exécutées

- JDK utilisé : 26.0.1 ; compilation Java cible 17, compatible avec Tomcat 11.
- `mvn clean` : BUILD SUCCESS.
- `mvn test` : BUILD SUCCESS ; 5 tests, 0 échec, 0 erreur, 0 ignoré.
- `mvn package` puis reconstruction finale : BUILD SUCCESS ; WAR `target/Lias-Jee.war`.
- Compilation indépendante par ECJ, compilateur Java Eclipse : 0 erreur, 10 avertissements (serialVersionUID absents et imports inutilisés).
- Jasper fourni par Tomcat 11.0.22 : 26 JSP compilées, 0 erreur.
- Test métier existant `IntegrationTest.main` exécuté séparément sur `lias_test` : 36 assertions réussies. Cette classe n'est pas un test JUnit et n'est pas comptée dans les cinq tests Maven.
- WAR final réellement déployé sur Tomcat 11.0.22 / JDK 26.0.1, contexte `/Lias-Jee`, port isolé 18083.
- 102 vérifications HTTP réussies, plus les cinq connexions : sept pages publiques et dix-neuf routes pour directeur, membre, doctorant, associé et administrateur. Codes 200 ou 403 vérifiés selon le profil. `/dashboard` et `/dashboard.jsp` : 200 pour chacun des cinq profils.
- Routes demandées : `/`, `/login`, `/membres`, `/publications`, `/evenements` publiques 200 ; `/dashboard` authentifié 200 ; `/demandes` directeur 200, autres profils 403 ; `/gouvernance` directeur/admin 200, autres profils 403. Sans utilisateur, les routes privées redirigent vers le login (test du filtre).
- Inspection du WAR : PageServlet.class, JwtFilter.class et pilote MySQL présents ; API Servlet fournie par Tomcat et classes de test absentes du WAR.
- Recherche globale `module(` : plus aucun appel à `JwtFilter.module`.

Les journaux sont conservés dans `build/verification` : `mvn-clean.log`, `mvn-test.log`, `mvn-package.log`, `mvn-final.log`, `ecj.log`, `jsp-compile.log`, `integration.log`, `http-results.json`, `http-final.log`, `migration-final.log`.

## Base Eclipse : étape restante

Le contrôle en lecture seule de `lias_data_base` a confirmé un ancien schéma (notamment absence de parametre et audit_log) et trois mots de passe non migrés. Une sauvegarde locale a été réalisée et la migration corrigée a été exécutée sans erreur sur une copie fraîche `lias_test_eclipse_final`, sur le serveur MySQL de test :33307. Le hachage des mots de passe a aussi été essayé sur une autre copie. La base d'origine sur :3306 n'a pas été modifiée.

L'accord demandé porte sur l'exécution de `database/lias_update.sql` et `DatabaseTool migrate-passwords` sur la base d'origine. Les mots de passe saisis par les utilisateurs ne changent pas. Un compte ancien n'a aucun statut défini : son statut doit être renseigné avec une valeur réelle avant de pouvoir se connecter. Les responsabilités sont calculées d'après les mandats actifs ; aucune date de mandat n'est inventée.

## Dans Eclipse après modification

1. Arrêter Tomcat dans Servers.
2. Sélectionner le projet, F5 (Refresh).
3. Maven > Update Project… (Alt+F5), puis Project > Clean… pour Lias-Jee.
4. Vérifier JDK 26.0.1 pour le lancement Tomcat et Tomcat 11.0.22 dans Targeted Runtimes. La conformité Java du projet reste 17.
5. Dans Servers : Clean…, puis Publish ; si les anciens fichiers persistent, retirer et réajouter Lias-Jee au serveur, puis republier.
6. Après migration de la base, redémarrer Tomcat et se reconnecter pour renouveler le JWT/session.
7. Tester `http://localhost:8080/Lias-Jee/login`, puis `/dashboard`.

La disparition des marqueurs de l'interface Eclipse n'a pas été observée directement ; les sources ont été compilées par Maven et par ECJ. Les réponses 200 sont celles du WAR de test avec base migrée, pas une affirmation de republication automatique sur le serveur Eclipse.
