package com.lias.model;

public class Membre {
    private int idMembre;
    private String nom;
    private String prenom;
    private String email;
    private String photo;
    private String compteStatut;
    private boolean publicationAutorisee;
    public boolean isPublicationAutorisee() { return publicationAutorisee; }
    public void setPublicationAutorisee(boolean value) { publicationAutorisee = value; }
    private String statutActuel; // Ajouté pour gérer le statut du membre
    private String roleActuel;   // Ajouté pour gérer la responsabilité administrative
    
    // Constructeur vide
    public Membre() {}

    // Constructeur complet
    public Membre(int idMembre, String nom, String prenom, String email, String photo, String compteStatut, String statutActuel, String roleActuel) {
        this.idMembre = idMembre;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.photo = photo;
        this.compteStatut = compteStatut;
        this.statutActuel = statutActuel;
        this.roleActuel = roleActuel;
    }

    // Getters et Setters
    public int getIdMembre() { return idMembre; }
    public void setIdMembre(int idMembre) { this.idMembre = idMembre; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getCompteStatut() { return compteStatut; }
    public void setCompteStatut(String compteStatut) { this.compteStatut = compteStatut; }

    public String getStatutActuel() { return statutActuel; }
    public void setStatutActuel(String statutActuel) { this.statutActuel = statutActuel; }

    public String getRoleActuel() { return roleActuel; }
    public void setRoleActuel(String roleActuel) { this.roleActuel = roleActuel; }
}
