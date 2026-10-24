from recette_support import *
a=accounts();d=login_as('director');uid=a['member.id'];birth='1992-05-14';hired='2019-10-07';today=db('SELECT CURRENT_DATE')
d.post('CompteServlet',dict(action='permission',id=uid,publication_autorisee='0',date_naissance=birth,date_embauche=hired))
check(db('SELECT date_naissance,date_embauche FROM utilisateur WHERE id='+uid)==birth+'\t'+hired,'dates personnelles persistées en base réelle')
for role in ['member','director','admin','associe','doctorant','chef']:
 b=login_as(role);page='profil' if role=='member' else ('membres' if role in ['director','admin'] else 'profil?id='+uid)
 code,body,url=b.req(page);allowed=role in ['member','director','admin']
 check(code==200 and ((birth in body and hired in body) if allowed else (birth not in body and hired not in body)),role+' confidentialité dates personnelles')
old={line.split('\t')[0]:line for line in db('SELECT * FROM historique_affiliation WHERE id_membre='+uid+' AND date_fin IS NOT NULL ORDER BY id_historique').splitlines()}
count=int(db('SELECT COUNT(*) FROM historique_affiliation WHERE id_membre='+uid))
for departed in ['RETRAITE','ANCIEN_MEMBRE']:
 d.post('CompteServlet',dict(action='statut',id=uid,statut_actuel=departed))
 d.post('CompteServlet',dict(action='statut',id=uid,statut_actuel='PERMANENT'))
check(int(db('SELECT COUNT(*) FROM historique_affiliation WHERE id_membre='+uid))==count+2,'deux retours créent deux périodes supplémentaires')
now={line.split('\t')[0]:line for line in db('SELECT * FROM historique_affiliation WHERE id_membre='+uid+' ORDER BY id_historique').splitlines()}
check(all(now.get(k)==v for k,v in old.items()),'anciennes périodes clôturées inchangées')
check(db('SELECT COUNT(*) FROM historique_affiliation WHERE id_membre='+uid+' AND (date_debut IS NULL OR date_fin<date_debut OR date_debut>CURRENT_DATE)')=='0','dates périodes valides après allers-retours')
check(db('SELECT COUNT(*) FROM historique_affiliation WHERE id_membre='+uid+' AND date_fin IS NULL AND date_debut=CURRENT_DATE')=='1','une seule affiliation actuelle datée du retour')
check(db('SELECT date_naissance,date_embauche FROM utilisateur WHERE id='+uid)==birth+'\t'+hired,'dates personnelles conservées après allers-retours')
d.post('EquipeServlet',dict(action='affecter',membre_id=uid,equipe_id=a['team.id']))
save_results('personal-dates')
