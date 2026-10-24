package com.lias.controller;
import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.*;import java.sql.*;import java.nio.file.*;import java.util.*;
import com.lias.dao.Sql;import com.lias.model.Membre;import com.lias.security.Access;import com.lias.service.*;
@WebServlet("/DownloadServlet") public class DownloadServlet extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse s)throws IOException {
  try {Membre actor=(Membre)r.getSession().getAttribute("user");String kind=r.getParameter("kind");int id=Integer.parseInt(r.getParameter("id"));String name;
   if("cv".equals(kind)){Access.require(Access.director(actor));name=Sql.str(Sql.rows("SELECT cv FROM demandes_adhesion WHERE id_demande=?",id).get(0).get("cv"));}
   else {Access.require(Access.internal(actor));name=Sql.str(Sql.rows("SELECT nom_fichier FROM document WHERE id_doc=?",id).get(0).get("nom_fichier"));}
   Path file=FilesService.resolve(name);if(!Files.isRegularFile(file)){s.sendError(404);return;}
   s.setContentType("application/pdf");s.setHeader("Content-Disposition","attachment; filename=\"document-"+id+".pdf\"");s.setContentLengthLong(Files.size(file));Files.copy(file,s.getOutputStream());
  }catch(SecurityException e){s.sendError(403);}catch(Exception e){s.sendError(404);}
 }
}
