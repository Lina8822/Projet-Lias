package com.lias.controller;
import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.*;import java.nio.file.*;
import com.lias.dao.Sql;import com.lias.model.Membre;import com.lias.service.FilesService;
@WebServlet("/PhotoServlet") public class PhotoServlet extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse s)throws IOException {
  try{int id=Integer.parseInt(r.getParameter("id"));var u=Sql.rows("SELECT photo,profil_public FROM utilisateur WHERE id=?",id).get(0);Membre actor=(Membre)r.getSession().getAttribute("user");
   if(!Boolean.TRUE.equals(u.get("profil_public"))&&!"1".equals(Sql.str(u.get("profil_public")))&&(actor==null||actor.getIdMembre()!=id)){s.sendError(404);return;}
   Path p=FilesService.resolve(Sql.str(u.get("photo")));s.setContentType(p.toString().endsWith(".png")?"image/png":"image/jpeg");Files.copy(p,s.getOutputStream());
  }catch(Exception e){s.sendError(404);}
 }
}
