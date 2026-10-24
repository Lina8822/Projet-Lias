package com.lias.service;
import java.sql.*;import com.lias.dao.Sql;import com.lias.model.Membre;
public final class Trace {
 private Trace(){}
 public static void audit(Connection c,Membre u,String action,String entity,int id,Object before,Object after)throws SQLException{
  Sql.update(c,"INSERT INTO audit_log(acteur_id,action,entite,entite_id,avant,apres) VALUES (?,?,?,?,?,?)",u==null?null:u.getIdMembre(),action,entity,id,before==null?null:before.toString(),after==null?null:after.toString());
 }
 public static void notify(Connection c,Integer user,String message,String link)throws SQLException {Sql.update(c,"INSERT INTO notification(id_membre,message,type_notif,lien) VALUES (?,?,?,?)",user,message,"INFORMATION",link);}
 public static void broadcast(Connection c,String message,String link,boolean internal)throws SQLException{
  Sql.update(c,"INSERT INTO notification(id_membre,message,type_notif,lien) SELECT id,?,'INFORMATION',? FROM utilisateur WHERE compte_statut='ACTIF' AND statut_actuel IN ("+(internal?"'PERMANENT'":"'PERMANENT','ASSOCIE'")+")",message,link);
 }
}
