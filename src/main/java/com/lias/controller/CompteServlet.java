package com.lias.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.MultipartConfig;
@WebServlet("/CompteServlet")
@MultipartConfig(maxFileSize=10485760,maxRequestSize=12582912,fileSizeThreshold=1048576)
public class CompteServlet extends ActionServlet {
 @Override protected String module(){return "comptes";}
}
