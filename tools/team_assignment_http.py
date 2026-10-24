"""Real Tomcat regression; dedicated fixtures, isolated lias_test* database only."""
from recette_support import *

assert os.environ.get('LIAS_TEST_DB','').startswith('lias_test'), 'Isolated database required'
tag='TEAM-403-'+uuid.uuid4().hex[:10]
director_browser=login_as('director')
admin=login_as('admin')
for name in [tag+' A',tag+' B']:
 director_browser.post('EquipeServlet',dict(nom=name,description='Régression affectation',date_creation='2026-10-05'))
team_a=db('SELECT id FROM equipe WHERE nom='+literal(tag+' A'))
team_b=db('SELECT id FROM equipe WHERE nom='+literal(tag+' B'))
db('INSERT INTO utilisateur(nom,prenom,email,statut_actuel,role_actuel,compte_statut) VALUES ('+literal(tag)+",'Legacy',"+literal(tag+'@example.test')+",NULL,NULL,'ACTIF')")
uid=db('SELECT id FROM utilisateur WHERE email='+literal(tag+'@example.test'))
values=dict(action='affecter',membre_id=uid,equipe_id=team_a)
code,body,_=admin.req('equipes.jsp')
check(code==200 and 'Affecter un membre' in body and tag in body,'formulaire ADMIN propose le membre sans statut')
director_browser.post('EquipeServlet',values)
old=db('SELECT id,date_debut,equipe_id FROM membre_equipe WHERE utilisateur_id='+uid)
admin.post('EquipeServlet',dict(values,equipe_id=team_b))
check(db('SELECT equipe_id FROM utilisateur WHERE id='+uid)==team_b,'équipe actuelle B')
check(db('SELECT id,date_debut,equipe_id FROM membre_equipe WHERE utilisateur_id='+uid+' AND date_fin IS NOT NULL')==old,'ancienne période A conservée et clôturée')
check(db('SELECT equipe_id FROM membre_equipe WHERE utilisateur_id='+uid+' AND date_fin IS NULL')==team_b,'nouvelle période B ouverte')
check(db('SELECT COUNT(*) FROM membre_equipe WHERE utilisateur_id='+uid)=='2','deux périodes conservées')
check(db('SELECT statut_actuel IS NULL AND role_actuel IS NULL FROM utilisateur WHERE id='+uid)=='1','statut et rôle vides inchangés')
check(db("SELECT COUNT(*) FROM audit_log WHERE action='AFFECTATION_EQUIPE' AND entite_id="+uid)=='2','deux affectations auditées')
for path in ['equipes.jsp','profil.jsp?id='+uid]:
 code,body,_=director_browser.req(path)
 check(code==200 and tag+' B' in body and tag in body,'composition/profil cohérents : '+path)
for role in ['member','chef','associe','doctorant']:
 b=login_as(role);code,_,_=b.post('EquipeServlet',values,expect=False)
 check(code==403,role+' POST direct refusé')
visitor=Browser();code,_,_=visitor.req('EquipeServlet',dict(values,csrf=visitor.token('login.jsp')))
check(code==403,'visiteur POST direct refusé')
for role in ['retraite','ancien']:
 b=Browser();a=accounts();code,body,url=b.login(a[role],a[role+'.password'])
 check('compte restreint' in body and not any(c.name=='jwt' for c in b.jar),role+' connexion refusée')
 code,_,_=b.req('EquipeServlet',dict(values,csrf=b.token('login.jsp')))
 check(code==403,role+' POST direct refusé')
for action in ['save','archive']:
 code,_,_=admin.post('EquipeServlet',dict(action=action,id=team_a),expect=False)
 check(code==403,'ADMIN ne gagne pas le droit '+action)
code,_,_=admin.req('EquipeServlet',values)
check(code==403,'CSRF obligatoire pour ADMIN')
director_browser.post('EquipeServlet',dict(id=team_b,nom=tag+' B',description='Modification vérifiée',date_creation='2026-10-05'))
code,_,_=director_browser.post('EquipeServlet',dict(action='archive',id=team_a),expect=False)
# Equipe has no archive/state field; this unsupported action was already forbidden.
check(code==403,'archivage non proposé des équipes reste refusé')
check(db('SELECT COUNT(*) FROM membre_equipe WHERE utilisateur_id='+uid)=='2','tentative archivage ne supprime aucune période')
save_results('team-assignment')
