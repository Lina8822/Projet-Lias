package com.lias.model;

import java.sql.Date;

public class HistoriqueAffiliation {
    private int idHistorique;
    private int idMembre;
    private String ancienStatut;
    private String ancienRole;
    private Date dateDebut;
    private Date dateFin;

    public HistoriqueAffiliation() {}

    // Getters et Setters
    public int getIdHistorique() { return idHistorique; }
    public void setIdHistorique(int idHistorique) { this.idHistorique = idHistorique; }
    public int getIdMembre() { return idMembre; }
    public void setIdMembre(int idMembre) { this.idMembre = idMembre; }
    public String getAncienStatut() { return ancienStatut; }
    public void setAncienStatut(String ancienStatut) { this.ancienStatut = ancienStatut; }
    public String getAncienRole() { return ancienRole; }
    public void setAncienRole(String ancienRole) { this.ancienRole = ancienRole; }
    public Date getDateDebut() { return dateDebut; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }
    public String getDateFin() { return dateFin != null ? dateFin.toString() : "Présent"; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }
}