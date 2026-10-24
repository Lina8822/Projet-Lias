import urllib.request,urllib.parse,urllib.error,http.cookiejar,re,json,subprocess,time,uuid,os
from pathlib import Path
from test_environment import required_password
BASE=os.environ.get('LIAS_TEST_BASE','http://localhost:8080/Lias-Jee/')
OUT=Path(os.environ.get('LIAS_TEST_OUT','build/final-verification'));OUT.mkdir(parents=True,exist_ok=True)
results=[]
def check(ok,name):
 results.append({'test':name,'ok':bool(ok)});(OUT/'http-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
 print(('PASS ' if ok else 'FAIL ')+name,flush=True)
 if not ok:raise AssertionError(name)
def db(sql):
 return subprocess.check_output([os.environ.get('LIAS_TEST_MYSQL','C:/xampp/mysql/bin/mysql.exe'),'-h','127.0.0.1','-P',os.environ.get('LIAS_TEST_DB_PORT','3306'),'-u',os.environ.get('LIAS_TEST_DB_USER','root'),'--default-character-set=utf8mb4','-N','-B',os.environ.get('LIAS_TEST_DB','lias_data_base')],input=sql.encode('utf-8')).decode('utf-8').strip()
def literal(s):return "'"+str(s).replace("'","''")+"'"
class Browser:
 def __init__(self):self.jar=http.cookiejar.CookieJar();self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
 def req(self,path,data=None,headers=None):
  if isinstance(data,dict):data=urllib.parse.urlencode(data).encode()
  try:
   r=self.opener.open(urllib.request.Request(BASE+path,data,headers or {}),timeout=25);return r.status,r.read().decode('utf-8'),r.url
  except urllib.error.HTTPError as e:return e.code,e.read().decode('utf-8'),e.url
 def token(self,page='profil.jsp'):
  code,body,_=self.req(page);m=re.search(r'name="csrf" value="([^"]+)"',body)
  if not m:raise AssertionError('Missing CSRF token '+page+' '+str(code))
  return m.group(1)
 def login(self,email,password):
  code,body,url=self.req('LoginServlet',dict(email=email,password=password,csrf=self.token('login.jsp')))
  return code,body,url
 def post(self,path,values,expect=True):
  code,body,url=self.req(path,dict(values,csrf=self.token()))
  if expect:check(code==200 and 'Opération enregistrée avec succès.' in body,path+' '+values.get('action','save'))
  return code,body,url
 def upload(self,path,values,filename='recette.pdf',content=b'%PDF-1.4\n1 0 obj<<>>endobj\n%%EOF',photo=False):
  boundary=uuid.uuid4().hex;values=dict(values,csrf=self.token('candidature.jsp' if path=='CandidatureServlet' else 'profil.jsp'))
  parts=[('--'+boundary+'\r\nContent-Disposition: form-data; name="'+k+'"\r\n\r\n'+str(v)+'\r\n').encode() for k,v in values.items()]
  parts+=[('--'+boundary+'\r\nContent-Disposition: form-data; name="fichier"; filename="'+filename+'"\r\nContent-Type: application/octet-stream\r\n\r\n').encode()+content+b'\r\n',('--'+boundary+'--\r\n').encode()]
  return self.req(path,b''.join(parts),{'Content-Type':'multipart/form-data; boundary='+boundary})
def director():
 b=Browser();code,body,url=b.login('directeur.test@lias.example.test',required_password("LIAS_TEST_DIRECTOR_PASSWORD"));check(code==200 and url.endswith('/dashboard.jsp'),'login réel directeur / dashboard 200');return b
if __name__=='__main__':
 b=director();public=Browser()
 for path in ['','login','candidature','membres','publications','evenements','equipes']:
  code,body,url=public.req(path);check(code==200,'public GET /'+path)
 for path in ['dashboard','membres','profil','equipes','gouvernance','demandes','evenements','publications','documents','materiel','conventions','reunions','messagerie','calendrier','rapport','notifications','recherche','historique']:
  code,body,url=b.req(path);(OUT/(path+'.html')).write_text(body,encoding='utf-8');check(code==200,'directeur GET /'+path)
 code,_,_=b.req('AjouterPublicationServlet',{'titre':'sans csrf'});check(code==403,'POST sans CSRF refusé')
 check(db('SELECT COUNT(*) FROM utilisateur WHERE id IN (1,2,3,4)')=='4','comptes antérieurs conservés')

