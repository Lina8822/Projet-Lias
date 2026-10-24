import com.lias.config.DBConnection;
import com.lias.dao.*;
import com.lias.service.Trace;
import java.util.Objects;
/** Restore the initial unknown date after the ADMIN HTTP test; no row is deleted. */
public class RestoreUnsetLabDate {
 public static void main(String[] args)throws Exception {
  if(args.length!=2||!args[0].equals("--development-lias_data_base"))throw new IllegalArgumentException("Development only");
  try(var c=DBConnection.getConnection()){
   if(!"lias_data_base".equals(c.getCatalog())||!c.getMetaData().getURL().matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1):3306/.*"))throw new IllegalStateException("Wrong database");
   c.setAutoCommit(false);
   try {
    var old=Sql.one(c,"SELECT id,valeur FROM parametre WHERE cle='date_creation_laboratoire' FOR UPDATE");
    if(!Objects.equals(old.get("valeur"),args[1]))throw new IllegalStateException("Parameter changed since test; refusing to overwrite");
    var row=Sql.one(c,"SELECT id FROM utilisateur WHERE email='recette.admin@lias.example.test'");var admin=new AccountDAO().load(c,Sql.id(row.get("id")));
    com.lias.security.Access.require(com.lias.security.Access.admin(admin));
    Sql.update(c,"UPDATE parametre SET valeur=NULL WHERE id=?",old.get("id"));
    Trace.audit(c,admin,"RECETTE_RESTAURATION_PARAMETRE","parametre",Sql.id(old.get("id")),old.get("valeur"),"Date non renseignée (état initial)");c.commit();
   }catch(Exception e){c.rollback();throw e;}
  }
 }
}
