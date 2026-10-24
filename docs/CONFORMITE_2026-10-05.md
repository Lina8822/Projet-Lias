# Conformité PDF ↔ code — Lias-Jee

L’analyse porte sur le PDF officiel de huit pages, les contrôleurs, services, DAO, modèles, filtres, vues JSP, JavaScript, CSS, migrations SQL, configuration, tests et outils. L’inventaire automatisé des 109 fichiers sources et de leurs empreintes est conservé dans `build/current-verification/source-inventory.json`.

Les modules existent déjà : ils ne sont pas déclarés absents parce qu’un test manque. Les points PARTIEL ci-dessous correspondent à des bugs identifiés ou des parcours non encore prouvés au début de cette reprise. Les lignes PRÉSENT décrivent une implémentation conservée, puis revalidée ; les anciennes preuves ne remplacent pas les tests du 5 octobre.

## Répartition de l’implémentation

- `PageServlet`, `PortalDAO`, `Page`, JSP sous WEB-INF : consultation des modules et affichage.
- `ActionServlet` et servlets historiques conservés : mutations transactionnelles, contrôle de l’acteur en base.
- `MembershipService`, `AccountDAO` : candidatures, profils, affiliations, mandats, statut et rôle séparés.
- `ContentDAO`, `Module`, `AnnualReport`, `ReportPdf` : science, partenaires, conventions, réunions et rapport annuel serveur.
- `CommunicationService`, `MaterialService`, `FilesService`, `Trace` : échanges, notifications, archives, matériel, versions et audit.
- `Access`, `JwtFilter`, `PageRoutes`, `Passwords`, `JwtUtil` : rôles courants, URLs directes, sessions, CSRF, mots de passe et JWT.
- Les anciens DAO restent conservés ; les servlets actifs utilisent les services transactionnels. Aucune JSP ne contient de SQL/import java.sql.

La présentation publique existante répond au PDF : LIAS, Faculté, équipes, activités et publications consultables sans authentification. Le design est conservé. Les associés peuvent participer à des événements sur invitation et publier selon autorisation, sans accès complet aux modules internes.

## Comparaison initiale et résultat

| # | Fonctionnalité du PDF | État initial | Modifications réalisées | Tests effectués | État final |
|---|---|---|---|---|---|
| 1 | Objectifs du système | PARTIEL : modules présents, validation globale incomplète | Architecture et page publique conservées ; mémoire institutionnelle sécurisée | Matrice HTTP, collaboration, audit, rapport | PASS |
| 2 | Typologie des utilisateurs | PARTIEL : restrictions présentes, ancienne documentation contradictoire | Permissions associé conservées ; règle associé clarifiée dans la documentation | 289 contrôles matrice ; droit publication associé accordé puis révoqué | PASS |
| 3 | Profils membres | PARTIEL : modification existante, consultation croisée à recetter | Fonctionnalités conservées ; tests croisés et photo ajoutés à la recette | crossprofile (18), profile-documents (15), refus édition tiers | PASS |
| 4 | Données temporelles et affiliations | PARTIEL : périodes présentes, parcours multiples à vérifier | Audit détaillé des dates confidentielles ; tests départ/retour | personal-dates (25), lifecycle (20), JUnit affiliations | PASS |
| 5 | Gouvernance et mandats | PARTIEL : succession présente, clôture future incohérente possible | Fin de mandat futur valide ; identifiant mandat initial correctement audité | governance (28), succession (13), JUnit mandat futur et Directeur A/B | PASS |
| 6 | Demandes d’adhésion | PRÉSENT : implémentation et preuves anciennes | Workflow conservé ; acceptation/refus et exclusivité directeur revalidés | adhesion (21), matrice et création compte authentifiable | PASS |
| 7 | Rôles et responsabilités | PARTIEL : mandats conservés, progression complète non prouvée | Test Membre → Chef → Vice → Directeur → ancien directeur | JUnit succession rôles ; history-audit (34) | PASS |
| 8 | Départ et retraite | PRÉSENT : gel, désactivation et réactivation | Conservation de l’historique vérifiée ; clôture future corrigée | lifecycle (20), personal-dates (25), sessions et connexions refusées | PASS |
| 9 | Événements scientifiques | PRÉSENT : éditions, organisateurs, archives | Audit avant/après des changements de responsabilités complété | mutations (36), collaboration (19), extended (13) | PASS |
| 10 | Archivage documentaire | PRÉSENT : upload, téléchargement, recherche, versions | Fonctionnalités conservées ; clés étrangères documentaires renforcées | mutations, catégories, PDF invalide refusé, versions et téléchargement | PASS |
| 11 | Rapport annuel | PRÉSENT : PDF serveur déjà implémenté | Générateur conservé ; tests accents, échappement et pagination ajoutés | pdf (18), pdf-content (6), JUnit PDF, extraction et rendu français | PASS |
| 12 | Publications | PRÉSENT : propriété, consultation et filtres | Fonctionnalités conservées ; associé autorisé puis révoqué testé | publications (9), extended (13), doctorant et auteur imposé | PASS |
| 13 | Communication interne | PRÉSENT : privé, équipe et événement | Fonctionnalités conservées ; références SQL renforcées | communication (27), lecture privée et équipe tierce refusées | PASS |
| 14 | Matériel | PRÉSENT : arrivages, distributions, bénéficiaires et sans dotation | Fonctionnalités conservées ; références SQL renforcées | mutations (36), stock final, double livraison et insuffisance refusées | PASS |
| 15 | Besoins en matériel | PRÉSENT : demande, validation et historique | Fonctionnalités conservées ; alias et décisions revalidés | material-member (11), membre/vice/directeur et refus tiers | PASS |
| 16 | Conventions et collaborations | PRÉSENT : partenaires, conventions, documents, activités | Fonctionnalités conservées ; relations restrictives vérifiées | mutations : partenaire, convention, activité et archivage | PASS |
| 17 | Réunions et PV | PRÉSENT : réunion, ordre du jour, PV et archivage | Fonctionnalités conservées ; références document/réunion renforcées | mutations : réunion, participant, PV lié aux archives | PASS |
| 18 | Recherche globale | PRÉSENT : recherche et filtrage par statut | Fonctionnalités conservées ; visibilité réellement testée | search-profiles (35), recherche données créées, archives internes | PASS |
| 19 | Notifications | PRÉSENT : notifications nominatives et lectures individuelles | Fonctionnalités conservées ; invitations associé et décisions testées | communication (27), adhesion (21), security (27), lecture tierce refusée | PASS |
| 20 | Calendrier | PRÉSENT : données serveur et navigation JavaScript | Interface conservée ; navigation vérifiée dans le navigateur | Mois/semaine/jour, période suivante, Aujourd’hui, détail événement | PASS |
| 21 | Règles générales et mémoire institutionnelle | PARTIEL : contraintes SQL et certains audits incomplets | 41 FK RESTRICT ; audits détaillés ; migration mots de passe sûre et tracée ; comptes techniques affiliés et tracés | history-audit (34), 6 JUnit JDBC, migration répétée, aucune ancienne période supprimée | PASS |

