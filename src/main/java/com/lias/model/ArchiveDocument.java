package com.lias.model;
import java.sql.Date;

public class ArchiveDocument {
    private int idDoc;
    private String titre;
    private String typeDoc;
    private String nomFichier;
    private int idDeposeur;
    private String nomDeposeur; // Pour l'affichage sur la page web
    private Date dateDepot;

    public ArchiveDocument() {}

    public int getIdDoc() { return idDoc; }
    public void setIdDoc(int idDoc) { this.idDoc = idDoc; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getTypeDoc() { return typeDoc; }
    public void setTypeDoc(String typeDoc) { this.typeDoc = typeDoc; }
    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }
    public int getIdDeposeur() { return idDeposeur; }
    public void setIdDeposeur(int idDeposeur) { this.idDeposeur = idDeposeur; }
    public String getNomDeposeur() { return nomDeposeur; }
    public void setNomDeposeur(String nomDeposeur) { this.nomDeposeur = nomDeposeur; }
    public Date getDateDepot() { return dateDepot; }
    public void setDateDepot(Date dateDepot) { this.dateDepot = dateDepot; }
}