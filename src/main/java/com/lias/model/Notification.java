package com.lias.model;
import java.sql.Timestamp;

public class Notification {
    private int idNotif;
    private int idMembre;
    private String message;
    private String typeNotif;
    private boolean estLu;
    private Timestamp dateCreation;

    public Notification() {}

    public int getIdNotif() { return idNotif; }
    public void setIdNotif(int idNotif) { this.idNotif = idNotif; }
    public int getIdMembre() { return idMembre; }
    public void setIdMembre(int idMembre) { this.idMembre = idMembre; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTypeNotif() { return typeNotif; }
    public void setTypeNotif(String typeNotif) { this.typeNotif = typeNotif; }
    public boolean isEstLu() { return estLu; }
    public void setEstLu(boolean estLu) { this.estLu = estLu; }
    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }
}