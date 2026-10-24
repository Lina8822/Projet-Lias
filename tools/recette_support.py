from final_http import *

def accounts():
 return dict(re.sub(r'\\(.)',r'\1',line).split('=',1) for line in Path(os.environ.get('LIAS_TEST_ACCOUNTS','build/test-accounts.properties')).read_text().splitlines() if line and not line.startswith('#'))

def login_as(role):
 a=accounts(); b=Browser();code,body,url=b.login(a[role],a[role+'.password'])
 check(code==200 and url.endswith('/dashboard.jsp'),role+' connexion réelle')
 return b

def save_results(name):
 (OUT/(name+'-results.json')).write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')

def audit_ids():
 return set(db('SELECT id FROM audit_log').splitlines())
