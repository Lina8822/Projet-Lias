package com.lias.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.MultipartConfig;
/** Route retained; delegates to the existing validated transactional services. */
@WebServlet("/AjouterEvenementServlet")
@MultipartConfig(maxFileSize=10485760,maxRequestSize=12582912,fileSizeThreshold=1048576)
public class AjouterEvenementServlet extends ActionServlet {
 private static final long serialVersionUID = 1L;
 @Override protected String module(){return "evenements";}
}
