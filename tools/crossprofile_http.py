from recette_support import *
a=accounts();member=login_as('member');associe=login_as('associe');director=login_as('director');admin=login_as('admin')
target=a['associe.id'];birth='1990-04-12';hired='2018-09-01'
director.post('CompteServlet',dict(action='permission',id=target,publication_autorisee='0',date_naissance=birth,date_embauche=hired))
for actor,other in [(member,'associe'),(associe,'member')]:
 code,body,url=actor.req('membres');check(code==200 and a[other] in body,'annuaire public entre comptes '+other)
 code,body,url=actor.req('profil?id='+a[other+'.id']);check(code==200 and a[other] in body,'profil tiers consultable '+other)
 check('Modifier mon profil' not in body and 'Changer mon mot de passe' not in body,'aucun formulaire édition tiers '+other)
before=db('SELECT nom,prenom,email,biographie,centre_interet FROM utilisateur WHERE id IN ('+a['member.id']+','+target+') ORDER BY id')
code,body,url=member.post('ModifierProfilServlet',dict(id=target,nom='RECETTE',prenom='member',email=a['member'],etablissement='LIAS recette',laboratoire='LIAS',biographie='',centre_interet='',profil_public='1'),expect=False)
check(code==403,'POST profil tiers refusé explicitement par serveur')
check(db('SELECT nom,prenom,email,biographie,centre_interet FROM utilisateur WHERE id IN ('+a['member.id']+','+target+') ORDER BY id')==before,'profil tiers et profil acteur inchangés après refus')
for page in ['membres','profil?id='+target]:
 code,body,url=member.req(page);check(code==200 and birth not in body and hired not in body,'dates confidentielles absentes pour membre tiers '+page)
code,body,url=associe.req('profil');check(code==200 and birth in body and hired in body,'dates visibles par membre concerné')
for role,b in [('director',director),('admin',admin)]:
 code,body,url=b.req('membres');check(code==200 and birth in body and hired in body,'dates visibles pour '+role)
save_results('crossprofile')
