package com.lias.model;

import java.sql.Timestamp;

public class DemandeAdhesion {
    private int idDemande;
    private String nom;
    private String prenom;
    private String email;
    private String cvPath;
    private String motivation;
    private Timestamp dateSoumission;
    private String etatDemande;

    public DemandeAdhesion() {}

    // Getters et Setters
    public int getIdDemande() { return idDemande; }
    public void setIdDemande(int idDemande) { this.idDemande = idDemande; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }
    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }
    public Timestamp getDateSoumission() { return dateSoumission; }
    public void setDateSoumission(Timestamp dateSoumission) { this.dateSoumission = dateSoumission; }
    public String getEtatDemande() { return etatDemande; }
    public void setEtatDemande(String etatDemande) { this.etatDemande = etatDemande; }
}