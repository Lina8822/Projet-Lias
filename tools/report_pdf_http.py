from recette_support import *
b=login_as('director');year=time.strftime('%Y')
for role in ['director','vice']:
 b=login_as(role)
 with b.opener.open(BASE+'RapportPdfServlet?annee='+year) as response:
  pdf=response.read();check(response.status==200 and response.headers.get_content_type()=='application/pdf' and pdf.startswith(b'%PDF-') and len(pdf)>1000,role+' PDF annuel réel')
  if role=='director':(OUT/'rapport-annuel.pdf').write_bytes(pdf)
 code,body,url=b.req('rapport?annee='+year);check('Exporter en PDF' in body,role+' bouton export visible')
 check(b.req('RapportPdfServlet?annee=invalid')[0]==400,'année PDF invalide refusée')
for role in ['admin','member','associe','doctorant']:
 b=login_as(role);check(b.req('RapportPdfServlet')[0]==403,role+' PDF interdit côté serveur')
check(db("SELECT COUNT(*) FROM audit_log WHERE action='EXPORT_RAPPORT'")!='0','export PDF audité')
save_results('pdf')
