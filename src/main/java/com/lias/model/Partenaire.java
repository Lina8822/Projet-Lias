package com.lias.model;
import java.sql.Date;

public class Partenaire {
    private int idPartenaire;
    private String nomInstitution;
    private String typeConvention;
    private Date dateSignature;
    private String description;

    public Partenaire() {}

    public int getIdPartenaire() { return idPartenaire; }
    public void setIdPartenaire(int idPartenaire) { this.idPartenaire = idPartenaire; }
    public String getNomInstitution() { return nomInstitution; }
    public void setNomInstitution(String nomInstitution) { this.nomInstitution = nomInstitution; }
    public String getTypeConvention() { return typeConvention; }
    public void setTypeConvention(String typeConvention) { this.typeConvention = typeConvention; }
    public Date getDateSignature() { return dateSignature; }
    public void setDateSignature(Date dateSignature) { this.dateSignature = dateSignature; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}