## Bugs et incohérences corrigés

- Un départ pouvait clôturer un mandat futur avant sa date de début.
- L’audit des dates confidentielles ne conservait pas leurs nouvelles valeurs.
- Un changement organisateur/participant écrasait la responsabilité sans audit de ses valeurs avant/après.
- La maintenance pouvait rehasher un digest ancien comme s’il était du texte brut ; les migrations sont maintenant tracées sans secret.
- L’initialisation du directeur utilisait l’identifiant du membre comme identifiant de mandat dans l’audit. La création ADMIN manquait d’affiliation et de trace.
- Les références SQL de plusieurs tables historiques n’avaient pas de clés étrangères dans une installation neuve.
- Maven, Eclipse et les scripts ne produisaient pas le même nom de WAR/contexte ; le build PowerShell pouvait garder des classes obsolètes.
- Le driver MySQL livré était absent du classpath des tests Maven JDBC.
- README : ancien nom, mauvais chemins, documentation inexistante et description obsolète du PDF.

## Fichiers modifiés

- `.gitignore`
- `src/main/java/com/lias/config/DatabaseTool.java`
- `src/main/java/com/lias/controller/ActionServlet.java`
- `src/main/java/com/lias/service/CommunicationService.java`
- `src/main/java/com/lias/service/MembershipService.java`
- `src/main/webapp/WEB-INF/web.xml`
- `tools/build.ps1`
- `tools/CreateTestAccounts.java`
- `tools/final_http.py`
- `tools/recette_support.py`
- `tools/run.ps1`
- `.settings/org.eclipse.wst.common.component`
- `README.md`
- `pom.xml`
- `docs/FINAL_VERIFICATION.md`

## Fichiers créés

- `src/test/java/com/lias/InstitutionalHistoryTest.java`
- `src/test/java/com/lias/ReportPdfTest.java`
- `database/lias_integrity.sql`
- `tools/extended_http.py`
- `tools/SeedIsolated.java`
- `tools/verify-current.ps1`
- `docs/CONFORMITE_2026-10-05.md`

## Portée et limites

PASS signifie que l’implémentation et les scénarios cités ont été validés sur le code et l’environnement de recette décrits. Cela ne promet pas une couverture exhaustive de toutes les saisies ou de tous les navigateurs.

| Vérification supplémentaire | Résultat | Justification |
|---|---|---|
| Publication dans le serveur Eclipse habituel et base historique sur le port 3306 | NON TESTABLE | Aucun service n’écoutait sur les ports 8080/3306 ; cette tâche n’a pas migré ni remplacé cette base. |
| Migration d’intégrité sur les données historiques extérieures à la recette | NON TESTABLE | La migration a réussi deux fois sur la base isolée ; les éventuelles références orphelines d’une autre base restent à examiner sans suppression d’historique. |
| Inspection visuelle exhaustive de chaque formulaire sur tous les appareils | NON TESTABLE | Calendrier réellement manipulé ; routes/JSP et principaux workflows exercés en HTTP. L’inspection de tous les navigateurs/appareils n’a pas été réalisée. |

Aucune fonctionnalité métier manquante n’a été constatée dans les 21 rubriques. Le projet est validé pour la démonstration dans l’environnement isolé. La remise sur le serveur habituel exige encore sauvegarde, contrôle des migrations et publication du WAR final. Aucune conformité universelle à 100 % n’est revendiquée.

Les comptes et données ajoutés sont exclusivement de recette. Les données historiques de la base habituelle restent intactes. Les fixtures peuvent accumuler de nouveaux enregistrements lors d’une réexécution ; elles restent dans la base dédiée, jamais dans le WAR. Les comptes de développement figurent dans `build/test-accounts.properties`, ignoré par Git. Aucun mot de passe réel n’est publié.
