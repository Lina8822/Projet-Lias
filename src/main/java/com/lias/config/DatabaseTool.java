package com.lias.config;
import java.sql.*;import java.util.*;import com.lias.dao.*;import com.lias.security.Passwords;
/** Explicit operator-only maintenance commands; no HTTP endpoint. */
public final class DatabaseTool {
 public static int migratePasswords(Connection c)throws SQLException {
  int count=0;
  for(var u:Sql.rows(c,"SELECT id,mot_de_passe FROM utilisateur FOR UPDATE")) {
   String password=Sql.str(u.get("mot_de_passe"));
   if(Passwords.verifyLegacyPlaintext(password,password)) {
    int id=Sql.id(u.get("id"));Sql.update(c,"UPDATE utilisateur SET mot_de_passe=? WHERE id=?",Passwords.hash(password),id);
    com.lias.service.Trace.audit(c,null,"MIGRATION_PASSWORD","utilisateur",id,null,"Stockage PBKDF2");count++;
   }
  }
  return count;
 }
 public static void main(String[] args)throws Exception {
  if(args.length<1)throw new IllegalArgumentException("migrate-passwords | create-admin EMAIL NOM PRENOM | initial-director ID DATE_DEBUT");
  try(Connection c=DBConnection.getConnection()){c.setAutoCommit(false);try{
   switch(args[0]){
    case "check"->{System.out.println("Base : "+c.getCatalog());for(var row:Sql.rows(c,"SELECT statut_actuel,role_actuel,compte_statut,COUNT(*) effectif FROM utilisateur GROUP BY statut_actuel,role_actuel,compte_statut"))System.out.println(row);for(var row:Sql.rows(c,"SELECT table_name,column_name,column_type FROM information_schema.columns WHERE table_schema=DATABASE() AND column_name IN ('id','id_membre','id_doc','id_partenaire')"))System.out.println(row);for(var row:Sql.rows(c,"SELECT email,COUNT(*) n FROM utilisateur GROUP BY email HAVING COUNT(*)>1"))System.out.println("Email en double : "+row);}
    case "import-uploads"->{
     if(args.length!=2)throw new IllegalArgumentException("import-uploads DOSSIER_ORIGINAL");java.nio.file.Path original=java.nio.file.Path.of(args[1]).toRealPath();int count=0;
     for(var d:Sql.rows(c,"SELECT id_doc,nom_fichier FROM document FOR UPDATE")){
      String name=Sql.str(d.get("nom_fichier"));if(name.matches("[0-9a-f-]{36}\\.pdf"))continue;
      java.nio.file.Path source=original.resolve(name).normalize();if(!source.startsWith(original)||!java.nio.file.Files.isRegularFile(source)||!source.toRealPath().startsWith(original))throw new IllegalArgumentException("Chemin non importable pour document "+d.get("id_doc"));
      if(java.nio.file.Files.size(source)>10485760)throw new IllegalArgumentException("Document trop grand : "+d.get("id_doc"));byte[] bytes=java.nio.file.Files.readAllBytes(source);
      if(bytes.length<5||!new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))throw new IllegalArgumentException("PDF requis pour document "+d.get("id_doc"));
      String stored=java.util.UUID.randomUUID()+".pdf";java.nio.file.Files.write(com.lias.service.FilesService.root().resolve(stored),bytes,java.nio.file.StandardOpenOption.CREATE_NEW);
      Sql.update(c,"UPDATE document SET nom_fichier=? WHERE id_doc=?",stored,d.get("id_doc"));Sql.update(c,"INSERT INTO audit_log(action,entite,entite_id,avant,apres) VALUES ('IMPORT_ARCHIVE','document',?,?,?)",d.get("id_doc"),name,stored);count++;
     }System.out.println(count+" archives importées. Les fichiers sources sont conservés.");
    }
    case "migrate-passwords"->System.out.println(migratePasswords(c)+" mots de passe migrés sans affichage de secrets.");
    case "create-admin"->{if(args.length!=4)throw new IllegalArgumentException("create-admin EMAIL NOM PRENOM");if(!Sql.rows(c,"SELECT id FROM utilisateur WHERE email=?",args[1]).isEmpty())throw new IllegalArgumentException("Email déjà utilisé.");java.io.Console console=System.console();if(console==null)throw new IllegalStateException("Un terminal interactif est requis.");char[] pw=console.readPassword("Mot de passe (12 caractères minimum) : ");if(pw.length<12)throw new IllegalArgumentException("Mot de passe trop court.");String hash=Passwords.hash(new String(pw));Arrays.fill(pw,'\0');int uid=Sql.insert(c,"INSERT INTO utilisateur(nom,prenom,email,mot_de_passe,statut_actuel,role_actuel,compte_statut,date_affiliation) VALUES (?,?,?,?,'PERMANENT','ADMIN','ACTIF',CURRENT_DATE)",args[2],args[3],args[1],hash);Sql.update(c,"INSERT INTO historique_affiliation(id_membre,ancien_statut,affiliation,date_debut) VALUES (?,'PERMANENT','LIAS',CURRENT_DATE)",uid);com.lias.service.Trace.audit(c,null,"CREATION_ADMIN","utilisateur",uid,null,"Compte technique créé");}
    case "initial-director"->{if(args.length!=3)throw new IllegalArgumentException("initial-director ID DATE_DEBUT");int id=Integer.parseInt(args[1]);java.sql.Date start=java.sql.Date.valueOf(args[2]);if(start.toLocalDate().isAfter(java.time.LocalDate.now()))throw new IllegalArgumentException("Date future interdite.");if(!Sql.rows(c,"SELECT id_mandat FROM mandat WHERE type_mandat='DIRECTEUR' AND statut='ACTIF'").isEmpty())throw new IllegalArgumentException("Un mandat existe déjà. Utilisez la gouvernance.");Sql.one(c,"SELECT id FROM utilisateur WHERE id=? AND statut_actuel='PERMANENT' AND compte_statut='ACTIF' AND role_actuel<>'ADMIN' FOR UPDATE",id);int mid=Sql.insert(c,"INSERT INTO mandat(id_membre,type_mandat,date_debut,statut) VALUES (?,'DIRECTEUR',?,'ACTIF')",id,start);com.lias.service.Trace.audit(c,null,"INITIALISATION_OPERATEUR","mandat",mid,null,args[2]);}
    default->throw new IllegalArgumentException("Commande inconnue.");
   }c.commit();System.out.println("Opération terminée.");
  }catch(Exception e){c.rollback();throw e;}}
 }
}
