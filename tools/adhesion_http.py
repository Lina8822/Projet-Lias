from recette_support import *
d=login_as('director');tag=str(int(time.time()));before=audit_ids()
for decision in ['ACCEPTEE','REFUSEE']:
 email='recette.candidat.'+decision.lower()+'.'+tag+'@lias.example.test'
 b=Browser();code,body,url=b.upload('CandidatureServlet',dict(nom='RECETTE',prenom=decision,email=email,password=required_password(),motivation='Recette de la procédure publique',etablissement='LIAS développement',laboratoire='LIAS',statut_demande='DOCTORANT'))
 check(code==200 and 'Candidature enregistrée.' in body,'candidature publique CV '+decision)
 ident=db('SELECT id_demande FROM demandes_adhesion WHERE email='+literal(email))
 check(bool(ident),'dossier persisté '+decision)
 vice=login_as('vice');check(vice.post('DemandeServlet',dict(id=ident,decision=decision),expect=False)[0]==403,'vice ne décide pas une adhésion')
 d.post('DemandeServlet',dict(id=ident,decision=decision))
 check(db('SELECT etat_demande FROM demandes_adhesion WHERE id_demande='+ident)==decision,'décision persistée '+decision)
 check(db('SELECT COUNT(*) FROM notification_candidat WHERE demande_id='+ident)=='1','notification candidat '+decision)
 uid=db('SELECT id FROM utilisateur WHERE email='+literal(email))
 if decision=='ACCEPTEE':
  check(bool(uid),'compte créé après acceptation')
  check(db('SELECT COUNT(*) FROM historique_affiliation WHERE id_membre='+uid+' AND date_fin IS NULL')=='1','affiliation créée après acceptation')
  check(db('SELECT mot_de_passe LIKE '+literal('pbkdf2$%')+' FROM utilisateur WHERE id='+uid)=='1','mot de passe candidat haché')
  code,body,url=b.login(email,required_password());check(code==200 and url.endswith('dashboard.jsp'),'candidat accepté se connecte réellement')
 else:check(not uid,'aucun compte créé après refus')
check(before<=audit_ids(),'audit adhésion conservé')
save_results('adhesion')
