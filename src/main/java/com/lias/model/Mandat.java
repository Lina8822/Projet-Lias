package com.lias.model;
import java.sql.Date;

public class Mandat {
    private int idMandat;
    private int idMembre;
    private String nomMembre; // Pratique pour l'affichage
    private String typeMandat;
    private Date dateDebut;
    private Date dateFin;
    private String statut;

    public Mandat() {}

    // Getters et Setters
    public int getIdMandat() { return idMandat; }
    public void setIdMandat(int idMandat) { this.idMandat = idMandat; }
    public int getIdMembre() { return idMembre; }
    public void setIdMembre(int idMembre) { this.idMembre = idMembre; }
    public String getNomMembre() { return nomMembre; }
    public void setNomMembre(String nomMembre) { this.nomMembre = nomMembre; }
    public String getTypeMandat() { return typeMandat; }
    public void setTypeMandat(String typeMandat) { this.typeMandat = typeMandat; }
    public Date getDateDebut() { return dateDebut; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }
    public Date getDateFin() { return dateFin; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}