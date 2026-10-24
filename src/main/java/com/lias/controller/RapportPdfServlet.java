package com.lias.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import com.lias.model.Membre;
import com.lias.security.Access;
import com.lias.service.*;
import com.lias.view.Page;
import com.lias.config.DBConnection;

@WebServlet("/RapportPdfServlet")
public class RapportPdfServlet extends HttpServlet {
 private static final long serialVersionUID=1L;
 @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws IOException {
  Object value=r.getSession().getAttribute("user");Membre u=value instanceof Membre?(Membre)value:null;
  if(!Access.direction(u)){s.sendError(403);return;}
  try {
   Page p=new Page("rapport","Rapport annuel");AnnualReport.populate(p,r.getParameter("annee"));byte[] pdf=ReportPdf.render(p);
   try(var c=DBConnection.getConnection()){Trace.audit(c,u,"EXPORT_RAPPORT","rapport",0,null,p.title);}
   s.setContentType("application/pdf");s.setHeader("Content-Disposition","attachment; filename=rapport-lias.pdf");s.setHeader("Cache-Control","no-store");s.setContentLength(pdf.length);s.getOutputStream().write(pdf);
  }catch(IllegalArgumentException e){s.sendError(400,"Année invalide");}
  catch(Exception e){getServletContext().log("Annual PDF unavailable",e);if(!s.isCommitted())s.sendError(503,"Rapport indisponible");}
 }
}
