from recette_support import *
a=accounts();b=login_as('member');d=login_as('vice');tag='RECETTE-BESOIN-'+str(int(time.time()))
b.post('DemandeServlet',dict(action='demander',nom_materiel=tag,quantite='1',description='Besoin de recette soumis par membre'))
ident=db('SELECT id_demande_mat FROM demande_materiel WHERE nom_materiel='+literal(tag))
check(db('SELECT id_membre FROM demande_materiel WHERE id_demande_mat='+ident)==a['member.id'],'demande rattachée au membre connecté')
for endpoint in ['DemandeServlet','AjouterMaterielServlet']:
 check(b.post(endpoint,dict(action='traiter',id=ident,decision='ACCEPTEE'),expect=False)[0]==403,'membre ne décide pas via '+endpoint)
check(db('SELECT etat_demande FROM demande_materiel WHERE id_demande_mat='+ident)=='EN_ATTENTE','décision interdite ne change pas état')
d.post('DemandeServlet',dict(action='traiter',id=ident,decision='REFUSEE'))
check(db('SELECT etat_demande FROM demande_materiel WHERE id_demande_mat='+ident)=='REFUSEE','direction traite la demande matériel')
check(b.post('DemandeServlet',dict(id='1',decision='ACCEPTEE'),expect=False)[0]==403,'alias matériel ne donne aucun droit adhésion')
check(db("SELECT COUNT(*) FROM audit_log WHERE action='DECISION_MATERIEL' AND entite_id="+ident)=='1','décision matériel auditée')
save_results('material-member')
