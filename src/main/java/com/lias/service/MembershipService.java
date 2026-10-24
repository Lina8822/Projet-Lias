package com.lias.service;
import java.sql.*;import java.util.*;import com.lias.dao.*;import com.lias.model.Membre;import com.lias.security.*;
public class MembershipService {
 public void apply(Connection c,Input in,String cv)throws SQLException{
  String email=in.email("email");if(cv==null)throw new IllegalArgumentException("Veuillez joindre un CV PDF.");
  if(!Sql.rows(c,"SELECT id_demande FROM demandes_adhesion WHERE email=? AND etat_demande='EN_ATTENTE'",email).isEmpty())throw new IllegalArgumentException("Une candidature est déjà en attente pour cet email.");
  if(!Sql.rows(c,"SELECT id FROM utilisateur WHERE email=? AND compte_statut='ACTIF'",email).isEmpty())throw new IllegalArgumentException("Un compte actif existe déjà pour cet email.");
  int id=Sql.insert(c,"INSERT INTO demandes_adhesion(nom,prenom,email,motivation,etablissement,laboratoire,statut_demande,cv,password_hash) VALUES (?,?,?,?,?,?,?,?,?)",in.text("nom",120,true),in.text("prenom",120,true),email,in.text("motivation",10000,true),in.text("etablissement",255,true),in.text("laboratoire",255,true),in.choice("statut_demande","PERMANENT","ASSOCIE","DOCTORANT"),cv,Passwords.hash(in.password()));
  Trace.notify(c,null,"Nouvelle candidature reçue.","demandes.jsp");Trace.audit(c,null,"CANDIDATURE","demandes_adhesion",id,null,email);
 }
 public void decide(Connection c,Membre actor,Input in)throws SQLException{
  Access.require(Access.director(actor));int id=in.id("id");String state=in.choice("decision","ACCEPTEE","REFUSEE");
  Map<String,Object> d=Sql.one(c,"SELECT * FROM demandes_adhesion WHERE id_demande=? FOR UPDATE",id);
  if(!"EN_ATTENTE".equals(d.get("etat_demande")))throw new IllegalArgumentException("Cette demande a déjà été traitée.");
  if(state.equals("ACCEPTEE")){
   if(Sql.str(d.get("password_hash")).isBlank())d.put("password_hash",Passwords.hash(in.password()));
   List<Map<String,Object>> users=Sql.rows(c,"SELECT * FROM utilisateur WHERE email=? FOR UPDATE",d.get("email"));int uid;
   if(users.isEmpty())uid=Sql.insert(c,"INSERT INTO utilisateur(nom,prenom,email,mot_de_passe,statut_actuel,role_actuel,compte_statut,etablissement,laboratoire,date_affiliation) VALUES (?,?,?,?,?,'MEMBRE_EQUIPE','ACTIF',?,?,CURRENT_DATE)",d.get("nom"),d.get("prenom"),d.get("email"),d.get("password_hash"),d.get("statut_demande"),d.get("etablissement"),d.get("laboratoire"));
   else {
    if(users.size()!=1)throw new IllegalArgumentException("Email présent sur plusieurs comptes. Corrigez la base.");Map<String,Object> u=users.get(0);if("ACTIF".equals(u.get("compte_statut")))throw new IllegalArgumentException("Le compte est déjà actif.");uid=Sql.id(u.get("id"));
    Sql.update(c,"UPDATE utilisateur SET mot_de_passe=?,statut_actuel=?,compte_statut='ACTIF',date_affiliation=CURRENT_DATE WHERE id=?",d.get("password_hash"),d.get("statut_demande"),uid);
    Sql.update(c,"UPDATE historique_affiliation SET date_fin=CURRENT_DATE WHERE id_membre=? AND date_fin IS NULL",uid);
   }
   Sql.update(c,"INSERT INTO historique_affiliation(id_membre,ancien_statut,affiliation,date_debut) VALUES (?,?,'LIAS',CURRENT_DATE)",uid,d.get("statut_demande"));
   Trace.notify(c,uid,"Votre candidature a été acceptée. Bienvenue au LIAS.","profil.jsp");
  }
  Sql.update(c,"UPDATE demandes_adhesion SET etat_demande=?,decision_date=CURRENT_TIMESTAMP,decision_par=? WHERE id_demande=?",state,actor.getIdMembre(),id);
  Sql.update(c,"INSERT INTO notification_candidat(demande_id,email,message) VALUES (?,?,?)",id,d.get("email"),"Votre candidature est "+state+".");
  Trace.audit(c,actor,"DECISION_ADHESION","demandes_adhesion",id,"EN_ATTENTE",state);
 }
 public void profile(Connection c,Membre actor,Input in,String photo)throws SQLException {
  requireOwnAccount(actor,in);int id=actor.getIdMembre();Map<String,Object> before=Sql.one(c,"SELECT nom,prenom,email,etablissement,laboratoire,biographie,centre_interet,photo,profil_public FROM utilisateur WHERE id=? FOR UPDATE",id);
  String email=in.email("email");if(!Sql.rows(c,"SELECT id FROM utilisateur WHERE email=? AND id<>?",email,id).isEmpty())throw new IllegalArgumentException("Adresse email déjà utilisée.");
  Map<String,Object> data=new LinkedHashMap<>();for(String f:List.of("nom","prenom","etablissement","laboratoire"))data.put(f,in.text(f,120,true));data.put("email",email);data.put("biographie",in.text("biographie",10000,false));data.put("centre_interet",in.text("centre_interet",4000,false));data.put("profil_public",in.choice("profil_public","1","0"));if(photo!=null)data.put("photo",photo);
  ContentDAO.update(c,"utilisateur","id",id,data);Trace.audit(c,actor,"PROFIL_MODIFIE","utilisateur",id,before,data);
 }
 public void lifecycle(Connection c,Membre actor,Input in)throws SQLException {
  Access.require(Access.director(actor)||Access.admin(actor));int id=in.id("id");if(id==actor.getIdMembre())throw new IllegalArgumentException("Votre propre compte ne peut pas être modifié ici.");
  Map<String,Object> before=Sql.one(c,"SELECT statut_actuel,compte_statut,date_affiliation FROM utilisateur WHERE id=? FOR UPDATE",id);
  String status=in.choice("statut_actuel","PERMANENT","ASSOCIE","DOCTORANT","RETRAITE","ANCIEN_MEMBRE");boolean active=Set.of("PERMANENT","ASSOCIE","DOCTORANT").contains(status);
  if(status.equals(before.get("statut_actuel")))throw new IllegalArgumentException("Ce statut est déjà appliqué.");
  Sql.update(c,"UPDATE historique_affiliation SET date_fin=CURRENT_DATE WHERE id_membre=? AND date_fin IS NULL",id);
  if(active)Sql.update(c,"INSERT INTO historique_affiliation(id_membre,ancien_statut,affiliation,date_debut) VALUES (?,?,'LIAS',CURRENT_DATE)",id,status);
  if(!status.equals("PERMANENT")){
   Sql.update(c,"UPDATE mandat SET date_fin=GREATEST(date_debut,CURRENT_DATE),statut='TERMINE' WHERE id_membre=? AND statut='ACTIF'",id);
   Sql.update(c,"UPDATE membre_equipe SET date_fin=CURRENT_DATE WHERE utilisateur_id=? AND date_fin IS NULL",id);
   Sql.update(c,"UPDATE utilisateur SET role_actuel='MEMBRE_EQUIPE',equipe_id=NULL WHERE id=?",id);
  }
  Sql.update(c,"UPDATE utilisateur SET statut_actuel=?,compte_statut=?,date_affiliation=CASE WHEN ? THEN CURRENT_DATE ELSE date_affiliation END WHERE id=?",status,active?"ACTIF":status.equals("RETRAITE")?"GELE":"DESACTIVE",active,id);
  Trace.audit(c,actor,"CHANGEMENT_STATUT","utilisateur",id,before,status);
 }
 public void assignTeam(Connection c,Membre actor,Input in)throws SQLException {
  Access.require(Access.assignTeam(actor));int uid=in.id("membre_id"),team=in.id("equipe_id");Sql.one(c,"SELECT id FROM equipe WHERE id=?",team);Map<String,Object> before=Sql.one(c,"SELECT equipe_id FROM utilisateur WHERE id=? FOR UPDATE",uid);
  if(!Access.teamTarget(new AccountDAO().load(c,uid)))throw new IllegalArgumentException("Ce membre ne peut pas être affecté : son compte est gelé, désactivé ou son statut est invalide.");
  Sql.update(c,"UPDATE membre_equipe SET date_fin=CURRENT_DATE WHERE utilisateur_id=? AND date_fin IS NULL",uid);
  Sql.update(c,"INSERT INTO membre_equipe(utilisateur_id,equipe_id,date_debut) VALUES (?,?,CURRENT_DATE)",uid,team);Sql.update(c,"UPDATE utilisateur SET equipe_id=? WHERE id=?",team,uid);
  Trace.audit(c,actor,"AFFECTATION_EQUIPE","utilisateur",uid,before,team);
 }
 public void closeMandate(Connection c,Membre actor,Input in)throws SQLException {
  Access.require(Access.director(actor)||Access.admin(actor));int id=in.id("id");var before=Sql.one(c,"SELECT * FROM mandat WHERE id_mandat=? FOR UPDATE",id);
  if(!"ACTIF".equals(before.get("statut")))throw new IllegalArgumentException("Ce mandat est déjà clôturé.");
  Sql.update(c,"UPDATE mandat SET statut='TERMINE',date_fin=GREATEST(date_debut,CURRENT_DATE) WHERE id_mandat=?",id);Trace.audit(c,actor,"CLOTURE_MANDAT","mandat",id,before,"TERMINE");
 }
 public void password(Connection c,Membre actor,Input in)throws SQLException {
  requireOwnAccount(actor,in);var old=Sql.one(c,"SELECT mot_de_passe FROM utilisateur WHERE id=? FOR UPDATE",actor.getIdMembre());
  if(!Passwords.verify(in.text("ancien_password",128,true),Sql.str(old.get("mot_de_passe"))))throw new IllegalArgumentException("Le mot de passe actuel est incorrect.");
  Sql.update(c,"UPDATE utilisateur SET mot_de_passe=? WHERE id=?",Passwords.hash(in.password()),actor.getIdMembre());Trace.audit(c,actor,"MOT_DE_PASSE_MODIFIE","utilisateur",actor.getIdMembre(),null,"Secret renouvelé");
 }
 private static void requireOwnAccount(Membre actor,Input in){
  Access.require(Access.active(actor));Integer requested=in.optionalId("id");
  Access.require(requested==null||requested==actor.getIdMembre());
 }
 public void mandate(Connection c,Membre actor,Input in)throws SQLException {
  Access.require(Access.director(actor)||Access.admin(actor));int uid=in.id("membre_id");String role=in.choice("type_mandat","DIRECTEUR","VICE_DIRECTEUR","CHEF_EQUIPE","MEMBRE_EQUIPE");String start=in.date("date_debut",true),end=in.date("date_fin",false);in.dateOrder(start,end);
  if(start.compareTo(java.time.LocalDate.now().toString())<0)throw new IllegalArgumentException("Une nouvelle nomination doit prendre effet aujourd’hui ou ultérieurement.");
  Integer team=in.optionalId("equipe_id");if(Set.of("CHEF_EQUIPE","MEMBRE_EQUIPE").contains(role)&&team==null)throw new IllegalArgumentException("Une équipe est requise pour ce rôle.");
  // Lock a stable row for singleton governance, avoiding concurrent first nominations.
  Sql.one(c,"SELECT id FROM utilisateur ORDER BY id LIMIT 1 FOR UPDATE");
  Membre target=new AccountDAO().load(c,uid);Access.require(Access.internal(target));if(Access.admin(target))throw new IllegalArgumentException("Le compte technique ADMIN doit rester distinct de la gouvernance.");
  if(team!=null)Sql.one(c,"SELECT id FROM equipe WHERE id=? FOR UPDATE",team);
  String where="type_mandat=? AND statut='ACTIF'";List<Object> args=new ArrayList<>();args.add(role);
  if(role.equals("CHEF_EQUIPE")){where+=" AND equipe_id=?";args.add(team);}else if(role.equals("MEMBRE_EQUIPE")){where+=" AND id_membre=? AND equipe_id=?";args.add(uid);args.add(team);}
  var before=Sql.rows(c,"SELECT * FROM mandat WHERE "+where+" FOR UPDATE",args.toArray());
  for(var old:before){if(Sql.str(old.get("date_debut")).compareTo(start)>0)throw new IllegalArgumentException("Une nomination ultérieure existe déjà. Clôturez-la d’abord.");}
  List<Object> closeArgs=new ArrayList<>();closeArgs.add(start);closeArgs.add(start);closeArgs.addAll(args);
  Sql.update(c,"UPDATE mandat SET statut=CASE WHEN ?<=CURRENT_DATE THEN 'TERMINE' ELSE statut END,date_fin=GREATEST(date_debut,DATE_SUB(?,INTERVAL 1 DAY)) WHERE "+where,closeArgs.toArray());
  int id=Sql.insert(c,"INSERT INTO mandat(id_membre,type_mandat,date_debut,date_fin,statut,equipe_id) VALUES (?,?,?,?,'ACTIF',?)",uid,role,start,end,team);
  Trace.audit(c,actor,"NOMINATION","mandat",id,before,role+" / membre "+uid);Trace.notify(c,uid,"Une responsabilité vous a été attribuée : "+role,"profil.jsp");
 }
}
