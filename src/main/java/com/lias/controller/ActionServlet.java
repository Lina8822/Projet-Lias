package com.lias.controller;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.*;import java.sql.*;import java.util.*;
import com.lias.config.DBConnection;import com.lias.dao.*;import com.lias.model.Membre;import com.lias.model.Module;import com.lias.service.*;import com.lias.security.*;
/** Common POST controller, inherited by the original Servlet routes. */
public abstract class ActionServlet extends HttpServlet {
 protected abstract String module();
 protected String module(HttpServletRequest r){return module();}
 protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {
  String module=module(r),file=null;Membre actor=module.equals("candidature")?null:(Membre)r.getSession().getAttribute("user");
  Map<String,String> values=new HashMap<>();r.getParameterMap().forEach((k,v)->{if(v.length>0)values.put(k,v[0]);});Input in=new Input(values);String action=values.getOrDefault("action","save");
  try {
   if(!module.equals("candidature"))Access.require(Access.write(actor,module,action));
   if(r.getContentType()!=null&&r.getContentType().startsWith("multipart/"))file=FilesService.save(r.getPart("fichier"),module.equals("profil"));
   try(Connection c=DBConnection.getConnection()){
    c.setAutoCommit(false);
    try {
     // Serialize sensitive writes with governance to make revocation atomic.
     Sql.rows(c,"SELECT id FROM utilisateur ORDER BY id LIMIT 1 FOR UPDATE");
     // Recheck the actor in the transaction.
     if(actor!=null){actor=new AccountDAO().load(c,actor.getIdMembre());Access.require(Access.write(actor,module,action));}
     MembershipService members=new MembershipService();CommunicationService comm=new CommunicationService();
     switch(module){
      case "candidature"->members.apply(c,in,file);
      case "demandes"->members.decide(c,actor,in);
      case "profil"->{if(action.equals("password"))members.password(c,actor,in);else members.profile(c,actor,in,file);}
      case "comptes"->{if(action.equals("parametre")){Access.require(Access.admin(actor));String date=in.date("date_creation",true);var old=Sql.rows(c,"SELECT valeur FROM parametre WHERE cle='date_creation_laboratoire' FOR UPDATE");if(old.isEmpty())Sql.update(c,"INSERT INTO parametre(cle,valeur) VALUES ('date_creation_laboratoire',?)",date);else Sql.update(c,"UPDATE parametre SET valeur=? WHERE cle='date_creation_laboratoire'",date);Trace.audit(c,actor,"PARAMETRAGE","parametre",0,old,date);}else if(action.equals("permission")){Access.require(Access.admin(actor)||Access.director(actor));int id=in.id("id");var old=Sql.one(c,"SELECT publication_autorisee,date_naissance,date_embauche FROM utilisateur WHERE id=? FOR UPDATE",id);Map<String,Object> updated=new LinkedHashMap<>();updated.put("publication_autorisee",in.choice("publication_autorisee","1","0"));updated.put("date_naissance",in.date("date_naissance",false));updated.put("date_embauche",in.date("date_embauche",false));ContentDAO.update(c,"utilisateur","id",id,updated);Trace.audit(c,actor,"PARAMETRES_COMPTE","utilisateur",id,old,updated);}else members.lifecycle(c,actor,in);}
      case "gouvernance"->{if(action.equals("cloturer"))members.closeMandate(c,actor,in);else members.mandate(c,actor,in);}
      case "materiel"->new MaterialService().execute(c,actor,in,action);
      case "messagerie"->{if(action.equals("lire"))comm.readMessage(c,actor,in.id("id"));else comm.message(c,actor,in);}
      case "notifications"->comm.readNotification(c,actor,in.id("id"));
      case "documents"->comm.document(c,actor,in,file);
      case "equipes"->{if(action.equals("affecter"))members.assignTeam(c,actor,in);else save(c,actor,module,in,action);}
      case "evenements","reunions"->{if(action.equals("participant"))comm.participant(c,actor,in,module.equals("reunions"));else if(action.equals("pv"))comm.document(c,actor,in,file);else save(c,actor,module,in,action);}
      case "conventions"->{String section=values.getOrDefault("section","conventions");if(!Set.of("conventions","partenaires","activites").contains(section))throw new IllegalArgumentException("Section inconnue.");save(c,actor,section,in,action);}
      default->save(c,actor,module,in,action);
     }
     c.commit();
    }catch(Exception e){c.rollback();throw e;}
   }
   r.getSession().setAttribute("succes",module.equals("candidature")?"Candidature enregistrée. Vous pourrez vous connecter avec le mot de passe choisi après acceptation.":"Opération enregistrée avec succès.");
  }catch(SecurityException e){cleanup(file);s.sendError(403);return;}
  catch(IllegalArgumentException e){cleanup(file);r.getSession().setAttribute("erreur",e.getMessage());Map<String,String> retained=new HashMap<>(values);retained.keySet().removeIf(k->k.toLowerCase().contains("password")||k.equals("csrf"));r.getSession().setAttribute("formValues",retained);}
  catch(Exception e){cleanup(file);getServletContext().log("Action failed: "+module+"/"+action,e);r.getSession().setAttribute("erreur","L’opération n’a pas abouti. Vérifiez les données et réessayez.");}
  String page=module.equals("comptes")?"membres":module.equals("notifications")?"notifications":module;
  s.sendRedirect(r.getContextPath()+"/"+page+".jsp");
 }
 private void save(Connection c,Membre actor,String module,Input in,String action)throws SQLException {ContentDAO dao=new ContentDAO();if(action.equals("archive"))dao.archive(c,actor,Module.get(module),in.id("id"));else if(action.equals("save"))dao.save(c,actor,Module.get(module),in);else throw new IllegalArgumentException("Action inconnue.");}
 private void cleanup(String file){if(file!=null)try{java.nio.file.Files.deleteIfExists(FilesService.resolve(file));}catch(IOException e){getServletContext().log("Orphan upload: "+file,e);}}
}
