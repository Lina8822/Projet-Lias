package com.lias.service;
import java.sql.*;import java.util.*;import com.lias.dao.*;import com.lias.model.Membre;import com.lias.security.Access;
public class CommunicationService {
 public void message(Connection c,Membre actor,Input in)throws SQLException {
  Access.require(Access.internal(actor));Integer to=in.optionalId("destinataire_id"),team=in.optionalId("equipe_id"),event=in.optionalId("evenement_id");
  if((to==null?0:1)+(team==null?0:1)+(event==null?0:1)>1)throw new IllegalArgumentException("Choisissez un seul destinataire, équipe ou événement.");
  if(to!=null)Access.require(Access.internal(new AccountDAO().load(c,to)));
  if(team!=null)Access.require(!Sql.rows(c,"SELECT id FROM utilisateur WHERE id=? AND equipe_id=?",actor.getIdMembre(),team).isEmpty());
  if(event!=null)Sql.one(c,"SELECT id_event FROM evenement WHERE id_event=?",event);
  int id=Sql.insert(c,"INSERT INTO message(id_expediteur,contenu,destinataire_id,equipe_id,evenement_id) VALUES (?,?,?,?,?)",actor.getIdMembre(),in.text("contenu",4000,true),to,team,event);
  if(to!=null)Trace.notify(c,to,"Nouveau message de "+actor.getPrenom(),"messagerie.jsp?destinataire_id="+actor.getIdMembre());
  else if(team!=null)Sql.update(c,"INSERT INTO notification(id_membre,message,type_notif,lien) SELECT id,?,'MESSAGE',? FROM utilisateur WHERE equipe_id=? AND id<>? AND statut_actuel='PERMANENT' AND compte_statut='ACTIF'","Nouveau message d’équipe","messagerie.jsp?equipe_id="+team,team,actor.getIdMembre());
  else Trace.broadcast(c,"Nouveau message au laboratoire",event==null?"messagerie.jsp":"messagerie.jsp?evenement_id="+event,true);
  Trace.audit(c,actor,"MESSAGE_ENVOYE","message",id,null,"Message enregistré");
 }
 public void readMessage(Connection c,Membre actor,int id)throws SQLException {
  Access.require(Access.internal(actor));var m=Sql.one(c,"SELECT * FROM message WHERE id_message=?",id);
  Integer to=m.get("destinataire_id")==null?null:Sql.id(m.get("destinataire_id"));Integer team=m.get("equipe_id")==null?null:Sql.id(m.get("equipe_id"));
  if(to!=null)Access.require(to==actor.getIdMembre()||Sql.id(m.get("id_expediteur"))==actor.getIdMembre());
  if(team!=null)Access.require(!Sql.rows(c,"SELECT id FROM utilisateur WHERE id=? AND equipe_id=?",actor.getIdMembre(),team).isEmpty());
  Sql.update(c,"INSERT INTO lecture_message(message_id,membre_id) VALUES (?,?) ON DUPLICATE KEY UPDATE membre_id=VALUES(membre_id)",id,actor.getIdMembre());
 }
 public void document(Connection c,Membre actor,Input in,String file)throws SQLException {
  Access.require(Access.internal(actor));if(file==null)throw new IllegalArgumentException("Joignez un fichier PDF.");
  Integer event=in.optionalId("evenement_id"),reunion=in.optionalId("reunion_id"),previous=in.optionalId("precedent_id");
  if(event!=null)Sql.one(c,"SELECT id_event FROM evenement WHERE id_event=?",event);if(reunion!=null){Access.require(Access.direction(actor));Sql.one(c,"SELECT id_reunion FROM reunion WHERE id_reunion=? FOR UPDATE",reunion);}
  int version=1;if(previous!=null){var old=Sql.one(c,"SELECT * FROM document WHERE id_doc=? FOR UPDATE",previous);Access.require(Access.direction(actor)||Sql.id(old.get("id_deposeur"))==actor.getIdMembre());if(!Sql.rows(c,"SELECT id_doc FROM document WHERE precedent_id=?",previous).isEmpty())throw new IllegalArgumentException("Sélectionnez la dernière version du document.");version=Sql.id(old.get("version"))+1;}
  String type=reunion==null?in.choice("type_doc","FINANCEMENT","PROGRAMME","ATTESTATION","RAPPORT","ADMINISTRATIF","PV","AUTRE"):"PV";
  int id=Sql.insert(c,"INSERT INTO document(titre,type_doc,nom_fichier,id_deposeur,evenement_id,reunion_id,version,precedent_id,date_depot) VALUES (?,?,?,?,?,?,?,?,CURRENT_DATE)",in.text("titre",255,true),type,file,actor.getIdMembre(),event,reunion,version,previous);
  if(reunion!=null)Sql.update(c,"UPDATE reunion SET nom_fichier_pv=? WHERE id_reunion=?",file,reunion);
  Trace.audit(c,actor,"DEPOT_DOCUMENT","document",id,null,"version="+version);Trace.broadcast(c,"Document ajouté : "+in.text("titre",255,true),"documents.jsp",true);
 }
 public void participant(Connection c,Membre actor,Input in,boolean reunion)throws SQLException {
  Access.require(Access.direction(actor));int entity=in.id("id"),user=in.id("membre_id");Access.require(Access.active(new AccountDAO().load(c,user)));
  var before=Sql.rows(c,reunion?"SELECT * FROM reunion_participant WHERE reunion_id=? AND membre_id=?":"SELECT * FROM evenement_membre WHERE evenement_id=? AND membre_id=?",entity,user);
  if(reunion){Access.require(Access.internal(new AccountDAO().load(c,user)));Sql.one(c,"SELECT id_reunion FROM reunion WHERE id_reunion=?",entity);Sql.update(c,"INSERT INTO reunion_participant(reunion_id,membre_id) VALUES (?,?) ON DUPLICATE KEY UPDATE membre_id=VALUES(membre_id)",entity,user);}
  else {Sql.one(c,"SELECT id_event FROM evenement WHERE id_event=?",entity);Sql.update(c,"INSERT INTO evenement_membre(evenement_id,membre_id,responsabilite) VALUES (?,?,?) ON DUPLICATE KEY UPDATE responsabilite=VALUES(responsabilite)",entity,user,in.choice("responsabilite","PARTICIPANT","ORGANISATEUR"));}
  var after=Sql.rows(c,reunion?"SELECT * FROM reunion_participant WHERE reunion_id=? AND membre_id=?":"SELECT * FROM evenement_membre WHERE evenement_id=? AND membre_id=?",entity,user);
  Trace.audit(c,actor,"PARTICIPANT_AFFECTE",reunion?"reunion":"evenement",entity,before,after);Trace.notify(c,user,"Vous êtes invité à une activité du laboratoire.",reunion?"reunions.jsp":"evenements.jsp");
 }
 public void readNotification(Connection c,Membre actor,int id)throws SQLException {
  var n=Sql.one(c,"SELECT id_membre FROM notification WHERE id_notif=?",id);Object recipient=n.get("id_membre");Access.require(recipient==null?Access.director(actor):Sql.id(recipient)==actor.getIdMembre());
  Sql.update(c,"INSERT INTO notification_lecture(notification_id,membre_id) VALUES (?,?) ON DUPLICATE KEY UPDATE membre_id=VALUES(membre_id)",id,actor.getIdMembre());
 }
}
