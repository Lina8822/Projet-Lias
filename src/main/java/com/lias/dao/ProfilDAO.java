package com.lias.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.lias.config.DBConnection;
import com.lias.model.HistoriqueAffiliation;

public class ProfilDAO {

    // Met à jour la table utilisateur
    public boolean modifierProfil(int idMembre, String biographie, String centreInteret, String email) {
        String sql = "UPDATE utilisateur SET biographie = ?, centre_interet = ?, email = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, biographie);
            ps.setString(2, centreInteret);
            ps.setString(3, email);
            ps.setInt(4, idMembre);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modifierProfil : " + e.getMessage());
            return false;
        }
    }

    // Récupère les lignes temporelles de l'historique
    public List<HistoriqueAffiliation> getHistoriqueMembre(int idMembre) {
        List<HistoriqueAffiliation> liste = new ArrayList<>();
        String sql = "SELECT * FROM historique_affiliation WHERE id_membre = ? ORDER BY date_fin DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMembre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistoriqueAffiliation ha = new HistoriqueAffiliation();
                    ha.setIdHistorique(rs.getInt("id_historique"));
                    ha.setIdMembre(rs.getInt("id_membre"));
                    ha.setAncienStatut(rs.getString("ancien_statut"));
                    ha.setAncienRole(rs.getString("ancien_role"));
                    ha.setDateDebut(rs.getDate("date_debut"));
                    ha.setDateFin(rs.getDate("date_fin"));
                    liste.add(ha);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getHistoriqueMembre : " + e.getMessage());
        }
        return liste;
    }
}