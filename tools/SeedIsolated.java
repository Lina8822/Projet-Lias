import com.lias.config.DBConnection;
import com.lias.dao.Sql;
import com.lias.security.Passwords;
/** Seeds only a fresh, explicitly named test database. Never changes existing users. */
public class SeedIsolated {
 public static void main(String[] args)throws Exception {
  String password=requiredPassword("LIAS_TEST_PASSWORD");
  String directorPassword=requiredPassword("LIAS_TEST_DIRECTOR_PASSWORD");
  try(var c=DBConnection.getConnection()) {
   if(!c.getCatalog().startsWith("lias_test")||!Sql.rows(c,"SELECT id FROM utilisateur").isEmpty())throw new IllegalStateException("Fresh isolated database required");
   c.setAutoCommit(false);
   for(int id=1;id<=4;id++)Sql.update(c,"INSERT INTO utilisateur(id,nom,prenom,email,mot_de_passe,statut_actuel,role_actuel,compte_statut,date_affiliation,etablissement,laboratoire) VALUES (?,'RECETTE',?,?,?,?, 'MEMBRE_EQUIPE','ACTIF',CURRENT_DATE,'FacultÃ© test','LIAS')",id,"Test "+id,id==4?"directeur.test@lias.example.test":"seed"+id+"@example.test",Passwords.hash(id==4?directorPassword:password),"PERMANENT");
   Sql.update(c,"INSERT INTO mandat(id_membre,type_mandat,date_debut,statut) VALUES (4,'DIRECTEUR',CURRENT_DATE,'ACTIF')");
   Sql.update(c,"INSERT INTO historique_affiliation(id_membre,ancien_statut,affiliation,date_debut) SELECT id,statut_actuel,'LIAS',CURRENT_DATE FROM utilisateur");
   Sql.update(c,"INSERT INTO parametre(cle,valeur) VALUES ('date_creation_laboratoire','2000-01-01') ON DUPLICATE KEY UPDATE valeur=VALUES(valeur)");
   c.commit();
  }
 }

 private static String requiredPassword(String name) {
  String value=System.getenv(name);
  if(value==null||value.length()<12)throw new IllegalArgumentException(name+" required (12+ characters)");
  return value;
 }
}
