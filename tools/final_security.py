from final_http import *
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,*args,**kwargs): return None
b=Browser();csrf=b.token('login');before={c.name:c.value for c in b.jar}
b.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(b.jar),NoRedirect())
code,body,url=b.req('LoginServlet',dict(email='directeur.test@lias.example.test',password=required_password("LIAS_TEST_DIRECTOR_PASSWORD"),csrf=csrf),{'Cookie':'JSESSIONID='+before['JSESSIONID']+'; jwt=invalid.expired.cookie'})
check(code==302,'POST /LoginServlet réel avec ancien JWT invalide = 302')
after={c.name:c.value for c in b.jar}
check('jwt' in after and 'JSESSIONID' in after,'cookies JWT et session créés')
check(before['JSESSIONID']!=after['JSESSIONID'],'identifiant session renouvelé à la connexion')
check(any(c.name=='jwt' and c.has_nonstandard_attr('HttpOnly') and c.get_nonstandard_attr('SameSite')=='Lax' for c in b.jar),'JWT HttpOnly et SameSite=Lax')
b.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(b.jar))
check(b.req('dashboard')[0]==200,'GET /dashboard authentifié = 200 après redéploiement')
wrong=Browser();code,body,url=wrong.login('directeur.test@lias.example.test','incorrect')
check(code==200 and 'Email ou mot de passe incorrect, ou compte restreint.' in body and not any(c.name=='jwt' for c in wrong.jar),'mauvais mot de passe : message sans HTTP 500 ni JWT')
v=Browser()
for path in ['dashboard','documents','materiel','gouvernance','demandes','reunions','messagerie','rapport','profil','DownloadServlet?id=1']:
 code,body,url=v.req(path);check(code==200 and '/login.jsp' in url,'visiteur redirigé serveur : '+path)
for path in ['WEB-INF/views/portal.jsp','../Lias-Jee/WEB-INF/web.xml']:
 check(v.req(path)[0] in (400,403,404),'ressource privée non exposée : '+path)
check(b.req('rapport?annee=bad')[0]==400,'année rapport invalide = 400')
check(b.req('DownloadServlet?id=99999999')[0] in (400,403,404),'document absent sans 500')
check(b.req('LogoutServlet')[0]==405,'déconnexion GET refusée')
tag='RECETTE-DATE-'+str(int(time.time()))
code,body,url=b.upload('DocumentServlet',{'titre':tag,'type_doc':'RAPPORT'})
check(code==200 and 'Opération enregistrée avec succès.' in body,'nouvel upload après correction')
check(db('SELECT date_depot IS NOT NULL FROM document WHERE titre='+literal(tag))=='1','date de dépôt réellement persistée')
notification=db('SELECT id_notif FROM notification WHERE id_membre=4 ORDER BY id_notif DESC LIMIT 1')
b.post('NotificationServlet',{'action':'lire','id':notification})
check(db('SELECT COUNT(*) FROM notification_lecture WHERE membre_id=4 AND notification_id='+notification)=='1','notification marquée lue en base')
csrf=b.token();code,body,url=b.req('LogoutServlet',{'csrf':csrf})
check(code==200 and '/login.jsp' in url and not any(c.name=='jwt' for c in b.jar),'déconnexion POST supprime JWT')
check('/login.jsp' in b.req('dashboard')[2],'session déconnectée ne peut plus lire dashboard')
(OUT/'security-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')

