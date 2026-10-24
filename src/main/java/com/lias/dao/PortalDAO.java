package com.lias.dao;
import java.sql.*;import java.util.*;import com.lias.model.Membre;import com.lias.model.Module;import com.lias.security.Access;
public class PortalDAO {
 public static final String CURRENT_ROLE="CASE WHEN u.role_actuel='ADMIN' THEN 'ADMIN' ELSE COALESCE((SELECT type_mandat FROM mandat md WHERE md.id_membre=u.id AND md.statut='ACTIF' AND md.date_debut<=CURRENT_DATE AND (md.date_fin IS NULL OR md.date_fin>=CURRENT_DATE) ORDER BY FIELD(type_mandat,'DIRECTEUR','VICE_DIRECTEUR','CHEF_EQUIPE','MEMBRE_EQUIPE') LIMIT 1),'MEMBRE_EQUIPE') END";
 public List<Map<String,Object>> members(Membre actor)throws SQLException {
  return Sql.rows("SELECT u.id,u.photo,u.nom,u.prenom,u.email,u.statut_actuel,"+CURRENT_ROLE+" role_actuel,e.nom equipe,u.etablissement,u.laboratoire,u.date_affiliation FROM utilisateur u LEFT JOIN equipe e ON e.id=u.equipe_id"+(actor==null?" WHERE u.profil_public=TRUE":"")+" ORDER BY u.nom,u.prenom");
 }
 public List<Map<String,Object>> content(Module m,Membre actor)throws SQLException {
  Access.require(Access.read(actor,m.permission()));String where=actor==null&&m.state()!=null?" WHERE "+m.state()+"<>'ARCHIVE'":"";
  return Sql.rows("SELECT * FROM "+m.table()+where+" ORDER BY "+m.pk()+" DESC");
 }
 public List<Map<String,Object>> documents()throws SQLException{return Sql.rows("SELECT d.*,CONCAT(u.prenom,' ',u.nom) auteur,e.titre evenement,r.titre reunion FROM document d LEFT JOIN utilisateur u ON d.id_deposeur=u.id LEFT JOIN evenement e ON e.id_event=d.evenement_id LEFT JOIN reunion r ON r.id_reunion=d.reunion_id ORDER BY d.date_depot DESC");}
 public List<Map<String,Object>> notifications(Membre actor)throws SQLException {
  return Sql.rows("SELECT n.*,CASE WHEN l.id IS NULL THEN 'NON LUE' ELSE 'LUE' END lecture FROM notification n LEFT JOIN notification_lecture l ON l.notification_id=n.id_notif AND l.membre_id=? WHERE n.id_membre=?"+(Access.director(actor)?" OR n.id_membre IS NULL":"")+" ORDER BY n.date_creation DESC",actor.getIdMembre(),actor.getIdMembre());
 }
 public List<Map<String,Object>> mandates()throws SQLException{return Sql.rows("SELECT m.*,CONCAT(u.prenom,' ',u.nom) membre,e.nom equipe,CASE WHEN m.statut='ACTIF' AND m.date_debut<=CURRENT_DATE AND (m.date_fin IS NULL OR m.date_fin>=CURRENT_DATE) THEN 'ACTIF' ELSE 'INACTIF' END etat_calcule FROM mandat m JOIN utilisateur u ON u.id=m.id_membre LEFT JOIN equipe e ON e.id=m.equipe_id ORDER BY m.date_debut DESC");}
 public List<Map<String,Object>> affiliations(Integer uid)throws SQLException{return Sql.rows("SELECT h.*,CONCAT(u.prenom,' ',u.nom) membre FROM historique_affiliation h JOIN utilisateur u ON u.id=h.id_membre"+(uid==null?"":" WHERE h.id_membre=?")+" ORDER BY h.date_debut DESC",uid==null?new Object[]{}:new Object[]{uid});}
 public List<Map<String,Object>> messages(Membre actor,Integer recipient,Integer team,Integer event)throws SQLException {
  String where;List<Object> args=new ArrayList<>();
  if(recipient!=null){where="((m.id_expediteur=? AND m.destinataire_id=?) OR (m.id_expediteur=? AND m.destinataire_id=?))";Collections.addAll(args,actor.getIdMembre(),recipient,recipient,actor.getIdMembre());}
  else if(team!=null){Access.require(!Sql.rows("SELECT id FROM utilisateur WHERE id=? AND equipe_id=?",actor.getIdMembre(),team).isEmpty());where="m.equipe_id=?";args.add(team);}
  else if(event!=null){where="m.evenement_id=?";args.add(event);}
  else where="m.destinataire_id IS NULL AND m.equipe_id IS NULL AND m.evenement_id IS NULL";
  args.add(0,actor.getIdMembre());
  return Sql.rows("SELECT m.*,CONCAT(u.prenom,' ',u.nom) expediteur,CASE WHEN l.id IS NULL AND m.id_expediteur<>? THEN 'NON LU' ELSE 'LU' END lecture FROM message m JOIN utilisateur u ON u.id=m.id_expediteur LEFT JOIN lecture_message l ON l.message_id=m.id_message AND l.membre_id="+actor.getIdMembre()+" WHERE "+where+" ORDER BY m.date_envoi ASC",args.toArray());
 }
 public List<Map<String,Object>> calendar(Membre actor)throws SQLException {
  var events=Sql.rows("SELECT id_event id,titre,date_event debut,date_fin fin,type_event type,lieu,'evenements.jsp' lien FROM evenement WHERE statut<>'ARCHIVE'");
  if(Access.internal(actor))events.addAll(Sql.rows("SELECT id_reunion id,titre,date_reunion debut,date_reunion fin,'REUNION' type,lieu,'reunions.jsp' lien FROM reunion WHERE statut<>'ARCHIVE'"));return events;
 }
 public Map<String,Long> stats(Membre actor)throws SQLException {
  Map<String,Long> result=new LinkedHashMap<>();result.put("Publications",count("SELECT COUNT(*) n FROM publication"));
  if(!"DOCTORANT".equals(actor.getStatutActuel())){result.put("Membres actifs",count("SELECT COUNT(*) n FROM utilisateur WHERE compte_statut='ACTIF' AND statut_actuel IN ('PERMANENT','ASSOCIE','DOCTORANT')"));result.put("Événements",count("SELECT COUNT(*) n FROM evenement WHERE statut<>'ARCHIVE'"));result.put("Équipes",count("SELECT COUNT(*) n FROM equipe"));}
  if(Access.internal(actor)){result.put("Documents",count("SELECT COUNT(*) n FROM document"));result.put("Matériel disponible",count("SELECT COALESCE(SUM(quantite),0) n FROM materiel WHERE etat='DISPONIBLE'"));result.put("Conventions actives",count("SELECT COUNT(*) n FROM convention WHERE etat='ACTIVE'"));}
  if(Access.director(actor))result.put("Candidatures en attente",count("SELECT COUNT(*) n FROM demandes_adhesion WHERE etat_demande='EN_ATTENTE'"));return result;
 }
 private long count(String sql)throws SQLException{return ((Number)Sql.rows(sql).get(0).get("n")).longValue();}
}
