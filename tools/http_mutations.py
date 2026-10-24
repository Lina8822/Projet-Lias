from test_environment import required_password
import runpy,subprocess,time,uuid,re,urllib.request,urllib.error
from pathlib import Path
x=runpy.run_path('tools/http_test.py');globals().update({k:x[k] for k in ('BASE','accounts','check','session','req','token','login')})
tag='http'+str(int(time.time()))
def db(query):
 return subprocess.check_output(['C:/xampp/mysql/bin/mysql.exe','--host=127.0.0.1','--port=33307','--user=root','--default-character-set=utf8mb4','-N','-B','lias_test','-e',query],encoding='utf-8').strip()
def post(s,path,values):
 _,body,_=req(s,'profil.jsp');values=dict(values,csrf=token(body));code,body,url=req(s,path,values);check(code==200 and 'Opération enregistrée avec succès.' in body,path+' '+values.get('action','save'));return body

def upload(s,path,values,filename='cv.pdf',contents=b'%PDF-1.4\n1 0 obj<<>>endobj\n%%EOF'):
 _,body,_=req(s,'candidature.jsp' if path=='CandidatureServlet' else 'documents.jsp');values=dict(values,csrf=token(body));boundary=uuid.uuid4().hex;parts=[]
 for k,v in values.items():parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="'+k+'"\r\n\r\n'+str(v)+'\r\n').encode())
 parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="fichier"; filename="'+filename+'"\r\nContent-Type: application/pdf\r\n\r\n').encode()+contents+b'\r\n');parts.append(('--'+boundary+'--\r\n').encode())
 request=urllib.request.Request(BASE+path,b''.join(parts),{'Content-Type':'multipart/form-data; boundary='+boundary})
 try:
  response=s.open(request,timeout=25);return response.status,response.read().decode('utf-8')
 except urllib.error.HTTPError as e:return e.code,e.read().decode('utf-8')
