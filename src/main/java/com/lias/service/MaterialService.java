package com.lias.service;
import java.sql.*;import java.util.*;import com.lias.dao.*;import com.lias.model.Membre;import com.lias.security.Access;
public class MaterialService {
 public void execute(Connection c,Membre actor,Input in,String action)throws SQLException {
  Access.require(Access.internal(actor));
  if(action.equals("demander")){Integer mid=in.optionalId("materiel_id");String requested=mid==null?in.text("nom_materiel",255,true):Sql.str(Sql.one(c,"SELECT nom FROM materiel WHERE id=?",mid).get("nom"));int id=Sql.insert(c,"INSERT INTO demande_materiel(id_membre,nom_materiel,materiel_id,quantite,description) VALUES (?,?,?,?,?)",actor.getIdMembre(),requested,mid,in.id("quantite"),in.text("description",4000,true));Trace.audit(c,actor,"DEMANDE_MATERIEL","demande_materiel",id,null,requested);Trace.notify(c,null,"Nouvelle demande de matériel.","materiel.jsp");return;}
  Access.require(Access.direction(actor));
  switch(action){
   case "etat"->{int mid=in.id("materiel_id");var before=Sql.one(c,"SELECT etat FROM materiel WHERE id=? FOR UPDATE",mid);String state=in.choice("etat","DISPONIBLE","MAINTENANCE","HORS_SERVICE");Sql.update(c,"UPDATE materiel SET etat=? WHERE id=?",state,mid);Trace.audit(c,actor,"ETAT_MATERIEL","materiel",mid,before,state);}
   case "rattacher"->{int id=in.id("id"),mid=in.id("materiel_id");var before=Sql.one(c,"SELECT materiel_id,etat_demande FROM demande_materiel WHERE id_demande_mat=? FOR UPDATE",id);if(!Set.of("EN_ATTENTE","ACCEPTEE").contains(Sql.str(before.get("etat_demande"))))throw new IllegalArgumentException("Une demande clôturée ne peut plus être rattachée.");Sql.one(c,"SELECT id FROM materiel WHERE id=?",mid);Sql.update(c,"UPDATE demande_materiel SET materiel_id=? WHERE id_demande_mat=?",mid,id);Trace.audit(c,actor,"RATTACHEMENT_MATERIEL","demande_materiel",id,before,mid);}

   case "stock"->{int id=Sql.insert(c,"INSERT INTO materiel(nom,quantite,etat) VALUES (?,0,?)",in.text("nom",255,true),in.choice("etat","DISPONIBLE","MAINTENANCE","HORS_SERVICE"));Trace.audit(c,actor,"CREATION_STOCK","materiel",id,null,in.text("nom",255,true));}
   case "arrivage"->{int mid=in.id("materiel_id"),q=in.id("quantite");Sql.one(c,"SELECT id FROM materiel WHERE id=? FOR UPDATE",mid);Sql.update(c,"UPDATE materiel SET quantite=quantite+? WHERE id=?",q,mid);int id=Sql.insert(c,"INSERT INTO arrivage(materiel_id,quantite,acteur_id) VALUES (?,?,?)",mid,q,actor.getIdMembre());Trace.audit(c,actor,"ARRIVAGE","arrivage",id,null,q);}
   case "traiter"->{int id=in.id("id");String decision=in.choice("decision","ACCEPTEE","REFUSEE","LIVREE");var d=Sql.one(c,"SELECT * FROM demande_materiel WHERE id_demande_mat=? FOR UPDATE",id);
    String old=Sql.str(d.get("etat_demande"));if((decision.equals("LIVREE")&&!old.equals("ACCEPTEE"))||(!decision.equals("LIVREE")&&!old.equals("EN_ATTENTE")))throw new IllegalArgumentException("Transition impossible : demande déjà traitée.");
    if(decision.equals("LIVREE")){if(d.get("materiel_id")==null)throw new IllegalArgumentException("Cette ancienne demande doit être rattachée à un matériel avant livraison.");distribute(c,actor,Sql.id(d.get("materiel_id")),Sql.id(d.get("id_membre")),Sql.id(d.get("quantite")),id);}
    Sql.update(c,"UPDATE demande_materiel SET etat_demande=? WHERE id_demande_mat=?",decision,id);Trace.audit(c,actor,"DECISION_MATERIEL","demande_materiel",id,old,decision);Trace.notify(c,Sql.id(d.get("id_membre")),"Demande de matériel : "+decision,"materiel.jsp");}
   case "distribuer"->distribute(c,actor,in.id("materiel_id"),in.id("membre_id"),in.id("quantite"),null);
   default->throw new IllegalArgumentException("Action matériel inconnue.");
  }
 }
 private void distribute(Connection c,Membre actor,int mat,int user,int q,Integer request)throws SQLException {
  Access.require(Access.internal(new AccountDAO().load(c,user)));var before=Sql.one(c,"SELECT * FROM materiel WHERE id=? FOR UPDATE",mat);
  if(!"DISPONIBLE".equals(before.get("etat"))||Sql.id(before.get("quantite"))<q)throw new IllegalArgumentException("Quantité disponible insuffisante ou matériel indisponible.");
  Sql.update(c,"UPDATE materiel SET quantite=quantite-? WHERE id=?",q,mat);int id=Sql.insert(c,"INSERT INTO distribution_materiel(materiel_id,membre_id,quantite,acteur_id,demande_id) VALUES (?,?,?,?,?)",mat,user,q,actor.getIdMembre(),request);
  Trace.audit(c,actor,"DISTRIBUTION","distribution_materiel",id,before,"membre="+user+", quantité="+q);
 }
}
