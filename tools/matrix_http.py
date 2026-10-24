from recette_support import *
a=accounts()
public={'','login','candidature','membres','equipes','evenements','publications'}
common={'profil','dashboard','notifications','recherche'}
internal={'documents','materiel','conventions','reunions','messagerie'}
pages=sorted(public|common|internal|{'calendrier','demandes','gouvernance','historique','rapport'})
endpoints={'publications':'AjouterPublicationServlet','documents':'DocumentServlet','materiel':'AjouterMaterielServlet','messagerie':'MessageServlet','gouvernance':'MandatServlet','demandes':'DemandeServlet','equipes':'EquipeServlet','evenements':'AjouterEvenementServlet','conventions':'ConventionServlet','reunions':'ReunionServlet'}
for role in ['visitor','director','admin','vice','chef','member','associe','doctorant','retraite','ancien']:
 b=Browser();active=role not in ['visitor','retraite','ancien']
 if role!='visitor':
  code,body,url=b.login(a[role],a[role+'.password'])
  check(code==200 and (url.endswith('/dashboard.jsp') if active else ('Email ou mot de passe incorrect, ou compte restreint.' in body and not any(c.name=='jwt' for c in b.jar))),role+' login '+('autorisé' if active else 'refusé'))
 allowed=set(public)
 writes=set()
 if active:
  allowed|=common
  if role!='doctorant':allowed.add('calendrier')
  if role not in ['associe','doctorant']:allowed|=internal;writes|={'publications','documents','materiel','messagerie'}
  if role=='doctorant':writes.add('publications')
  if role in ['director','admin']:allowed|={'gouvernance','historique'};writes.add('gouvernance')
  if role=='director':allowed.add('demandes');writes.add('demandes')
  if role in ['director','vice']:allowed.add('rapport');writes|={'equipes','evenements','conventions','reunions'}
 for page in pages:
  code,body,url=b.req(page)
  ok=(code==200 and '/login.jsp' not in url) if page in allowed and page!='login' else ((code==200) if page in allowed else (code==403 if active else (code==200 and '/login.jsp' in url)))
  check(ok,role+' GET /'+page+' => '+str(code)+(' login' if '/login.jsp' in url else ''))
 if active:
  for module,path in endpoints.items():
   code,body,url=b.req(path,{'csrf':b.token(),'action':'demander' if module=='materiel' else 'recette_invalid_action'})
   check(code!=403 and code<500 if module in writes else code==403,role+' POST '+path+' contrôle accès '+str(code))
 save_results('matrix')
