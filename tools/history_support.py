from recette_support import *
import datetime

def snapshot(table,where='1=1',key='id'):
 data=db('SELECT * FROM '+table+' WHERE '+where+' ORDER BY '+key)
 return {line.split('\t')[0]:line for line in data.splitlines()}

def untouched(before,after):
 return all(after.get(key)==value for key,value in before.items())

def nominate(d,user,role,team=None):
 values=dict(action='save',membre_id=user,type_mandat=role,date_debut=db('SELECT CURRENT_DATE'))
 if team:values['equipe_id']=team
 d.post('MandatServlet',values)
 return db('SELECT MAX(id_mandat) FROM mandat WHERE id_membre='+str(user)+' AND type_mandat='+literal(role))

def stable_mandate(mid):
 return db('SELECT id_mandat,id_membre,type_mandat,date_debut,equipe_id FROM mandat WHERE id_mandat='+mid)
