package com.lias.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.MultipartConfig;
@WebServlet("/NotificationServlet")
@MultipartConfig(maxFileSize=10485760,maxRequestSize=12582912,fileSizeThreshold=1048576)
public class NotificationServlet extends ActionServlet {
 @Override protected String module(){return "notifications";}
}
