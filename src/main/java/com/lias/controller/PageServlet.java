package com.lias.controller;
import jakarta.servlet.*;import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.*;import java.sql.*;import java.util.*;
import com.lias.dao.*;import com.lias.model.Membre;import com.lias.model.Module;import com.lias.model.Module.Field;import com.lias.security.*;import com.lias.view.Page;import com.lias.view.Page.*;
@WebServlet(urlPatterns={"","/index","/login","/candidature","/dashboard","/profil","/membres","/equipes","/gouvernance","/demandes","/evenements","/publications","/documents","/materiel","/conventions","/reunions","/messagerie","/calendrier","/rapport","/notifications","/recherche","/historique","/index.jsp","/login.jsp","/candidature.jsp","/dashboard.jsp","/profil.jsp","/membres.jsp","/equipes.jsp","/gouvernance.jsp","/demandes.jsp","/evenements.jsp","/publications.jsp","/documents.jsp","/materiel.jsp","/conventions.jsp","/reunions.jsp","/messagerie.jsp","/calendrier.jsp","/rapport.jsp","/notifications.jsp","/recherche.jsp","/historique.jsp"})
public class PageServlet extends HttpServlet {
 private final PortalDAO dao=new PortalDAO();
 protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {render(r,s);}
 protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {render(r,s);}
 private void render(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {
  String key=PageRoutes.key(r.getServletPath()); if(key==null){s.sendError(404);return;} Object candidate=r.getSession().getAttribute("user"); Membre u=!Set.of("index","login","candidature").contains(key)&&candidate instanceof Membre?(Membre)candidate:null; if(!Access.read(u,key)){s.sendError(403);return;} Page p=new Page(key,title(key));
  try {
   if(u!=null)p.notifications=dao.notifications(u);
   if(!Set.of("index","login","candidature").contains(key))choices(p,u);
   switch(key){
    case "login","candidature"->{}
    case "index"->{try{var settings=Sql.rows("SELECT valeur FROM parametre WHERE cle='date_creation_laboratoire'");if(!settings.isEmpty())p.profile.put("date_creation",settings.get(0).get("valeur"));}catch(Exception unavailable){getServletContext().log("Date laboratoire indisponible",unavailable);}}
    case "dashboard"->{p.stats=dao.stats(u);p.calendar="DOCTORANT".equals(u.getStatutActuel())?List.of():dao.calendar(u);block(p,"Publications par année",cols("annee","Année","total","Publications"),Sql.rows("SELECT annee,COUNT(*) total FROM publication GROUP BY annee ORDER BY annee DESC"));if(Access.internal(u))block(p,"Membres par statut",cols("statut_actuel","Statut","total","Effectif"),Sql.rows("SELECT statut_actuel,COUNT(*) total FROM utilisateur GROUP BY statut_actuel"));}
    case "equipes","evenements","publications","reunions"->{module(p,u,r,Module.get(key));extras(p,u,key);}
    case "conventions"->{for(String section:List.of("partenaires","conventions","activites"))module(p,u,r,Module.get(section));}
    case "membres"->members(p,u);
    case "profil"->profile(p,u,r);
    case "gouvernance"->{block(p,"Mandats actuels et chronologie",cols("membre","Responsable","type_mandat","Rôle","equipe","Équipe","date_debut","Début","date_fin","Fin","etat_calcule","État"),dao.mandates());form(p,"Nommer une responsabilité","MandatServlet","save",ref("membre_id","Membre permanent","membres",true),f("type_mandat","Rôle","select",true,"DIRECTEUR,VICE_DIRECTEUR,CHEF_EQUIPE,MEMBRE_EQUIPE"),ref("equipe_id","Équipe (chef / membre)","equipes",false),f("date_debut","Prise d’effet","date",true,""),f("date_fin","Fin prévue","date",false,""));form(p,"Clôturer un mandat","MandatServlet","cloturer",ref("id","Mandat","mandats",true));block(p,"Historique des affiliations",cols("membre","Membre","ancien_statut","Statut","affiliation","Affiliation","date_debut","Début","date_fin","Fin"),dao.affiliations(null));}
    case "demandes"->applications(p);
    case "documents"->{documents(p,dao.documents());documentForm(p,"Ajouter une archive ou une version","DocumentServlet","save",false);}
    case "materiel"->material(p,u);
    case "messagerie"->chat(p,u,r);
    case "calendrier"->p.calendar=dao.calendar(u);
    case "notifications"->notifications(p);
    case "recherche"->search(p,u,r);
    case "rapport"->report(p,r);
    case "historique"->block(p,"Journal des opérations",cols("date_creation","Date","acteur","Acteur","action","Action","entite","Entité","entite_id","Identifiant","avant","Avant","apres","Après"),Sql.rows("SELECT a.*,CONCAT(u.prenom,' ',u.nom) acteur FROM audit_log a LEFT JOIN utilisateur u ON u.id=a.acteur_id ORDER BY a.id DESC"));
    default->throw new IllegalArgumentException("Page inconnue");
   }
  }catch(SecurityException e){s.sendError(403);return;}catch(IllegalArgumentException e){r.setAttribute("erreur",e.getMessage());s.setStatus(400);}catch(Exception e){getServletContext().log("Page load: "+key,e);r.setAttribute("erreur","Les données ne sont pas disponibles. Vérifiez la connexion MySQL et la migration SQL.");s.setStatus(503);}
  r.setAttribute("page",p);r.getRequestDispatcher("/WEB-INF/views/"+(Set.of("index","login","candidature").contains(key)?key:"portal")+".jsp").forward(r,s);
 }
 private void choices(Page p,Membre u)throws SQLException {
  p.choices.put("equipes",Sql.rows("SELECT id,nom label FROM equipe ORDER BY nom"));
  if(u!=null)p.choices.put("membres",Sql.rows("SELECT id,CONCAT(prenom,' ',nom) label FROM utilisateur WHERE compte_statut='ACTIF' ORDER BY nom"));
  if(Access.assignTeam(u))p.choices.put("membres_affectables",Sql.rows("SELECT id,CONCAT(prenom,' ',nom) label FROM utilisateur WHERE compte_statut='ACTIF' AND (statut_actuel IN ('PERMANENT','ASSOCIE','DOCTORANT') OR statut_actuel IS NULL OR TRIM(statut_actuel)='') ORDER BY nom"));
  if(Access.director(u)||Access.admin(u))p.choices.put("mandats",Sql.rows("SELECT m.id_mandat id,CONCAT(u.prenom,' ',u.nom,' · ',m.type_mandat) label FROM mandat m JOIN utilisateur u ON u.id=m.id_membre WHERE m.statut='ACTIF'"));
  if(Access.internal(u)){
   p.choices.put("documents",Sql.rows("SELECT id_doc id,CONCAT(titre,' · v',version) label FROM document ORDER BY id_doc DESC"));p.choices.put("partenaires",Sql.rows("SELECT id_partenaire id,nom_institution label FROM partenaire ORDER BY nom_institution"));p.choices.put("conventions",Sql.rows("SELECT id,titre label FROM convention"));p.choices.put("materiel",Sql.rows("SELECT id,CONCAT(nom,' · stock ',quantite) label FROM materiel"));p.choices.put("reunions",Sql.rows("SELECT id_reunion id,titre label FROM reunion"));
  }
  p.choices.put("evenements",Sql.rows("SELECT id_event id,titre label FROM evenement WHERE statut<>'ARCHIVE'"));
 }
 private void module(Page p,Membre u,HttpServletRequest r,Module m)throws SQLException {
  var data=dao.content(m,u);Map<String,String> columns=new LinkedHashMap<>();for(Field f:m.fields())columns.put(f.name(),f.label());if(m.state()!=null)columns.put(m.state(),"État");
  List<Row> rows=new ArrayList<>();for(var item:data){List<Action> actions=new ArrayList<>();boolean canEdit=Access.write(u,m.permission())&&(!m.key().equals("publications")||Access.direction(u)||Sql.id(item.get("id_auteur"))==u.getIdMembre());String id=Sql.str(item.get(m.pk()));
   if(canEdit)actions.add(link("Modifier",p.key+".jsp",Map.of("edit",id,"section",m.key())));
   if(Access.direction(u)&&m.state()!=null&&!"ARCHIVE".equals(item.get(m.state())))actions.add(post("Archiver",m.endpoint(),Map.of("action","archive","id",id,"section",m.key()),"Archiver cet élément ? L’historique sera conservé."));
   if(m.key().equals("publications")&&Sql.str(item.get("lien_pdf")).matches("https?://.+"))actions.add(link("Consulter",Sql.str(item.get("lien_pdf")),Map.of()));
   Map<String,Object> display=new LinkedHashMap<>(item);for(Field f:m.fields())if(f.type().equals("reference")){String refid=Sql.str(item.get(f.name()));display.put(f.name(),p.choices.getOrDefault(f.options(),List.of()).stream().filter(o->Sql.str(o.get("id")).equals(refid)).map(o->o.get("label")).findFirst().orElse(""));}rows.add(new Row(display,actions));
  }p.blocks.add(new Block(m.title(),columns,rows));
  if(Access.write(u,m.permission())){
   Map<String,Object> values=new HashMap<>();Map<String,String> hidden=new HashMap<>(Map.of("action","save","section",m.key()));
   if(m.key().equals(r.getParameter("section"))&&r.getParameter("edit")!=null){int id=Integer.parseInt(r.getParameter("edit"));var existing=data.stream().filter(d->Sql.id(d.get(m.pk()))==id).findFirst().orElseThrow(()->new IllegalArgumentException("Élément introuvable"));if(m.key().equals("publications"))Access.require(Access.direction(u)||Sql.id(existing.get("id_auteur"))==u.getIdMembre());values.putAll(existing);hidden.put("id",String.valueOf(id));}
   var fields=m.fields().stream().filter(f->!f.name().equals("document_id")||Access.internal(u)).toList();
   p.forms.add(new Form((values.isEmpty()?"Ajouter · ":"Modifier · ")+m.title(),m.endpoint(),fields,values,hidden,false));
  }
 }
 private void extras(Page p,Membre u,String key)throws SQLException {
  if(key.equals("equipes")){
   if(Access.assignTeam(u))form(p,"Affecter un membre","EquipeServlet","affecter",ref("membre_id","Membre","membres_affectables",true),ref("equipe_id","Équipe","equipes",true));
   block(p,"Composition des équipes",cols("equipe","Équipe","membre","Membre","role","Responsabilité"),Sql.rows("SELECT e.nom equipe,CONCAT(u.prenom,' ',u.nom) membre,"+PortalDAO.CURRENT_ROLE+" role FROM utilisateur u JOIN equipe e ON e.id=u.equipe_id"+(u==null?" WHERE u.profil_public=TRUE":"")+" ORDER BY e.nom,u.nom"));
  }
  if(Set.of("evenements","reunions").contains(key)){boolean meeting=key.equals("reunions");
   if(Access.direction(u)){form(p,"Ajouter un participant / organisateur",meeting?"ReunionServlet":"AjouterEvenementServlet","participant",ref("id",meeting?"Réunion":"Événement",key,true),ref("membre_id","Membre","membres",true),meeting?f("note","Information facultative","text",false,""):f("responsabilite","Responsabilité","select",true,"PARTICIPANT,ORGANISATEUR"));if(meeting)documentForm(p,"Déposer un procès-verbal","ReunionServlet","pv",true);}
   String sql=meeting?"SELECT r.titre activite,CONCAT(u.prenom,' ',u.nom) membre,'PARTICIPANT' responsabilite FROM reunion_participant p JOIN reunion r ON r.id_reunion=p.reunion_id JOIN utilisateur u ON u.id=p.membre_id":"SELECT e.titre activite,CONCAT(u.prenom,' ',u.nom) membre,p.responsabilite FROM evenement_membre p JOIN evenement e ON e.id_event=p.evenement_id JOIN utilisateur u ON u.id=p.membre_id"+(u==null?" WHERE u.profil_public=TRUE":"");block(p,"Participants et organisateurs",cols("activite","Activité","membre","Membre","responsabilite","Responsabilité"),Sql.rows(sql));
   if(meeting)documents(p,Sql.rows("SELECT d.*,CONCAT(u.prenom,' ',u.nom) auteur FROM document d LEFT JOIN utilisateur u ON u.id=d.id_deposeur WHERE reunion_id IS NOT NULL ORDER BY date_depot DESC"));
  }
 }
 private void members(Page p,Membre u)throws SQLException {
  if(Access.admin(u))form(p,"Paramétrage du laboratoire","CompteServlet","parametre",f("date_creation","Date de création du LIAS","date",true,""));
  List<Row> rows=new ArrayList<>();for(var row:dao.members(u))rows.add(new Row(row,u==null?List.of():List.of(link("Profil","profil.jsp",Map.of("id",Sql.str(row.get("id")))))));
  p.blocks.add(new Block("Annuaire du laboratoire",cols("photo","Photo","prenom","Prénom","nom","Nom","email","Email","statut_actuel","Statut","role_actuel","Rôle","equipe","Équipe","etablissement","Établissement","laboratoire","Laboratoire d’origine","date_affiliation","Affiliation"),rows));
  if(Access.director(u)||Access.admin(u)){
   block(p,"Données confidentielles (direction / administration)",cols("nom","Nom","prenom","Prénom","date_naissance","Naissance","date_embauche","Embauche","publication_autorisee","Publication associée autorisée"),Sql.rows("SELECT nom,prenom,date_naissance,date_embauche,publication_autorisee FROM utilisateur ORDER BY nom"));
   p.choices.put("tous_membres",Sql.rows("SELECT id,CONCAT(prenom,' ',nom,' · ',statut_actuel) label FROM utilisateur ORDER BY nom"));
   form(p,"Départ, retraite ou réactivation","CompteServlet","statut",ref("id","Membre","tous_membres",true),f("statut_actuel","Nouveau statut","select",true,"PERMANENT,ASSOCIE,DOCTORANT,RETRAITE,ANCIEN_MEMBRE"));
   form(p,"Données confidentielles et droit de publication","CompteServlet","permission",ref("id","Membre","tous_membres",true),f("publication_autorisee","Publication autorisée pour associé","select",true,"0,1"),f("date_naissance","Date de naissance (confidentielle)","date",false,""),f("date_embauche","Date d’embauche","date",false,""));
  }
 }
 private void profile(Page p,Membre u,HttpServletRequest r)throws SQLException {
  int id=r.getParameter("id")==null?u.getIdMembre():Integer.parseInt(r.getParameter("id"));boolean self=id==u.getIdMembre();
  if(self)form(p,"Changer mon mot de passe","ModifierProfilServlet","password",f("ancien_password","Mot de passe actuel","password",true,""),f("password","Nouveau mot de passe (12 caractères minimum)","password",true,""));
  p.profile=Sql.rows("SELECT u.id,u.nom,u.prenom,u.email,u.photo,u.statut_actuel,u.etablissement,u.laboratoire,u.biographie,u.centre_interet,u.date_affiliation,u.profil_public,u.equipe_id,e.nom equipe,"+PortalDAO.CURRENT_ROLE+" role FROM utilisateur u LEFT JOIN equipe e ON e.id=u.equipe_id WHERE u.id=?",id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Profil introuvable."));
  block(p,"Publications du membre",cols("titre","Titre","auteurs","Auteurs","annee","Année","journal_conference","Revue / conférence","doi","DOI"),Sql.rows("SELECT titre,auteurs,annee,journal_conference,doi FROM publication WHERE id_auteur=? ORDER BY annee DESC",id));
  if(self)block(p,"Mes données confidentielles",cols("date_naissance","Naissance","date_embauche","Embauche"),Sql.rows("SELECT date_naissance,date_embauche FROM utilisateur WHERE id=?",id));
  if(self)p.forms.add(new Form("Modifier mon profil","ModifierProfilServlet",List.of(f("nom","Nom","text",true,""),f("prenom","Prénom","text",true,""),f("email","Email","email",true,""),f("etablissement","Établissement","text",true,""),f("laboratoire","Laboratoire d’origine","text",true,""),f("biographie","Biographie","textarea",false,""),f("centre_interet","Centres d’intérêt","textarea",false,""),f("profil_public","Visible dans l’annuaire public","select",true,"1,0"),f("fichier","Photo PNG / JPEG (10 Mo max.)","file",false,"image/png,image/jpeg")),p.profile,Map.of("action","save"),true));
  if(self||Access.director(u)||Access.admin(u)){block(p,"Périodes d’affiliation",cols("ancien_statut","Statut","affiliation","Affiliation","date_debut","Début","date_fin","Fin"),dao.affiliations(id));block(p,"Responsabilités exercées",cols("type_mandat","Rôle","date_debut","Début","date_fin","Fin","etat_calcule","État"),dao.mandates().stream().filter(m->Sql.id(m.get("id_membre"))==id).toList());}
 }
 private void applications(Page p)throws SQLException {
  p.choices.put("anciens_dossiers",Sql.rows("SELECT id_demande id,CONCAT(prenom,' ',nom) label FROM demandes_adhesion WHERE etat_demande='EN_ATTENTE' AND (password_hash IS NULL OR password_hash='')"));
  if(!p.choices.get("anciens_dossiers").isEmpty())p.forms.add(new Form("Accepter un ancien dossier sans mot de passe","DemandeServlet",List.of(ref("id","Dossier","anciens_dossiers",true),f("password","Mot de passe initial à communiquer au candidat (12 caractères minimum)","password",true,"")),Map.of(),Map.of("decision","ACCEPTEE"),false));

  var data=Sql.rows("SELECT id_demande,nom,prenom,email,etablissement,laboratoire,statut_demande,motivation,date_soumission,etat_demande,cv,decision_date FROM demandes_adhesion ORDER BY date_soumission DESC");List<Row> rows=new ArrayList<>();
  for(var d:data){String id=Sql.str(d.get("id_demande"));List<Action> actions=new ArrayList<>();if(!Sql.str(d.get("cv")).isBlank())actions.add(link("CV","DownloadServlet",Map.of("id",id,"kind","cv")));if("EN_ATTENTE".equals(d.get("etat_demande")))for(String state:List.of("ACCEPTEE","REFUSEE"))actions.add(post(state.equals("ACCEPTEE")?"Accepter":"Refuser","DemandeServlet",Map.of("id",id,"decision",state),"Confirmer cette décision d’adhésion ?"));rows.add(new Row(d,actions));}
  p.blocks.add(new Block("Dossiers et décisions",cols("prenom","Prénom","nom","Nom","email","Email","etablissement","Établissement","laboratoire","Laboratoire","statut_demande","Statut demandé","motivation","Motivation","date_soumission","Soumission","etat_demande","Décision","decision_date","Date de décision"),rows));
  block(p,"Notifications aux candidats (registre)",cols("email","Destinataire","message","Notification","date_creation","Date"),Sql.rows("SELECT email,message,date_creation FROM notification_candidat ORDER BY id DESC"));
 }
 private void documents(Page p,List<Map<String,Object>> data){List<Row> rows=new ArrayList<>();for(var d:data)rows.add(new Row(d,List.of(link("Télécharger","DownloadServlet",Map.of("id",Sql.str(d.get("id_doc")))))));p.blocks.add(new Block("Archives documentaires",cols("titre","Nom","type_doc","Catégorie","auteur","Déposé par","date_depot","Date","evenement","Événement","reunion","Réunion","version","Version"),rows));}
 private void documentForm(Page p,String title,String endpoint,String action,boolean pv){List<Field> fs=new ArrayList<>(List.of(f("titre","Nom du document","text",true,""),f("type_doc","Catégorie","select",true,"FINANCEMENT,PROGRAMME,ATTESTATION,RAPPORT,ADMINISTRATIF,PV,AUTRE"),ref("evenement_id","Événement associé","evenements",false),ref("precedent_id","Document précédent (nouvelle version)","documents",false)));if(pv)fs.add(ref("reunion_id","Réunion","reunions",true));fs.add(f("fichier","Fichier PDF (10 Mo max.)","file",true,"application/pdf"));p.forms.add(new Form(title,endpoint,fs,Map.of(),Map.of("action",action),true));}
 private void material(Page p,Membre u)throws SQLException {
  block(p,"Inventaire",cols("nom","Matériel","quantite","Stock disponible","etat","État"),Sql.rows("SELECT * FROM materiel ORDER BY nom"));
  var data=Sql.rows("SELECT d.*,CONCAT(u.prenom,' ',u.nom) membre FROM demande_materiel d JOIN utilisateur u ON u.id=d.id_membre"+(Access.direction(u)?"":" WHERE d.id_membre=?")+" ORDER BY date_demande DESC",Access.direction(u)?new Object[]{}:new Object[]{u.getIdMembre()});List<Row> rows=new ArrayList<>();
  for(var d:data){List<Action> actions=new ArrayList<>();if(Access.direction(u)){List<String> states="EN_ATTENTE".equals(d.get("etat_demande"))?List.of("ACCEPTEE","REFUSEE"):"ACCEPTEE".equals(d.get("etat_demande"))?List.of("LIVREE"):List.of();for(String state:states)actions.add(post(state,"AjouterMaterielServlet",Map.of("action","traiter","id",Sql.str(d.get("id_demande_mat")),"decision",state),"Confirmer le traitement de cette demande ?"));}rows.add(new Row(d,actions));}
  p.blocks.add(new Block("Demandes de matériel",cols("membre","Demandeur","nom_materiel","Matériel","quantite","Quantité","description","Justification","date_demande","Date","etat_demande","État"),rows));
  form(p,"Exprimer un besoin","AjouterMaterielServlet","demander",ref("materiel_id","Matériel existant","materiel",false),f("nom_materiel","Ou nouveau matériel souhaité","text",false,""),f("quantite","Quantité","number",true,""),f("description","Justification","textarea",true,""));
  if(Access.direction(u)){
   p.choices.put("besoins",Sql.rows("SELECT id_demande_mat id,CONCAT(nom_materiel,' · demande #',id_demande_mat) label FROM demande_materiel WHERE etat_demande IN ('EN_ATTENTE','ACCEPTEE')"));
   form(p,"Rattacher un besoin à l’inventaire","AjouterMaterielServlet","rattacher",ref("id","Demande","besoins",true),ref("materiel_id","Matériel","materiel",true));
   form(p,"Actualiser l’état du matériel","AjouterMaterielServlet","etat",ref("materiel_id","Matériel","materiel",true),f("etat","État","select",true,"DISPONIBLE,MAINTENANCE,HORS_SERVICE"));
   form(p,"Référencer du matériel","AjouterMaterielServlet","stock",f("nom","Désignation","text",true,""),f("etat","État","select",true,"DISPONIBLE,MAINTENANCE,HORS_SERVICE"));
   form(p,"Enregistrer un arrivage","AjouterMaterielServlet","arrivage",ref("materiel_id","Matériel","materiel",true),f("quantite","Quantité reçue","number",true,""));
   form(p,"Distribuer du matériel","AjouterMaterielServlet","distribuer",ref("materiel_id","Matériel","materiel",true),ref("membre_id","Bénéficiaire","membres",true),f("quantite","Quantité","number",true,""));
   block(p,"Historique des arrivages",cols("nom","Matériel","quantite","Quantité","date_arrivage","Date"),Sql.rows("SELECT a.*,m.nom FROM arrivage a JOIN materiel m ON m.id=a.materiel_id ORDER BY a.id DESC"));
   block(p,"Distributions et bénéficiaires",cols("nom","Matériel","membre","Bénéficiaire","quantite","Quantité","date_distribution","Date"),Sql.rows("SELECT d.*,m.nom,CONCAT(u.prenom,' ',u.nom) membre FROM distribution_materiel d JOIN materiel m ON m.id=d.materiel_id JOIN utilisateur u ON u.id=d.membre_id ORDER BY d.id DESC"));
   block(p,"Membres sans dotation",cols("membre","Membre","nom","Matériel non reçu"),Sql.rows("SELECT CONCAT(u.prenom,' ',u.nom) membre,m.nom FROM utilisateur u CROSS JOIN materiel m WHERE u.compte_statut='ACTIF' AND u.statut_actuel='PERMANENT' AND NOT EXISTS (SELECT 1 FROM distribution_materiel d WHERE d.membre_id=u.id AND d.materiel_id=m.id) ORDER BY u.nom,m.nom"));
  }
 }
 private void chat(Page p,Membre u,HttpServletRequest r)throws SQLException {
  Integer to=optional(r,"destinataire_id"),team=optional(r,"equipe_id"),event=optional(r,"evenement_id");if((to==null?0:1)+(team==null?0:1)+(event==null?0:1)>1)throw new IllegalArgumentException("Sélectionnez une seule conversation.");
  p.chat=dao.messages(u,to,team,event);p.choices.put("mes_equipes",Sql.rows("SELECT e.id,e.nom label FROM equipe e JOIN utilisateur u ON u.equipe_id=e.id WHERE u.id=?",u.getIdMembre()));
  Map<String,Object> values=new HashMap<>();values.put("destinataire_id",to);values.put("equipe_id",team);values.put("evenement_id",event);
  p.forms.add(new Form("Envoyer un message","MessageServlet",List.of(ref("destinataire_id","Destinataire (vide : laboratoire)","membres",false),ref("equipe_id","Ou équipe","mes_equipes",false),ref("evenement_id","Ou événement","evenements",false),f("contenu","Message","textarea",true,"")),values,Map.of("action","save"),false));
 }
 private void notifications(Page p){List<Row> rows=new ArrayList<>();for(var n:p.notifications){List<Action> actions=new ArrayList<>();actions.add(link("Ouvrir",safeLink(n.get("lien")),Map.of()));if("NON LUE".equals(n.get("lecture")))actions.add(post("Marquer comme lue","NotificationServlet",Map.of("id",Sql.str(n.get("id_notif"))),""));rows.add(new Row(n,actions));}p.blocks.add(new Block("Toutes les notifications",cols("message","Message","date_creation","Date","lecture","Lecture"),rows));}
 public static String safeLink(Object link){String value=Sql.str(link);return value.matches("[a-z]+\\.jsp(\\?[a-z_]+=[0-9]+)?")?value:"dashboard.jsp";}
 private void search(Page p,Membre u,HttpServletRequest r)throws SQLException {
  String query=Optional.ofNullable(r.getParameter("q")).orElse("").trim();if(query.length()>200)throw new IllegalArgumentException("Recherche trop longue.");p.subtitle="Résultats pour « "+query+" »";if(query.isEmpty())return;String q="%"+query.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
  if(!"DOCTORANT".equals(u.getStatutActuel()))block(p,"Membres",cols("nom","Nom","prenom","Prénom","email","Email"),Sql.rows("SELECT nom,prenom,email FROM utilisateur WHERE nom LIKE ? ESCAPE '!' OR prenom LIKE ? ESCAPE '!'",q,q));
  block(p,"Publications",cols("titre","Titre","auteurs","Auteurs","annee","Année"),Sql.rows("SELECT titre,auteurs,annee FROM publication WHERE titre LIKE ? ESCAPE '!' OR auteurs LIKE ? ESCAPE '!'",q,q));
  if(!"DOCTORANT".equals(u.getStatutActuel()))block(p,"Événements",cols("titre","Titre","type_event","Type","date_event","Date"),Sql.rows("SELECT titre,type_event,date_event FROM evenement WHERE titre LIKE ? ESCAPE '!'",q));
  if(Access.internal(u))documents(p,Sql.rows("SELECT d.*,CONCAT(u.prenom,' ',u.nom) auteur,e.titre evenement,r.titre reunion FROM document d LEFT JOIN utilisateur u ON u.id=d.id_deposeur LEFT JOIN evenement e ON e.id_event=d.evenement_id LEFT JOIN reunion r ON r.id_reunion=d.reunion_id WHERE d.titre LIKE ? ESCAPE '!'",q));
 }
 private void report(Page p,HttpServletRequest r)throws SQLException {com.lias.service.AnnualReport.populate(p,r.getParameter("annee"));}
 private static Integer optional(HttpServletRequest r,String key){String v=r.getParameter(key);return v==null||v.isBlank()?null:Integer.valueOf(v);}
 private static String title(String k){return switch(k){case "index"->"Bienvenue au LIAS";case "dashboard"->"Vue d’ensemble";case "profil"->"Profil du chercheur";case "membres"->"Membres du laboratoire";case "gouvernance"->"Gouvernance & mandats";case "demandes"->"Candidatures";case "documents"->"Archives & documents";case "materiel"->"Matériel & dotations";case "messagerie"->"Messagerie interne";case "calendrier"->"Calendrier scientifique";case "rapport"->"Rapport annuel";case "recherche"->"Recherche globale";case "historique"->"Historique & traçabilité";default->Module.ALL.containsKey(k)?Module.get(k).title():k.substring(0,1).toUpperCase()+k.substring(1);};}
 private static Map<String,String> cols(String... labels){Map<String,String> m=new LinkedHashMap<>();for(int i=0;i<labels.length;i+=2)m.put(labels[i],labels[i+1]);return m;}
 private static void block(Page p,String title,Map<String,String> columns,List<Map<String,Object>> rows){p.blocks.add(new Block(title,columns,rows.stream().map(row->new Row(row,List.of())).toList()));}
 private static Field f(String n,String l,String t,boolean req,String opts){return new Field(n,l,t,req,opts);}
 private static Field ref(String n,String l,String table,boolean req){return f(n,l,"reference",req,table);}
 private static Action link(String label,String endpoint,Map<String,String> params){return new Action(label,endpoint,params,false,"");}
 private static Action post(String label,String endpoint,Map<String,String> params,String confirm){return new Action(label,endpoint,params,true,confirm);}
 private static void form(Page p,String title,String endpoint,String action,Field... fields){p.forms.add(new Form(title,endpoint,List.of(fields),Map.of(),Map.of("action",action),false));}
}


