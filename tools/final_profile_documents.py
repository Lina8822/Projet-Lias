from final_http import *
import struct,zlib

b=director()
tag='RECETTE-CATEGORIES-'+str(int(time.time()))
for category in ['FINANCEMENT','PROGRAMME','ATTESTATION','ADMINISTRATIF','AUTRE']:
 code,body,url=b.upload('DocumentServlet',{'titre':tag+' '+category,'type_doc':category})
 check(code==200 and 'Opération enregistrée avec succès.' in body,'upload catégorie '+category)
 ident=db('SELECT id_doc FROM document WHERE titre='+literal(tag+' '+category))
 with b.opener.open(BASE+'DownloadServlet?id='+ident) as response:
  check(response.status==200 and response.read().startswith(b'%PDF-'),'téléchargement catégorie '+category)

def chunk(kind,data):
 return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',1,1,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b'\0\x30\x70\x90'))+chunk(b'IEND',b'')
profile=dict(nom='Test',prenom='Directeur',email='directeur.test@lias.example.test',etablissement='LIAS - compte de recette',laboratoire='LIAS',biographie='Compte de développement utilisé pour la recette fonctionnelle.',centre_interet='Vérification fonctionnelle',profil_public='1')
code,body,url=b.upload('ModifierProfilServlet',profile,filename='photo.png',content=png)
check(code==200 and 'Opération enregistrée avec succès.' in body,'profil réel modifié et photo PNG chargée')
with b.opener.open(BASE+'PhotoServlet?id=4') as response:
 check(response.status==200 and response.read()==png,'photo PNG restituée sans altération')
check(db("SELECT COUNT(*) FROM audit_log WHERE action='PROFIL_MODIFIE' AND entite_id=4")!='0','historique modification profil conservé')
code,body,url=b.req('profil?id=4')
check(code==200 and profile['biographie'] in body and profile['centre_interet'] in body,'profil affiche les valeurs enregistrées')
(OUT/'profile-documents-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
