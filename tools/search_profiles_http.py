from recette_support import *
for role in ['director','admin','vice','chef','member','associe','doctorant']:
 b=login_as(role);code,body,url=b.req('recherche?q=RECETTE')
 check(code==200,role+' recherche réelle')
 headings=re.findall(r'<h2[^>]*>(.*?)</h2>',body)
 check('Publications' in headings,role+' bloc publications présent')
 check(('Membres' not in headings and 'Événements' not in headings) if role=='doctorant' else ('Membres' in headings and 'Événements' in headings),role+' filtrage membres/événements')
 check(('Archives documentaires' not in headings) if role in ['doctorant','associe'] else 'Archives documentaires' in headings,role+' filtrage archives internes')
save_results('search-profiles')
