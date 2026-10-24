package com.lias.model;
import java.sql.Timestamp;

public class Message {
    private int idMessage;
    private int idExpediteur;
    private String nomExpediteur; // Pour afficher "Prof. Omar a dit :"
    private String contenu;
    private Timestamp dateEnvoi;

    public Message() {}

    public int getIdMessage() { return idMessage; }
    public void setIdMessage(int idMessage) { this.idMessage = idMessage; }
    public int getIdExpediteur() { return idExpediteur; }
    public void setIdExpediteur(int idExpediteur) { this.idExpediteur = idExpediteur; }
    public String getNomExpediteur() { return nomExpediteur; }
    public void setNomExpediteur(String nomExpediteur) { this.nomExpediteur = nomExpediteur; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public Timestamp getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(Timestamp dateEnvoi) { this.dateEnvoi = dateEnvoi; }
}