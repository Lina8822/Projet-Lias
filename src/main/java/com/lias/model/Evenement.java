package com.lias.model;

import java.sql.Date;

public class Evenement {
    private int idEvent;
    private String titre;
    private String typeEvent;
    private Date dateEvent;
    private String lieu;
    private String description;

    public Evenement() {}

    // Getters et Setters
    public int getIdEvent() { return idEvent; }
    public void setIdEvent(int idEvent) { this.idEvent = idEvent; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getTypeEvent() { return typeEvent; }
    public void setTypeEvent(String typeEvent) { this.typeEvent = typeEvent; }
    public Date getDateEvent() { return dateEvent; }
    public void setDateEvent(Date dateEvent) { this.dateEvent = dateEvent; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}