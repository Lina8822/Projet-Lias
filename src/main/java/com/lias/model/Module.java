package com.lias.model;
import java.util.*;
/** Explicit field allowlists shared by forms and server validation. */
public record Module(String key,String title,String table,String pk,String endpoint,List<Field> fields,String state) {
 public record Field(String name,String label,String type,boolean required,String options){ }
 private static Field f(String name,String label,String type,boolean required,String options){return new Field(name,label,type,required,options);}
 public static final Map<String,Module> ALL=new LinkedHashMap<>();
 private static void add(String key,String title,String table,String pk,String endpoint,String state,Field... fields){ALL.put(key,new Module(key,title,table,pk,endpoint,List.of(fields),state));}
 static {
  add("equipes","Équipes de recherche","equipe","id","EquipeServlet",null,f("nom","Nom de l’équipe","text",true,""),f("description","Description","textarea",true,""),f("date_creation","Date de création","date",true,""));
  add("evenements","Événements scientifiques","evenement","id_event","AjouterEvenementServlet","statut",f("titre","Titre","text",true,""),f("type_event","Type","select",true,"CONFERENCE,SEMINAIRE,WORKSHOP,AUTRE"),f("date_event","Début","datetime-local",true,""),f("date_fin","Fin","datetime-local",true,""),f("lieu","Lieu","text",true,""),f("edition","Édition","text",false,""),f("description","Description","textarea",true,""));
  add("publications","Publications","publication","id_pub","AjouterPublicationServlet",null,f("titre","Titre","text",true,""),f("auteurs","Auteurs","textarea",true,""),f("annee","Année","year",true,""),f("journal_conference","Revue / conférence","text",true,""),f("doi","DOI","text",false,""),f("lien_pdf","Lien de la publication","url",false,""),f("equipe_id","Équipe","reference",false,"equipes"),f("document_id","Document PDF (archives)","reference",false,"documents"));
  add("partenaires","Partenaires","partenaire","id_partenaire","ConventionServlet",null,f("nom_institution","Organisme","text",true,""),f("nom","Nom du partenaire","text",true,""),f("pays","Pays","text",true,""),f("contact","Contact","text",false,""),f("email","Email","email",false,""),f("telephone","Téléphone","text",false,""),f("description","Description","textarea",false,""));
  add("conventions","Conventions","convention","id","ConventionServlet","etat",f("titre","Titre","text",true,""),f("partenaire_id","Partenaire","reference",true,"partenaires"),f("date_debut","Début","date",true,""),f("date_fin","Fin","date",true,""),f("description","Description","textarea",true,""),f("document_id","Document","reference",false,"documents"));
  add("activites","Activités des conventions","convention_activite","id","ConventionServlet",null,f("convention_id","Convention","reference",true,"conventions"),f("titre","Activité","text",true,""),f("date_activite","Date","date",true,""),f("description","Description","textarea",false,""));
  add("reunions","Réunions & procès-verbaux","reunion","id_reunion","ReunionServlet","statut",f("titre","Titre","text",true,""),f("date_reunion","Date et heure","datetime-local",true,""),f("lieu","Lieu","text",true,""),f("ordre_du_jour","Ordre du jour","textarea",true,""),f("contenu","Compte rendu","textarea",false,""));
 }
 public static Module get(String key){Module m=ALL.get(key);if(m==null)throw new IllegalArgumentException("Module inconnu.");return m;}
 public String permission(){return Set.of("partenaires","activites").contains(key)?"conventions":key;}
}
