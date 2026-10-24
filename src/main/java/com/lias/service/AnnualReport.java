package com.lias.service;
import java.sql.*;import java.util.*;import com.lias.dao.Sql;import com.lias.view.Page;import com.lias.view.Page.*;
/** Shared data source for HTML and PDF annual reports. */
public final class AnnualReport {
 private AnnualReport() {}
 public static void populate(Page p,String requestedYear)throws SQLException {
  int year=requestedYear==null?java.time.Year.now().getValue():Integer.parseInt(requestedYear);if(year<1900||year>java.time.Year.now().getValue()+1)throw new IllegalArgumentException("Année invalide.");p.title="Rapport annuel · "+year;p.subtitle="Données institutionnelles de l’année "+year+" · Généré le "+java.time.LocalDate.now();
  var pubs=Sql.rows("SELECT titre,auteurs,annee,journal_conference,doi FROM publication WHERE annee=?",year);p.stats.put("Publications",(long)pubs.size());block(p,"Production scientifique",cols("titre","Titre","auteurs","Auteurs","journal_conference","Revue / conférence","doi","DOI"),pubs);
  var events=Sql.rows("SELECT titre,type_event,date_event,date_fin,lieu FROM evenement WHERE YEAR(date_event)=?",year);p.stats.put("Événements",(long)events.size());block(p,"Manifestations scientifiques",cols("titre","Titre","type_event","Type","date_event","Début","date_fin","Fin","lieu","Lieu"),events);
  var members=Sql.rows("SELECT DISTINCT u.id,u.nom,u.prenom,h.ancien_statut FROM historique_affiliation h JOIN utilisateur u ON u.id=h.id_membre WHERE h.date_debut<=? AND (h.date_fin IS NULL OR h.date_fin>=?)",year+"-12-31",year+"-01-01");p.stats.put("Membres affiliés",members.stream().map(m->m.get("id")).distinct().count());block(p,"Affiliations durant l’année (dates renseignées)",cols("nom","Nom","prenom","Prénom","ancien_statut","Statut"),members);
  block(p,"Équipes créées avant la fin de l’année",cols("nom","Équipe","description","Description","date_creation","Création"),Sql.rows("SELECT nom,description,date_creation FROM equipe WHERE date_creation<=?",year+"-12-31"));
  block(p,"Collaborations en cours durant l’année",cols("titre","Convention","date_debut","Début","date_fin","Fin"),Sql.rows("SELECT titre,date_debut,date_fin FROM convention WHERE date_debut<=? AND (date_fin IS NULL OR date_fin>=?)",year+"-12-31",year+"-01-01"));
  block(p,"Activités des conventions",cols("titre","Activité","date_activite","Date","description","Description"),Sql.rows("SELECT titre,date_activite,description FROM convention_activite WHERE YEAR(date_activite)=?",year));
  block(p,"Réunions",cols("titre","Titre","date_reunion","Date","ordre_du_jour","Ordre du jour"),Sql.rows("SELECT titre,date_reunion,ordre_du_jour FROM reunion WHERE YEAR(date_reunion)=?",year));
  block(p,"Dotations",cols("nom","Matériel","membre","Bénéficiaire","quantite","Quantité","date_distribution","Date"),Sql.rows("SELECT m.nom,CONCAT(u.prenom,' ',u.nom) membre,d.quantite,d.date_distribution FROM distribution_materiel d JOIN materiel m ON m.id=d.materiel_id JOIN utilisateur u ON u.id=d.membre_id WHERE YEAR(d.date_distribution)=?",year));
 }
 private static Map<String,String> cols(String... labels){Map<String,String> m=new LinkedHashMap<>();for(int i=0;i<labels.length;i+=2)m.put(labels[i],labels[i+1]);return m;}
 private static void block(Page p,String title,Map<String,String> columns,List<Map<String,Object>> rows){p.blocks.add(new Block(title,columns,rows.stream().map(row->new Row(row,List.of())).toList()));}
}
