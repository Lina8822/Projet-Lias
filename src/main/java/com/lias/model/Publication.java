package com.lias.model;

public class Publication {
    private int id_pub;
    private int id_auteur;
    private String titre;
    private String auteurs;
    private String journal_conference;
    private int annee;
    private String lien_pdf;

    public Publication() {}

    public int getId_pub() { return id_pub; }
    public void setId_pub(int id_pub) { this.id_pub = id_pub; }

    public int getId_auteur() { return id_auteur; }
    public void setId_auteur(int id_auteur) { this.id_auteur = id_auteur; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getAuteurs() { return auteurs; }
    public void setAuteurs(String auteurs) { this.auteurs = auteurs; }

    public String getJournalConference() { return journal_conference; }
    public void setJournalConference(String journal_conference) { this.journal_conference = journal_conference; }

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public String getLienPdf() { return lien_pdf; }
    public void setLienPdf(String lien_pdf) { this.lien_pdf = lien_pdf; }
}