# Audit initial LaboJEE
Cahier des charges : Jee-Project.pdf, 8 pages, 21 sections, lu intégralement.
Architecture conservée : Eclipse Dynamic Web Project, Jakarta, JSP, JDBC, MySQL, Tomcat.
Aucun schéma SQL fourni. Le schéma de migration provient des requêtes existantes.

## Existant conservé et étendu
Modèles Java, DAO membres/profil/matériel, routes Servlet et noms des JSP, connecteur MySQL 9.7.0.
Listes et insertions simples : équipes, publications, événements, documents, demandes, partenaires, messages.

## Défauts constatés
- ReunionServlet appelle trois méthodes absentes de MembreDAO ; reunions.jsp manque.
- Paramétrage Eclipse Java 25/26 incohérent ; aucune construction WAR reproductible.
- Identifiant matériel forcé à 1 ; profil avec repli à 1.
- Mots de passe stockés en clair et comptes créés avec nom123 affiché en console.
- JWT signé avec secret public en code ; JWT complet journalisé ; session reconstruite avec droits périmés.
- Absence de CSRF ; décision d'adhésion déclenchée par GET ; décision et création non transactionnelles.
- Mandats non transactionnels ; rôle actuel non recalculé ; chefs de toutes les équipes clôturés ensemble.
- Upload public à nom fourni par le client ; traversal et écrasement possibles.
- XSS via expressions JSP non échappées ; logique JDBC dans plusieurs pages ; erreurs SQL masquées.
- Statuts/rôles de l'annuaire fictifs, confidentialité et restrictions doctorants insuffisantes.
- Navigation et déconnexion incohérentes ; styles répétés ; rapport non filtré par année.

## Manques
Cycle de vie et affiliations, stocks/distributions, conventions distinctes des partenaires,
PV, ciblage et lecture messagerie, notifications lisibles individuellement, recherche globale,
calendrier navigable, portail public, audit et majorité des modifications/archivages.

## Environnement
JDK 26 et Tomcat 11 disponibles. Aucun MySQL à l'écoute sur localhost:3306 lors de l'audit.
Migration non exécutée sur une base utilisateur. Aucune donnée existante supprimée.