director,_=login('director');member,_=login('member');doc,_=login('doctorant');admin,_=login('admin')
candidate=session();email=tag+'@example.test'
code,body=upload(candidate,'CandidatureServlet',{'nom':'Candidat','prenom':'HTTP','email':email,'motivation':'Recherche','etablissement':'Faculté','laboratoire':'Externe','statut_demande':'DOCTORANT','password':required_password()},'../../evil.jsp')
check(code==200 and 'Candidature enregistrée' in body,'candidature multipart CV nom hostile')
app=db("SELECT id_demande FROM demandes_adhesion WHERE email='"+email+"'")
check(db('SELECT cv FROM demandes_adhesion WHERE id_demande='+app).endswith('.pdf'),'stockage UUID extension PDF')
post(director,'DemandeServlet',{'id':app,'decision':'ACCEPTEE'})
_,body,_=req(candidate,'candidature.jsp');code,body,_=req(candidate,'SuiviCandidatureServlet',{'csrf':token(body),'email':email,'password':required_password()})
check('ACCEPTEE' in body,'suivi candidat authentifié')
email2=tag+'-refus@example.test'
code,body=upload(candidate,'CandidatureServlet',{'nom':'Refus','prenom':'HTTP','email':email2,'motivation':'Recherche','etablissement':'Faculté','laboratoire':'Externe','statut_demande':'ASSOCIE','password':required_password()})
app2=db("SELECT id_demande FROM demandes_adhesion WHERE email='"+email2+"'");post(director,'DemandeServlet',{'id':app2,'decision':'REFUSEE'});check(db('SELECT COUNT(*) FROM notification_candidat WHERE demande_id='+app2)=='1','refus notifié et historisé')
event={'titre':tag,'type_event':'SEMINAIRE','date_event':'2026-09-15T10:00','date_fin':'2026-09-15T12:00','lieu':'Amphi','edition':'2026','description':'Séminaire test'}
post(director,'AjouterEvenementServlet',event);eid=db("SELECT id_event FROM evenement WHERE titre='"+tag+"'");post(director,'AjouterEvenementServlet',dict(event,id=eid,description='Description modifiée'));post(director,'AjouterEvenementServlet',{'action':'participant','id':eid,'membre_id':accounts['memberId'],'responsabilite':'ORGANISATEUR'})
post(director,'ReunionServlet',{'titre':tag+' reunion','date_reunion':'2026-09-16T10:00','lieu':'Salle LIAS','ordre_du_jour':'Bilan','contenu':'Discussion'});rid=db("SELECT id_reunion FROM reunion WHERE titre='"+tag+" reunion'")
code,body=upload(director,'ReunionServlet',{'action':'pv','titre':'PV '+tag,'type_doc':'PV','reunion_id':rid});check(code==200 and 'Opération enregistrée' in body,'upload PV lié aux archives')
did=db('SELECT id_doc FROM document WHERE reunion_id='+rid+' ORDER BY id_doc DESC LIMIT 1')
r=director.open(BASE+'DownloadServlet?id='+did);check(r.read().startswith(b'%PDF-'),'téléchargement protégé PDF')
code,body=upload(member,'DocumentServlet',{'titre':'Archive '+tag,'type_doc':'RAPPORT','evenement_id':eid});check(code==200 and 'Opération enregistrée' in body,'document membre événement')
id1=db("SELECT id_doc FROM document WHERE titre='Archive "+tag+"'");code,body=upload(member,'DocumentServlet',{'titre':'Archive '+tag+' v2','type_doc':'RAPPORT','precedent_id':id1});check(code==200 and db('SELECT version FROM document WHERE precedent_id='+id1)=='2','nouvelle version conserve ancienne')
code,body=upload(member,'DocumentServlet',{'titre':'Invalide','type_doc':'RAPPORT'},'payload.pdf',b'<script>bad</script>');check('Document PDF requis.' in body,'contenu non PDF refusé')
post(director,'ConventionServlet',{'section':'partenaires','nom_institution':tag,'nom':'Partenaire','pays':'Maroc','contact':'Contact','email':'contact@example.test','telephone':'0102030405','description':'Coopération'});pid=db("SELECT id_partenaire FROM partenaire WHERE nom_institution='"+tag+"'")
post(director,'ConventionServlet',{'section':'conventions','titre':tag,'partenaire_id':pid,'date_debut':'2026-01-01','date_fin':'2026-12-31','description':'Convention de recherche','document_id':did});cid=db("SELECT id FROM convention WHERE titre='"+tag+"'")
post(director,'ConventionServlet',{'section':'activites','convention_id':cid,'titre':tag,'date_activite':'2026-09-15','description':'Atelier commun'})
post(doc,'AjouterPublicationServlet',{'titre':tag+' <script>alert(1)</script>','auteurs':'Doctorant HTTP','annee':'2026','journal_conference':'Journal','doi':'10.1234/test','lien_pdf':'https://example.test/paper'})
_,body,_=req(doc,'publications.jsp');check(tag+' &lt;script&gt;alert(1)&lt;/script&gt;' in body,'XSS publication échappé')
_,body,_=req(doc,'profil.jsp');code,body,_=req(doc,'AjouterEvenementServlet',dict(event,csrf=token(body)));check(code==403,'doctorant création événement refusée serveur')
_,body,_=req(member,'profil.jsp');code,body,_=req(member,'ModifierProfilServlet',{'csrf':token(body),'nom':'HTTP','prenom':'Membre','email':accounts['member'],'etablissement':'Faculté','laboratoire':'LIAS','biographie':'Biographie HTTP','centre_interet':'IA','profil_public':'1'});check('Biographie HTTP' in body,'profil modifié puis rechargé')
_,body,_=req(director,'rapport.jsp?annee=2026');check(tag in body,'rapport annuel inclut activités de l’année');_,body,_=req(director,'rapport.jsp?annee=2025');check(tag not in body.split('<main',1)[-1],'rapport exclut données autre année')
_,body,_=req(member,'recherche.jsp?q='+tag);check(tag in body,'recherche globale retrouve données')
post(director,'AjouterEvenementServlet',{'action':'archive','id':eid});post(director,'ConventionServlet',{'action':'archive','section':'conventions','id':cid});check(db('SELECT COUNT(*) FROM evenement WHERE id_event='+eid)=='1','archivage conserve données')
print('MUTATIONS HTTP PASSED')
