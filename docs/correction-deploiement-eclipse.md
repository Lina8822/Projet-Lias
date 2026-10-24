# Correction effective du projet Eclipse et du déploiement — 10/09/2026

## Cause confirmée

Le projet Eclipse enregistré sous le nom Lias-Jee pointe vers `C:/Users/ELECTRO-RIMO/Downloads/Lias-Jee`, d'après le fichier `.metadata/.plugins/org.eclipse.core.resources/.projects/Lias-Jee/.location`. La première correction avait été effectuée dans le dossier fourni à la tâche : `C:/Users/ELECTRO-RIMO/eclipse-workspace/Lias-Jee`. Eclipse continuait donc à compiler et publier une autre copie, encore incorrecte.

Dans la copie Eclipse, `PageServlet.render` appelait `JwtFilter.module(r.getServletPath())` en ligne 10. Le filtre ne déclarait pas cette méthode. L'intention était d'identifier la page courante afin d'appliquer Access.read et de sélectionner le rendu. Elle est désormais traitée par `PageRoutes.key`, avec une liste fermée de routes. Aucun faux helper module n'a été ajouté au filtre. Les autres appels module dans PageServlet sont sa propre méthode de rendu de contenu ; ceux d'ActionServlet sont une méthode abstraite sans paramètre, implémentée par ses deux sous-classes.

Les lignes 88 et 134 de l'ancien JwtFilter correspondent au passage dans la chaîne de filtres. Elles ne démontrent pas que l'appel invalide soit dans le filtre. Le bytecode publié de PageServlet contenait littéralement le message `Unresolved compilation problem: The method module(String) is undefined for the type JwtFilter`. Trois autres classes publiées contenaient également des erreurs différées : AccountDAO, Access et FilesService.

## Actions réellement effectuées

- Corrections synchronisées dans les deux copies, après sauvegarde des fichiers originaux de la copie Eclipse.
- JwtFilter : les pages d'entrée index/login/candidature sont accessibles sans valider de JWT, même si un cookie expiré ou invalide est envoyé. Les règles existantes d'Access restent appliquées aux autres pages et aux actions.
- PageServlet : résolution par PageRoutes, mappings des URL sans extension, refus 403 contrôlé, utilisateur traité comme anonyme pour les pages d'entrée afin de ne pas charger ses notifications en base sur le login.
- PageRoutes, Access, Membre, DBConnection, MembreDAO, LoginServlet et JwtUtil : corrections de compilation et d'authentification de la première passe copiées dans le projet réellement utilisé par Eclipse.
- `.project` : nom Lias-Jee ; `.settings/org.eclipse.wst.common.component` : sortie `/Lias-Jee/target/classes`, sources de test exclues de la publication.
- pom.xml et LaunchRegressionTest : six tests JUnit exécutables, dont un nouveau test des pages publiques avec cookie invalide et ancienne session.
- Migration SQL corrigée copiée dans le vrai projet ; aucune modification de la base réelle effectuée.
- `mvn clean`, `mvn test`, `mvn package` exécutés avec succès dans chaque copie. Le clean a supprimé target avant recompilation ; les WAR ont été régénérés.
- Anciennes classes supprimées dans le seul déploiement Eclipse `tmp0/wtpwebapps/Lias-Jee/WEB-INF/classes`, après sauvegarde. Contenu du WAR neuf publié, puis contexte rechargé par Tomcat. Les autres applications Tomcat n'ont pas été touchées.
- Empreintes SHA-256 comparées : PageServlet, JwtFilter et PageRoutes publiés identiques aux classes reconstruites. Les classes du projet Downloads sont également identiques à celles du workspace corrigé.

## Vérifications sur le serveur réel localhost:8080

Avant intervention : `/login` et `/dashboard` répondaient 500 et contenaient le message module(String).

Après publication, requêtes sans suivre automatiquement les redirections :

| Route | Sans cookie | Cookie JWT invalide |
|---|---|---|
| / | 200 | 200 |
| /login | 200 | 200 |
| /candidature | 200 | 200 |
| /dashboard | 302 vers login | 302 vers login (session expirée) |
| /gouvernance | 302 vers login | 302 vers login (session expirée) |
| /demandes | 302 vers login | 302 vers login (session expirée) |
| /membres | 503 (schéma SQL ancien) | 302 vers login |
| /publications | 503 (schéma SQL ancien) | 302 vers login |
| /evenements | 503 (schéma SQL ancien) | 302 vers login |

Aucune de ces réponses ne contient une erreur de compilation. Le dashboard authentifié n'a pas été validé sur la base Eclipse non migrée. Le code actuel considère membres/publications/evenements comme des consultations publiques conformément à Access.read ; cette règle n'a pas été remplacée arbitrairement.

## Compilation

- Six tests JUnit : zéro échec, zéro erreur, zéro ignoré.
- Compilateur Eclipse ECJ : zéro erreur, dix avertissements non bloquants.
- Jasper de Tomcat 11.0.22 : zéro erreur de compilation des JSP.
- Runtime réellement interrogé : Tomcat Eclipse 11.0.22, Java 26.0.1, contexte /Lias-Jee, port 8080.
- Recherche dans les sources des deux copies : aucun `JwtFilter.module` restant.
- Recherche dans les classes neuves et publiées : aucune chaîne `Unresolved compilation problem` restante. La sauvegarde des anciennes classes conserve volontairement les preuves du problème.

Preuves : `build/verification/redeploy/http-8080.json`, `published-hashes.json`, `mvn-*.log`, `eclipse-mvn-*.log`, `jsp.log`, `ecj.log` et `global-search.txt`.

## À faire dans Eclipse

F5 sur le projet pour rafraîchir les fichiers modifiés extérieurement, puis Maven > Update Project et Project > Clean si les marqueurs restent affichés. Vérifier Properties > Resource > Location : le projet actuel utilise Downloads/Lias-Jee, désormais corrigé lui aussi. Le serveur en cours a déjà rechargé les bonnes classes ; les tests ci-dessus ne reposent pas sur une instance isolée.

Le schéma de `lias_data_base` reste à migrer pour les pages de données. Cela est distinct du bug de compilation maintenant éliminé. L'accord demandé précédemment pour modifier cette base n'a pas été reçu.
