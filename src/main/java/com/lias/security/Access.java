package com.lias.security;
import com.lias.model.Membre;
import java.util.Set;
public final class Access {
    private Access() {}
    public static boolean active(Membre u){return u!=null&&u.getStatutActuel()!=null&&Set.of("PERMANENT","ASSOCIE","DOCTORANT").contains(u.getStatutActuel())&&"ACTIF".equals(u.getCompteStatut());}
    public static boolean director(Membre u){return active(u)&&"PERMANENT".equals(u.getStatutActuel())&&"DIRECTEUR".equals(u.getRoleActuel());}
    public static boolean admin(Membre u){return active(u)&&"PERMANENT".equals(u.getStatutActuel())&&"ADMIN".equals(u.getRoleActuel());}
    public static boolean direction(Membre u){return director(u)||(active(u)&&"PERMANENT".equals(u.getStatutActuel())&&"VICE_DIRECTEUR".equals(u.getRoleActuel()));}
    public static boolean internal(Membre u){return active(u)&&"PERMANENT".equals(u.getStatutActuel());}
    public static boolean assignTeam(Membre u){return direction(u)||admin(u);}
    /** A legacy member may be administered without gaining authentication rights. */
    public static boolean teamTarget(Membre u){return u!=null&&"ACTIF".equals(u.getCompteStatut())&&(active(u)||u.getStatutActuel()==null||u.getStatutActuel().isBlank());}
    public static boolean write(Membre u,String module,String action){
        return "equipes".equals(module)&&"affecter".equals(action)?assignTeam(u):write(u,module);
    }
    public static boolean read(Membre u,String module){
        if(Set.of("index","login","candidature","equipes","membres","evenements","publications").contains(module))return true;
        if(!active(u))return false;
        if(Set.of("profil","dashboard","notifications","recherche").contains(module))return true;
        if("DOCTORANT".equals(u.getStatutActuel()))return false;
        if("calendrier".equals(module))return true;
        if(!internal(u))return false;
        if("demandes".equals(module))return director(u);
        if(Set.of("gouvernance","historique","comptes").contains(module))return director(u)||admin(u);
        if("rapport".equals(module))return direction(u);
        return Set.of("documents","materiel","conventions","reunions","messagerie").contains(module);
    }
    public static boolean write(Membre u,String module){
        if(!active(u))return false;
        if("publications".equals(module))return internal(u)||"DOCTORANT".equals(u.getStatutActuel())||u.isPublicationAutorisee();
        if(Set.of("profil","notifications").contains(module))return true;
        if(Set.of("documents","materiel","messagerie").contains(module))return internal(u);
        if("demandes".equals(module))return director(u);
        if(Set.of("gouvernance","comptes").contains(module))return director(u)||admin(u);
        return direction(u)&&Set.of("equipes","evenements","conventions","reunions").contains(module);
    }
    public static void require(boolean allowed){if(!allowed)throw new SecurityException("Vous n'avez pas l'autorisation d'effectuer cette action.");}
}
