package com.lias.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.lias.config.DBConnection;
import com.lias.model.DemandeMateriel;

public class MaterielDAO {

    // 1. Récupérer toutes les demandes de matériel (avec les noms des membres)
    public List<DemandeMateriel> getAllDemandesMateriel() {
        List<DemandeMateriel> liste = new ArrayList<>();
        String sql = "SELECT dm.*, u.nom, u.prenom FROM demande_materiel dm " +
                     "JOIN utilisateur u ON dm.id_membre = u.id " +
                     "ORDER BY dm.date_demande DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DemandeMateriel dm = new DemandeMateriel();
                dm.setIdDemandeMat(rs.getInt("id_demande_mat"));
                dm.setIdMembre(rs.getInt("id_membre"));
                dm.setNomMateriel(rs.getString("nom_materiel"));
                dm.setQuantite(rs.getInt("quantite"));
                dm.setDescription(rs.getString("description"));
                dm.setEtatDemande(rs.getString("etat_demande"));
                dm.setDateDemande(rs.getTimestamp("date_demande"));
                dm.setNomDemandeur(rs.getString("nom"));
                dm.setPrenomDemandeur(rs.getString("prenom"));
                liste.add(dm);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getAllDemandesMateriel : " + e.getMessage());
        }
        return liste;
    }

    // 2. Enregistrer une nouvelle demande de matériel (Besoin membre)
 // 2. Enregistrer une nouvelle demande de matériel (Besoin membre)
    public boolean insererDemandeMateriel(int idMembre, String nomMat, int qte, String desc) {
        String sql = "INSERT INTO demande_materiel (id_membre, nom_materiel, quantite, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMembre); // Utilisation de setInt au lieu de setLong
            ps.setString(2, nomMat);
            ps.setInt(3, qte);
            ps.setString(4, desc);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur insererDemandeMateriel : " + e.getMessage());
            return false;
        }
    }
    // 3. Valider ou Refuser une demande de matériel (Action Direction)
    public boolean traiterDemandeMateriel(int idDemandeMat, String nouvelEtat) {
        String sql = "UPDATE demande_materiel SET etat_demande = ? WHERE id_demande_mat = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouvelEtat);
            ps.setInt(2, idDemandeMat);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur traiterDemandeMateriel : " + e.getMessage());
            return false;
        }
    }
}