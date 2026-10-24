from pathlib import Path
import json,hashlib,datetime
root=Path('build/final-verification')
names=['matrix','lifecycle','adhesion','pdf','pdf-content','communication','publications','material-member','search-profiles']
manifest={'environment':'Eclipse Tomcat 11.0.22 localhost:8080/Lias-Jee / lias_data_base','verified_at':datetime.datetime.now().isoformat(),'proofs':[]}
for name in names:
 p=root/(name+'-results.json');data=json.loads(p.read_text(encoding='utf-8'));assert data and all(x['ok'] for x in data),name
 manifest['proofs'].append(dict(file=str(p),checks=len(data),sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
(root/'proof-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
p=Path('docs/FINAL_VERIFICATION.md');s=p.read_text(encoding='utf-8')
updates={
2:'| 2 | Typologie des utilisateurs | PARTIEL | `matrix-results.json` : 289 contrôles réels, neuf profils + visiteur, GET et contrôles POST ; comptes GELE/DESACTIVE refusés au login. | Conforme à Access actuel. Divergence à trancher : la nouvelle demande autorise la lecture interne ASSOCIE, le code actuel la refuse. Droits non élargis sans clarification. |',
6:'| 6 | Demandes d’adhésion | VALIDÉ | `adhesion-results.json` : 21 contrôles ; candidature publique avec CV, acceptation et connexion du nouveau compte, refus sans compte ; vice-directeur refusé (403). | Compte, affiliation, notification candidat et audit vérifiés en base réelle. |',
8:'| 8 | Départ et retraite | VALIDÉ | `lifecycle-results.json` : « retraite gèle le compte », « retraité ne peut plus se connecter », « départ désactive le compte », retour ACTIF et nouvelle affiliation ; `matrix-results.json` : login ancien/retraité refusé. | Mandats et affectations clôturés ; anciennes périodes et audit préservés. |',
11:'| 11 | Rapport annuel | VALIDÉ | `pdf-results.json` (18 contrôles) et `pdf-content-results.json` (6) : PDF HTTP réel, direction autorisée, autres profils 403, année invalide 400, données annuelles comparées à la base. | Export serveur paginé, mêmes requêtes que HTML, audit export ; PDF analysé par pypdf et rendu inspecté. |',
12:'| 12 | Publications | VALIDÉ | `publications-results.json` : 9 contrôles ; doctorant ajoute/modifie sa publication, propriétaire imposé malgré id_auteur falsifié, modification/archivage par tiers refusés. Filtres UI testés dans la recette précédente. | Propriété serveur vérifiée ; le champ bibliographique auteurs reste libre pour conserver les coauteurs. |',
13:'| 13 | Communication interne | VALIDÉ | `communication-results.json` : privé entre deux comptes, visibilité destinataire, lecture privée par tiers 403 ; équipe visible aux seuls membres et écriture tierce 403 ; discussion événement. | Lectures persistées et historique conservé sur la base réelle. |',
15:'| 15 | Besoins matériel | VALIDÉ | `material-member-results.json` : 11 contrôles ; membre soumet via DemandeServlet, décision non-direction 403 sur les deux routes, vice-directeur refuse réellement, état et audit SQL vérifiés. | Alias sécurisé ajouté ; droits d’adhésion non accordés au membre. |',
18:'| 18 | Recherche globale | VALIDÉ | `search-profiles-results.json` : 35 contrôles sur sept comptes actifs ; doctorant sans blocs Membres/Événements, archives uniquement PERMANENT ; recherche UI précédemment testée. | Filtrage réel du contenu HTML selon le statut, pas uniquement du menu. |',
19:'| 19 | Notifications | VALIDÉ | `communication-results.json` : notification document pour deux comptes, lecture individuelle, notification de l’autre compte inchangée, tentative tierce 403 ; candidature lue nominativement par directeur. | Non-lus isolés par membre ; messages et documents produisent les notifications attendues. |'
}
lines=s.splitlines()
for i,line in enumerate(lines):
 for number,new in updates.items():
  if line.startswith('| '+str(number)+' |'):lines[i]=new
s='\n'.join(lines)+'\n'
start=s.index('## Limites et prochaine étape nécessaire');end=s.index('## Complément de recette',start)
s=s[:start]+'''## Limites restantes après la recette ciblée

L’accord explicite de la nouvelle demande a permis de créer les comptes de développement. Aucun compte de production n’a été créé et aucun accord de création n’est encore bloquant.

La règle ASSOCIE reste à clarifier : la tâche 2 demande une lecture interne, tandis que Access.java et le filtrage demandé à la tâche 9 la limitent aux modules non internes. Les restrictions existantes sont conservées ; la matrice les prouve mais ne valide pas la règle contradictoire.

Les points 1, 3, 4, 5, 7 et 21 restent PARTIEL pour leurs contrôles plus larges non couverts par les neuf tâches ciblées (notamment paramétrage ADMIN, données personnelles temporelles, consultation croisée des profils et succession complète des responsabilités). Les cycles de statut et leur audit sont désormais prouvés. La recette frontend exhaustive demeure hors de cette reprise ciblée.

L’export PDF simple utilise les caractères Windows-1252 (français/latin occidental) et des polices PDF standard ; les écritures hors de cet alphabet nécessiteraient une police Unicode embarquée. Les données testées sont correctement rendues.

'''+s[end:]
s=s.replace("- L'accord spécifique pour les sept comptes de rôles reste nécessaire ; aucune création supplémentaire de compte effectuée.","- État historique à 04 h : création en attente. Cet accord a depuis été donné et les comptes ont été créés lors de la recette ciblée ci-dessous.")
s+='''
## Recette ciblée des neuf tâches — 11 septembre 2026, soirée

Toutes les preuves citées ci-dessus sont dans `build/final-verification/`. `proof-manifest.json` enregistre leur nombre de contrôles et leur empreinte SHA-256 : **436 contrôles réussis** au total (ils incluent des connexions et contrôles communs répétés).

### Comptes réellement provisionnés

`tools/CreateTestAccounts.java --development-lias_data_base` vérifie la base et son URL locale, refuse de réinitialiser un compte existant et conserve id=4. Les créations et mandats sont tracés par Trace.audit. Aucun compte historique n’est modifié.

| Profil | Email | ID | Statut / compte |
|---|---|---|---|
| DIRECTEUR | directeur.test@lias.example.test | 4 | PERMANENT / ACTIF |
| ADMIN | recette.admin@lias.example.test | 6 | PERMANENT / ACTIF |
| VICE_DIRECTEUR | recette.vice@lias.example.test | 7 | PERMANENT / ACTIF, mandat actif |
| CHEF_EQUIPE | recette.chef@lias.example.test | 8 | PERMANENT / ACTIF, mandat actif |
| MEMBRE_EQUIPE | recette.member@lias.example.test | 9 | PERMANENT / ACTIF après retour |
| ASSOCIE | recette.associe@lias.example.test | 10 | ASSOCIE / ACTIF |
| DOCTORANT | recette.doctorant@lias.example.test | 11 | DOCTORANT / ACTIF |
| RETRAITE | recette.retraite@lias.example.test | 12 | RETRAITE / GELE |
| ANCIEN_MEMBRE | recette.ancien@lias.example.test | 13 | ANCIEN_MEMBRE / DESACTIVE |

Mot de passe des comptes recette : `LIAS_TEST_PASSWORD`. Directeur : `LIAS_TEST_DIRECTOR_PASSWORD`. Identifiants reproductibles dans `build/test-accounts.properties` (format Java Properties ; fichier privé ignoré par Git). Les tests d’adhésion créent aussi un compte candidat DOCTORANT avec un email horodaté `recette.candidat.acceptee.*@lias.example.test` ; son mot de passe de développement est `LIAS_TEST_PASSWORD`. Le candidat refusé n’a aucun compte.

### Corrections réalisées

- Nouvelle migration `database/lias_membership_periods.sql` : l’ancienne clé primaire (utilisateur_id,equipe_id) empêchait le retour dans la même équipe. Ajout d’un identifiant de période, index membre et changement de clé primaire sans suppression de ligne ni de clé étrangère. Sauvegarde `build/final-verification/before-membership-periods.sql`. Les deux migrations déjà appliquées n’ont pas été modifiées pendant cette reprise.
- `AnnualReport.java` extrait les requêtes existantes de PageServlet.report ; HTML et PDF partagent cette source.
- `ReportPdf.java`, `RapportPdfServlet.java` et `WEB-INF/views/portal.jsp` : PDF serveur paginé, bouton direction uniquement, contrôle serveur, validation de l’année, audit et cache désactivé. Aucune dépendance PDF ajoutée.
- `ActionServlet.java`, `DemandeServlet.java`, `PageRoutes.java`, `JwtFilter.java` : seuls les actes explicites `demander`/`traiter` sur DemandeServlet sont orientés vers MaterialService. Les décisions d’adhésion restent réservées au directeur ; la décision matériel reste contrôlée par Access.direction dans le service.

### Scripts et exécution

Scripts sous `tools/` : `CreateTestAccounts.java`, `recette_support.py`, `matrix_http.py`, `lifecycle_http.py`, `adhesion_http.py`, `report_pdf_http.py`, `verify_report_pdf.py`, `communication_http.py`, `publications_http.py`, `material_member_http.py`, `search_profiles_http.py`, `recette_build.ps1`, `publish_development.ps1`, `ReloadDevelopment.java`, `update_verification_report.py`.

Maven `clean test package` après chaque étape : logs `maven-1.log` à `maven-9.log`, puis `maven-final.log` : BUILD SUCCESS, 9 tests JUnit, 0 échec, 0 erreur. Jasper final : `jsp-target.log`, 0 erreur. Le nouveau WAR est `target/Lias-Jee.war` ; sources et déploiement ont été synchronisés dans le projet réellement lié à Eclipse et le contexte Tomcat a été rechargé.

Les scripts de scénario écrivent uniquement des données de recette et les conservent ; leur réexécution ajoute des dossiers/messages/publications de test. Le script de comptes est réexécutable sans réinitialisation. La migration de périodes est une migration à exécuter une seule fois.
'''
p.write_text(s,encoding='utf-8')
print('Rapport mis à jour depuis les preuves réussies.')
