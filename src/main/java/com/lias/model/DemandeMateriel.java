package com.lias.model;

import java.sql.Timestamp;

public class DemandeMateriel {
    private int idDemandeMat;
    private int idMembre;
    private String nomMateriel;
    private int quantite;
    private String description;
    private String etatDemande;
    private Timestamp dateDemande;
    
    // Propriétés de jointure pour afficher le nom du demandeur sur l'interface
    private String nomDemandeur;
    private String prenomDemandeur;

    public DemandeMateriel() {}

    // Getters et Setters
    public int getIdDemandeMat() { return idDemandeMat; }
    public void setIdDemandeMat(int idDemandeMat) { this.idDemandeMat = idDemandeMat; }
    public int getIdMembre() { return idMembre; }
    public void setIdMembre(int idMembre) { this.idMembre = idMembre; }
    public String getNomMateriel() { return nomMateriel; }
    public void setNomMateriel(String nomMateriel) { this.nomMateriel = nomMateriel; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEtatDemande() { return etatDemande; }
    public void setEtatDemande(String etatDemande) { this.etatDemande = etatDemande; }
    public Timestamp getDateDemande() { return dateDemande; }
    public void setDateDemande(Timestamp dateDemande) { this.dateDemande = dateDemande; }
    public String getNomDemandeur() { return nomDemandeur; }
    public void setNomDemandeur(String nomDemandeur) { this.nomDemandeur = nomDemandeur; }
    public String getPrenomDemandeur() { return prenomDemandeur; }
    public void setPrenomDemandeur(String prenomDemandeur) { this.prenomDemandeur = prenomDemandeur; }
}
