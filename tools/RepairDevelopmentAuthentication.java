import com.lias.config.DBConnection;
import com.lias.dao.*;
import com.lias.model.Membre;
import com.lias.security.*;
import java.sql.*;
import java.util.*;

/** Explicit maintenance command for the local development database, never an HTTP endpoint. */
public class RepairDevelopmentAuthentication {
    public static void main(String[] args) throws Exception {
        String email = "directeur.test@lias.example.test";
        String password = System.getenv("LIAS_TEST_PASSWORD");
        if (password == null || password.length() < 12) throw new IllegalArgumentException("LIAS_TEST_PASSWORD required (12+ characters)");
        try (Connection c = DBConnection.getConnection()) {
            if (!"lias_data_base".equals(c.getCatalog())) throw new IllegalStateException("Expected lias_data_base");
            // Validate the exact required schema before any write.
            Sql.rows(c, "SELECT id,email,mot_de_passe,statut_actuel,role_actuel,compte_statut FROM utilisateur LIMIT 0");
            Sql.rows(c, "SELECT id_mandat,id_membre,type_mandat,date_debut,date_fin,statut FROM mandat LIMIT 0");
            c.setAutoCommit(false);
            try {
                var found=Sql.rows(c,"SELECT id FROM utilisateur WHERE email=?",email);
                int id;
                if(found.isEmpty()) {
                    id=Sql.insert(c,"INSERT INTO utilisateur(nom,prenom,email,mot_de_passe,statut_actuel,role_actuel,compte_statut,date_affiliation) VALUES (?,?,?,?,'PERMANENT','DIRECTEUR','ACTIF',CURRENT_DATE)","Test","Directeur",email,Passwords.hash(password));
                    Sql.update(c,"INSERT INTO mandat(id_membre,type_mandat,date_debut,statut) VALUES (?,'DIRECTEUR',CURRENT_DATE,'ACTIF')",id);
                    Sql.update(c,"INSERT INTO audit_log(action,entite,entite_id,apres) VALUES ('COMPTE_TEST_DEVELOPPEMENT','utilisateur',?,'Compte test directeur demandé par utilisateur')",id);
                } else {
                    id=Sql.id(found.get(0).get("id"));
                    if(!Passwords.verify(password,Sql.str(Sql.one(c,"SELECT mot_de_passe FROM utilisateur WHERE id=?",id).get("mot_de_passe"))))
                        throw new IllegalStateException("Existing test account has a different password; not overwritten");
                }
                Membre u=new AccountDAO().load(c,id);
                if(!Access.director(u))throw new IllegalStateException("Test account must be active director");

                c.commit();
                System.out.println("database="+c.getCatalog()+"; testId="+id+"; testEmail="+email+"; role="+u.getRoleActuel()+"; status="+u.getStatutActuel()+"; account="+u.getCompteStatut());
            } catch(Exception e) {c.rollback();throw e;}
        }
    }
}

