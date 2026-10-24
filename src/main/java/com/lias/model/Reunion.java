package com.lias.model;

import java.sql.Date;

public class Reunion {
    private int idReunion;
    private String titre;
    private Date dateReunion;
    private String ordreDuJour;
    private String nomFichierPv;   // null tant que le PV n'a pas été déposé
    private int idOrganisateur;
    private String nomOrganisateur; // pour l'affichage (JOIN avec utilisateur)
    private Date dateCreation;

    public Reunion() {}

    public int getIdReunion() { return idReunion; }
    public void setIdReunion(int idReunion) { this.idReunion = idReunion; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Date getDateReunion() { return dateReunion; }
    public void setDateReunion(Date dateReunion) { this.dateReunion = dateReunion; }

    public String getOrdreDuJour() { return ordreDuJour; }
    public void setOrdreDuJour(String ordreDuJour) { this.ordreDuJour = ordreDuJour; }

    public String getNomFichierPv() { return nomFichierPv; }
    public void setNomFichierPv(String nomFichierPv) { this.nomFichierPv = nomFichierPv; }

    public boolean isPvDepose() { return nomFichierPv != null && !nomFichierPv.isEmpty(); }

    public int getIdOrganisateur() { return idOrganisateur; }
    public void setIdOrganisateur(int idOrganisateur) { this.idOrganisateur = idOrganisateur; }

    public String getNomOrganisateur() { return nomOrganisateur; }
    public void setNomOrganisateur(String nomOrganisateur) { this.nomOrganisateur = nomOrganisateur; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }
}
