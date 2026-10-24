from history_support import *
a=accounts();d=login_as('director');uid=a['member.id'];team=a['team.id'];today=db('SELECT CURRENT_DATE')
audit_before=snapshot('audit_log');audit_start=max(map(int,audit_before))
closed_aff=snapshot('historique_affiliation','id_membre='+uid+' AND date_fin IS NOT NULL','id_historique')
closed_mandates=snapshot('mandat',"statut='TERMINE'",'id_mandat')
old_aff_ids=set(db('SELECT id_historique FROM historique_affiliation WHERE id_membre='+uid).splitlines())
old_mandate_ids=set(db('SELECT id_mandat FROM mandat WHERE id_membre='+uid).splitlines())
columns=[line.split('\t')[0] for line in db('SHOW COLUMNS FROM utilisateur').splitlines()]
columns=[c for c in columns if c not in ['statut_actuel','role_actuel','compte_statut','equipe_id','date_affiliation']]
query='SELECT JSON_ARRAY('+','.join('`'+c+'`' for c in columns)+') FROM utilisateur WHERE id='+uid
personal_before=db(query)
related_before={table:db('SELECT '+key+' FROM '+table+' WHERE '+owner+'='+uid+' ORDER BY '+key) for table,key,owner in [('publication','id_pub','id_auteur'),('document','id_doc','id_deposeur'),('demande_materiel','id_demande_mat','id_membre')]}
created_aff=[];created_mandates=[];checkpoints=[]
def checkpoint(label):
 check(db(query)==personal_before,label+' : données personnelles intégralement conservées')
 check(untouched(audit_before,snapshot('audit_log')),label+' : audit antérieur intact')
 checkpoints.append(int(db('SELECT MAX(id) FROM audit_log')))

for status in ['ASSOCIE','PERMANENT']:
 d.post('CompteServlet',dict(action='statut',id=uid,statut_actuel=status))
 created_aff.append(db('SELECT MAX(id_historique) FROM historique_affiliation WHERE id_membre='+uid))
 checkpoint('statut '+status)
chef=nominate(d,uid,'CHEF_EQUIPE',team);created_mandates.append(chef);checkpoint('nomination CHEF_EQUIPE')
d.post('MandatServlet',dict(action='cloturer',id=chef));checkpoint('clôture mandat chef')
member=nominate(d,uid,'MEMBRE_EQUIPE',team);created_mandates.append(member);checkpoint('nouveau mandat MEMBRE_EQUIPE')
check(db('SELECT statut FROM mandat WHERE id_mandat='+chef)=='TERMINE','mandat chef conservé et terminé')
check(db('SELECT statut FROM mandat WHERE id_mandat='+member)=='ACTIF','mandat membre distinct actif')
check(old_aff_ids|set(created_aff)<=set(db('SELECT id_historique FROM historique_affiliation WHERE id_membre='+uid).splitlines()),'toutes les affiliations anciennes et créées restent présentes')
check(old_mandate_ids|set(created_mandates)<=set(db('SELECT id_mandat FROM mandat WHERE id_membre='+uid).splitlines()),'tous les mandats anciens et créés restent présents')
check(len(set(created_aff))==2 and list(map(int,created_aff))==sorted(map(int,created_aff)),'ordre insertion des deux affiliations correct')
check(db('SELECT date_debut,date_fin FROM historique_affiliation WHERE id_historique='+created_aff[0])==today+'\t'+today,'période intermédiaire datée puis clôturée')
check(db('SELECT date_debut,date_fin FROM historique_affiliation WHERE id_historique='+created_aff[1])==today+'\tNULL','dernière période datée et ouverte')
check(untouched(closed_aff,snapshot('historique_affiliation','id_membre='+uid,'id_historique')),'affiliations antérieurement clôturées strictement inchangées')
check(untouched(closed_mandates,snapshot('mandat','1=1','id_mandat')),'mandats antérieurement clôturés strictement inchangés')
timeline=db('SELECT id,date_creation FROM audit_log WHERE id>'+str(audit_start)+' ORDER BY id').splitlines()
dates=[row.split('\t')[1] for row in timeline]
check(checkpoints==sorted(set(checkpoints)) and dates==sorted(dates),'ordre chronologique des audits cohérent (identifiants et horodatages)')
check(all(str(i) in snapshot('audit_log') for i in checkpoints),'audit de chaque étape toujours présent')
for table,key,owner in [('publication','id_pub','id_auteur'),('document','id_doc','id_deposeur'),('demande_materiel','id_demande_mat','id_membre')]:
 check(db('SELECT '+key+' FROM '+table+' WHERE '+owner+'='+uid+' ORDER BY '+key)==related_before[table],'données liées conservées : '+table)
nominate(d,a['chef.id'],'CHEF_EQUIPE',team)
d.post('EquipeServlet',dict(action='affecter',membre_id=uid,equipe_id=team))
check(db(query)==personal_before,'données personnelles conservées après restauration des rôles de recette')
check(untouched(audit_before,snapshot('audit_log')),'aucun audit initial modifié ou supprimé après scénario complet')
save_results('history-audit')
