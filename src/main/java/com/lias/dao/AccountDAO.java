package com.lias.dao;
import java.sql.*;
import java.util.*;
import com.lias.config.DBConnection;
import com.lias.model.Membre;
import com.lias.security.Passwords;
public class AccountDAO {
    public Membre authenticate(String email,String password)throws SQLException {
        if(email==null||password==null||email.length()>254||password.length()>1024)return null;
        try(Connection c=DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Map<String,Object>> rows=Sql.rows(c,"SELECT * FROM utilisateur WHERE email=? FOR UPDATE",email.trim());
                if(rows.size()!=1)return null;
                Map<String,Object> row=rows.get(0);
                String stored=Sql.str(row.get("mot_de_passe"));
                boolean modern=Passwords.verify(password,stored);
                boolean legacy=!modern && Boolean.parseBoolean(DBConnection.setting("LIAS_LEGACY_PASSWORDS","true")) && Passwords.verifyLegacyPlaintext(password,stored);
                if(!modern&&!legacy)return null;
                Membre user=load(c,Sql.id(row.get("id")));
                if(!com.lias.security.Access.active(user))return null;
                if(legacy)Sql.update(c,"UPDATE utilisateur SET mot_de_passe=? WHERE id=?",Passwords.hash(password),user.getIdMembre());
                c.commit();return user;
            } catch(SQLException|RuntimeException e) {c.rollback();throw e;}
        }
    }
    private static String canonicalRole(String role) {
        return switch(role.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "DIRECTEUR" -> "DIRECTEUR";
            case "VICE-DIRECTEUR", "VICE DIRECTEUR", "VICE_DIRECTEUR" -> "VICE_DIRECTEUR";
            case "CHEF D'ÉQUIPE", "CHEF D'EQUIPE", "CHEF_EQUIPE" -> "CHEF_EQUIPE";
            case "MEMBRE", "MEMBRE_EQUIPE" -> "MEMBRE_EQUIPE";
            default -> role;
        };
    }
    public Membre load(int id)throws SQLException{try(Connection c=DBConnection.getConnection()){return load(c,id);}}
    public Membre load(Connection c,int id)throws SQLException {
        List<Map<String,Object>> rows=Sql.rows(c,"SELECT * FROM utilisateur WHERE id=?",id);if(rows.isEmpty())return null;
        Map<String,Object> r=rows.get(0);Membre u=new Membre();u.setIdMembre(id);u.setNom(Sql.str(r.get("nom")));u.setPrenom(Sql.str(r.get("prenom")));u.setEmail(Sql.str(r.get("email")));u.setPhoto(Sql.str(r.get("photo")));
        u.setStatutActuel(Sql.str(r.get("statut_actuel")));u.setCompteStatut(Sql.str(r.get("compte_statut")));u.setPublicationAutorisee(Boolean.TRUE.equals(r.get("publication_autorisee"))||"1".equals(Sql.str(r.get("publication_autorisee"))));
        String role="ADMIN".equals(r.get("role_actuel"))?"ADMIN":"MEMBRE_EQUIPE";
        List<Map<String,Object>> mandates=Sql.rows(c,"SELECT type_mandat FROM mandat WHERE id_membre=? AND statut='ACTIF' AND date_debut<=CURRENT_DATE AND (date_fin IS NULL OR date_fin>=CURRENT_DATE) ORDER BY FIELD(type_mandat,'DIRECTEUR','VICE_DIRECTEUR','CHEF_EQUIPE','MEMBRE_EQUIPE')",id);
        if(!mandates.isEmpty()&&!"ADMIN".equals(role))role=canonicalRole(Sql.str(mandates.get(0).get("type_mandat")));u.setRoleActuel(role);return u;
    }
}

