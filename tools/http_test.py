import urllib.request,urllib.parse,urllib.error,http.cookiejar,re,json
from pathlib import Path
from test_environment import required_password
BASE='http://127.0.0.1:18080/LaboJEE/'
accounts=dict(line.split('=',1) for line in Path('build/test-accounts.properties').read_text().splitlines())
checks=[]
def check(test,name):
 checks.append((bool(test),name));print(('PASS ' if test else 'FAIL ')+name)
 if not test:raise AssertionError(name)
def session():return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def req(s,path,data=None):
 try:
  r=s.open(BASE+path,None if data is None else urllib.parse.urlencode(data).encode(),timeout=25);return r.status,r.read().decode('utf-8'),r.url
 except urllib.error.HTTPError as e:return e.code,e.read().decode('utf-8'),e.url
def token(body):
 m=re.search(r'name="csrf" value="([^"]+)"',body);assert m,body[:200];return m.group(1)
def login(role):
 s=session();code,b,url=req(s,'login.jsp');check(code==200,'login page '+role)
 code,b,url=req(s,'LoginServlet',{'csrf':token(b),'email':accounts[role],'password':required_password()})
 check(code==200 and url.endswith('/dashboard.jsp'),'login '+role);return s,b
s=session()
for page in ['index','login','candidature','equipes','membres','evenements','publications']:
 code,b,url=req(s,page+'.jsp');check(code==200,'public '+page)
code,b,url=req(s,'documents.jsp');check(url.endswith('/login.jsp'),'URL interne sans session redirige')
code,b,url=req(s,'AjouterPublicationServlet',{'titre':'intrusion'});check(code in (200,403) and ('Connexion' in b or code==403),'POST sans session protégé')
code,b,url=req(s,'LoginServlet',{'email':'a','password':'b'});check(code==403,'CSRF manquant refusé')
for role in ['director','member','doctorant','associe','admin']:
 s,b=login(role)
 allowed=['dashboard','profil','membres','equipes','publications','evenements','notifications','recherche']
 if role in ['director','member','admin']:allowed+=['documents','materiel','conventions','reunions','messagerie','calendrier']
 if role=='associe':allowed+=['calendrier']
 if role in ['director','admin']:allowed+=['gouvernance','historique']
 if role=='director':allowed+=['demandes','rapport']
 for page in allowed:
  code,b,url=req(s,page+'.jsp');Path('build/http-'+role+'-'+page+'.html').write_text(b,encoding='utf-8');check(code==200 and 'Les données ne sont pas disponibles' not in b,role+' GET '+page)
 if role=='doctorant':
  for page in ['documents','materiel','gouvernance','demandes','reunions','rapport','messagerie','calendrier']:
   code,b,url=req(s,page+'.jsp');check(code==403,'doctorant URL interdite '+page)
 if role!='director':
  code,b,url=req(s,'profil.jsp');csrf=token(b);code,b,url=req(s,'DemandeServlet',{'csrf':csrf,'id':'1','decision':'ACCEPTEE'});check(code==403,role+' décision adhésion interdite')
 code,b,url=req(s,'profil.jsp');code,b,url=req(s,'LogoutServlet',{'csrf':token(b)});check(url.endswith('/login.jsp'),'logout '+role)
 code,b,url=req(s,'dashboard.jsp');check(url.endswith('/login.jsp'),'session invalidée '+role)
for role in ['retraite','ancien']:
 s=session();_,b,_=req(s,'login.jsp');code,b,url=req(s,'LoginServlet',{'csrf':token(b),'email':accounts[role],'password':required_password()});check('Identifiants incorrects ou compte inactif.' in b,role+' login bloqué')
Path('build/http-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2),encoding='utf-8')
print('TOTAL',len(checks),'assertions HTTP')

