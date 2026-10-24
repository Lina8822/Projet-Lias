import com.lias.config.DBConnection;
import com.lias.dao.*;
import com.lias.security.Passwords;
import com.lias.service.Trace;
import java.sql.*;
import java.util.*;
import java.nio.file.*;

/** Explicit opt-in, local development fixtures only. Never resets an existing account. */
public class CreateTestAccounts {
 public static void main(String[] args) throws Exception {
  if(args.length!=1||!args[0].equals("--development-lias_data_base"))throw new IllegalArgumentException("Development opt-in required");
  Properties p=new Properties();String password=requiredPassword("LIAS_TEST_PASSWORD");
  String directorPassword=requiredPassword("LIAS_TEST_DIRECTOR_PASSWORD");
  try(Connection c=DBConnection.getConnection()) {
   if(!("lias_data_base".equals(c.getCatalog())||c.getCatalog().startsWith("lias_test"))||!c.getMetaData().getURL().matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1):(3306|33307)/.*"))throw new IllegalStateException("Not the approved local development database");
   c.setAutoCommit(false);
   try {
    var director=Sql.one(c,"SELECT email FROM utilisateur WHERE id=4");
    if(!"directeur.test@lias.example.test".equals(director.get("email")))throw new IllegalStateException("Unexpected director");
    p.setProperty("director",director.get("email").toString());p.setProperty("director.id","4");p.setProperty("director.password",directorPassword);
    var teams=Sql.rows(c,"SELECT id FROM equipe WHERE nom='RECETTE PROFILS'");
    int team=teams.isEmpty()?Sql.insert(c,"INSERT INTO equipe(nom,description,date_creation) VALUES ('RECETTE PROFILS','Ã‰quipe de dÃ©veloppement pour les tests HTTP',CURRENT_DATE)"):Sql.id(teams.get(0).get("id"));
    if(teams.isEmpty())Trace.audit(c,null,"RECETTE_CREATION","equipe",team,null,"Ã‰quipe de test");
    p.setProperty("team.id",String.valueOf(team));
    String[][] profiles={{"admin","PERMANENT","ADMIN","ACTIF"},{"vice","PERMANENT","VICE_DIRECTEUR","ACTIF"},{"chef","PERMANENT","CHEF_EQUIPE","ACTIF"},{"member","PERMANENT","MEMBRE_EQUIPE","ACTIF"},{"associe","ASSOCIE","MEMBRE_EQUIPE","ACTIF"},{"doctorant","DOCTORANT","MEMBRE_EQUIPE","ACTIF"},{"retraite","RETRAITE","MEMBRE_EQUIPE","GELE"},{"ancien","ANCIEN_MEMBRE","MEMBRE_EQUIPE","DESACTIVE"}};
    for(String[] row:profiles){String email="recette."+row[0]+"@lias.example.test";var old=Sql.rows(c,"SELECT id,mot_de_passe FROM utilisateur WHERE email=?",email);int id;
     if(old.isEmpty()){
      id=Sql.insert(c,"INSERT INTO utilisateur(nom,prenom,email,mot_de_passe,statut_actuel,role_actuel,compte_statut,etablissement,laboratoire,date_affiliation) VALUES ('RECETTE',?,?,?,?,?,?,'LIAS recette','LIAS',CURRENT_DATE)",row[0],email,Passwords.hash(password),row[1],row[2].equals("ADMIN")?"ADMIN":"MEMBRE_EQUIPE",row[3]);
      if(row[3].equals("ACTIF"))Sql.update(c,"INSERT INTO historique_affiliation(id_membre,ancien_statut,affiliation,date_debut) VALUES (?,?,'LIAS RECETTE',CURRENT_DATE)",id,row[1]);
      if(Set.of("vice","chef","member").contains(row[0])){
       Sql.update(c,"UPDATE utilisateur SET equipe_id=? WHERE id=?",team,id);
       Sql.update(c,"INSERT INTO membre_equipe(utilisateur_id,equipe_id,date_debut) VALUES (?,?,CURRENT_DATE)",id,team);
      }
      if(Set.of("vice","chef").contains(row[0])){
       if(row[0].equals("vice")&&!Sql.rows(c,"SELECT id_mandat FROM mandat WHERE UPPER(REPLACE(type_mandat,'-','_'))='VICE_DIRECTEUR' AND statut='ACTIF' AND (date_fin IS NULL OR date_fin>=CURRENT_DATE)").isEmpty())throw new IllegalStateException("Existing vice mandate: refusing to replace it");
       int mid=Sql.insert(c,"INSERT INTO mandat(id_membre,type_mandat,date_debut,statut,equipe_id) VALUES (?,?,CURRENT_DATE,'ACTIF',?)",id,row[2],row[0].equals("chef")?team:null);
       Trace.audit(c,null,"RECETTE_NOMINATION","mandat",mid,null,row[2]);
      }
      Trace.audit(c,null,"RECETTE_COMPTE","utilisateur",id,null,row[1]+" / "+row[2]+" / "+row[3]);
     }else {id=Sql.id(old.get(0).get("id"));if(!Passwords.verify(password,Sql.str(old.get(0).get("mot_de_passe"))))throw new IllegalStateException("Existing fixture password differs; no reset performed");}
     p.setProperty(row[0],email);p.setProperty(row[0]+".id",String.valueOf(id));p.setProperty(row[0]+".password",password);
    }
    c.commit();
   }catch(Exception e){c.rollback();throw e;}
  }
  Files.createDirectories(Path.of("build"));try(var out=Files.newBufferedWriter(Path.of("build/test-accounts.properties"))){p.store(out,"Local lias_data_base only. Real accounts; do not use isolated fixtures.");}
  System.out.println("Development accounts provisioned; director id=4 reused. Credentials: build/test-accounts.properties");
 }

 private static String requiredPassword(String name) {
  String value=System.getenv(name);
  if(value==null||value.length()<12)throw new IllegalArgumentException(name+" required (12+ characters)");
  return value;
 }
}
