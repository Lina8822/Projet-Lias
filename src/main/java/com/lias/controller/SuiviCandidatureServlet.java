package com.lias.controller;
import jakarta.servlet.*;import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.*;
import com.lias.dao.Sql;import com.lias.security.*;
@WebServlet("/SuiviCandidatureServlet")
public class SuiviCandidatureServlet extends HttpServlet {
 protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {
  String message="Dossier introuvable ou identifiants incorrects.";
  try{
   if(LoginThrottle.blocked(r.getRemoteAddr()))throw new IllegalArgumentException("Trop de tentatives. Réessayez dans quelques minutes.");
   String email=r.getParameter("email"),password=r.getParameter("password");
   if(email==null||email.length()>254||password==null||password.length()>128)throw new IllegalArgumentException(message);
   var rows=Sql.rows("SELECT etat_demande,password_hash FROM demandes_adhesion WHERE email=? ORDER BY date_soumission DESC LIMIT 1",email.trim());
   if(!rows.isEmpty()&&Passwords.verify(password,Sql.str(rows.get(0).get("password_hash")))){message="État de votre candidature : "+Sql.str(rows.get(0).get("etat_demande"))+".";r.getSession().setAttribute("succes",message);}else{LoginThrottle.fail(r.getRemoteAddr());r.getSession().setAttribute("erreur",message);}
  }catch(IllegalArgumentException e){r.getSession().setAttribute("erreur",e.getMessage());}catch(Exception e){getServletContext().log("Suivi candidature",e);r.getSession().setAttribute("erreur","Le suivi est momentanément indisponible.");}
  s.sendRedirect(r.getContextPath()+"/candidature.jsp");
 }
}
