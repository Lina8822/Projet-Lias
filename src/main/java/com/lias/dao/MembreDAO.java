package com.lias.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.lias.config.DBConnection;
import com.lias.model.Membre;
import com.lias.model.DemandeAdhesion;
import com.lias.model.Publication;

public class MembreDAO {

    public boolean ajouterReunion(String titre, java.sql.Date date, String ordre, int auteur) {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO reunion(titre,date_reunion,ordre_du_jour,id_organisateur) VALUES (?,?,?,?)")) {
            p.setString(1,titre); p.setDate(2,date); p.setString(3,ordre); p.setInt(4,auteur);
            return p.executeUpdate() == 1;
        } catch (SQLException e) { throw new IllegalStateException("Enregistrement indisponible", e); }
    }

    public com.lias.model.Reunion getReunionById(int id) {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement("SELECT * FROM reunion WHERE id_reunion=?")) {
            p.setInt(1,id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                com.lias.model.Reunion v = new com.lias.model.Reunion();
                v.setIdReunion(id); v.setTitre(r.getString("titre")); v.setDateReunion(r.getDate("date_reunion"));
                v.setOrdreDuJour(r.getString("ordre_du_jour")); v.setNomFichierPv(r.getString("nom_fichier_pv"));
                v.setIdOrganisateur(r.getInt("id_organisateur")); return v;
            }
        } catch (SQLException e) { throw new IllegalStateException("Lecture indisponible", e); }
    }

    public boolean attacherPvReunion(int id, String fichier) {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE reunion SET nom_fichier_pv=? WHERE id_reunion=?")) {
            p.setString(1,fichier); p.setInt(2,id); return p.executeUpdate()==1;
        } catch (SQLException e) { throw new IllegalStateException("Enregistrement indisponible", e); }
    }

    // 1. Authentification
    public Membre authentifier(String email, String password) {
        try { return new AccountDAO().authenticate(email, password); } catch (SQLException e) { throw new IllegalStateException("Authentification indisponible", e); }
    }

    // 2. Demandes en attente
    public List<DemandeAdhesion> getDemandesEnAttente() {
        List<DemandeAdhesion> liste = new ArrayList<>();
        String sql = "SELECT id_demande, nom, prenom, email, motivation, date_soumission, etat_demande FROM demandes_adhesion WHERE etat_demande = 'EN_ATTENTE' ORDER BY date_soumission DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DemandeAdhesion d = new DemandeAdhesion();
                d.setIdDemande(rs.getInt("id_demande"));
                d.setNom(rs.getString("nom"));
                d.setPrenom(rs.getString("prenom"));
                d.setEmail(rs.getString("email"));
                d.setMotivation(rs.getString("motivation"));
                d.setDateSoumission(rs.getTimestamp("date_soumission"));
                d.setEtatDemande(rs.getString("etat_demande"));
                liste.add(d);
            }
        } catch (Exception e) {}
        return liste;
    }

    // 3bis. Vérifie si un email est déjà utilisé par un membre existant ou déjà en attente de validation
    // (évite les candidatures en double)
    public boolean emailDejaUtilise(String email) {
        String sql = "SELECT "
                   + "(SELECT COUNT(*) FROM utilisateur WHERE email = ?) "
                   + "+ (SELECT COUNT(*) FROM demandes_adhesion WHERE email = ? AND etat_demande = 'EN_ATTENTE') "
                   + "AS total";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("total") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur emailDejaUtilise : " + e.getMessage());
        }
        return false;
    }

    // 3ter. Soumission publique d'une candidature (formulaire externe, sans authentification)
    public boolean soumettreDemandeAdhesion(String nom, String prenom, String email, String motivation) {
        String sql = "INSERT INTO demandes_adhesion (nom, prenom, email, motivation, etat_demande, date_soumission) "
                   + "VALUES (?, ?, ?, ?, 'EN_ATTENTE', NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, email);
            ps.setString(4, motivation);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur soumettreDemandeAdhesion : " + e.getMessage());
            return false;
        }
    }

    // 3. Traiter demande
    public boolean traiterDemande(int idDemande, String nouvelEtat) {
        String sql = "UPDATE demandes_adhesion SET etat_demande = ? WHERE id_demande = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouvelEtat);
            ps.setInt(2, idDemande);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // 4. Demande par ID
    public DemandeAdhesion getDemandeById(int idDemande) {
        String sql = "SELECT * FROM demandes_adhesion WHERE id_demande = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DemandeAdhesion d = new DemandeAdhesion();
                    d.setIdDemande(rs.getInt("id_demande"));
                    d.setNom(rs.getString("nom"));
                    d.setPrenom(rs.getString("prenom"));
                    d.setEmail(rs.getString("email"));
                    return d;
                }
            }
        } catch (SQLException e) {}
        return null;
    }

    // 5. Créer compte
    public boolean creerCompteUtilisateur(String nom, String prenom, String email, String password) {
        String sql = "INSERT INTO utilisateur (nom, prenom, email, mot_de_passe) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, email);
            ps.setString(4, password);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // 6. Tous les membres
    public List<Membre> getAllMembres() {
        List<Membre> liste = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, email FROM utilisateur ORDER BY nom ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Membre m = new Membre();
                m.setIdMembre(rs.getInt("id"));
                m.setNom(rs.getString("nom"));
                m.setPrenom(rs.getString("prenom"));
                m.setEmail(rs.getString("email"));
                m.setStatutActuel("Enseignant-Chercheur"); 
                m.setRoleActuel("Membre");
                liste.add(m);
            }
        } catch (SQLException e) {}
        return liste;
    }

    // 7. PUBLICATIONS (LA VRAIE REQUÊTE POUR TON AFFICHAGE)
    public List<Publication> getAllPublications() {
        List<Publication> liste = new ArrayList<>();
        String sql = "SELECT id_pub, id_auteur, titre, auteurs, journal_conference, annee, lien_pdf FROM publication ORDER BY id_pub DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Publication p = new Publication();
                p.setId_pub(rs.getInt("id_pub"));
                p.setId_auteur(rs.getInt("id_auteur"));
                p.setTitre(rs.getString("titre"));
                p.setAuteurs(rs.getString("auteurs"));
                p.setJournalConference(rs.getString("journal_conference"));
                p.setAnnee(rs.getInt("annee"));
                p.setLienPdf(rs.getString("lien_pdf"));
                liste.add(p);
            }
        } catch (SQLException e) { System.err.println("Erreur Publications: " + e.getMessage()); }
        return liste;
    }

    // 8. EVENEMENTS
    public java.util.List<com.lias.model.Evenement> getAllEvenements() {
        java.util.List<com.lias.model.Evenement> liste = new java.util.ArrayList<>();
        String sql = "SELECT * FROM evenement ORDER BY date_event ASC";
        try (Connection conn = com.lias.config.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                com.lias.model.Evenement e = new com.lias.model.Evenement();
                e.setIdEvent(rs.getInt("id_event"));
                e.setTitre(rs.getString("titre"));
                e.setTypeEvent(rs.getString("type_event"));
                e.setDateEvent(rs.getDate("date_event"));
                e.setLieu(rs.getString("lieu"));
                e.setDescription(rs.getString("description"));
                liste.add(e);
            }
        } catch (SQLException e) {}
        return liste;
    }

    // 9. PLANIFIER EVENEMENT
    public boolean planifierEvenement(String titre, String typeEvent, java.sql.Date dateEvent, String lieu, String description) {
        String sql = "INSERT INTO evenement (titre, type_event, date_event, lieu, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = com.lias.config.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setString(2, typeEvent);
            ps.setDate(3, dateEvent);
            ps.setString(4, lieu);
            ps.setString(5, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
 // =========================================================================
    // STATISTIQUES DU DASHBOARD (NOUVELLES MÉTHODES)
    // =========================================================================

    // 1. Compter le nombre total de publications
    public int getCountPublications() {
        String sql = "SELECT COUNT(*) FROM publication";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getCountPublications : " + e.getMessage());
        }
        return 0;
    }

    // 2. Compter le nombre total d'événements
    public int getCountEvenements() {
        String sql = "SELECT COUNT(*) FROM evenement";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getCountEvenements : " + e.getMessage());
        }
        return 0;
    }

    // 3. Compter les demandes d'adhésion en attente
    public int getCountDemandesEnAttente() {
        String sql = "SELECT COUNT(*) FROM demandes_adhesion WHERE etat_demande = 'EN_ATTENTE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getCountDemandesEnAttente : " + e.getMessage());
        }
        return 0;
    }

    // 4. Compter le nombre total de membres (utilisateurs)
    public int getCountMembres() {
        String sql = "SELECT COUNT(*) FROM utilisateur";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getCountMembres : " + e.getMessage());
        }
        return 0;
    }
 // =========================================================================
    // GESTION DES EQUIPES
    // =========================================================================

    // 1. Récupérer toutes les équipes
    public java.util.List<com.lias.model.Equipe> getAllEquipes() {
        java.util.List<com.lias.model.Equipe> liste = new java.util.ArrayList<>();
        String sql = "SELECT * FROM equipe";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                com.lias.model.Equipe eq = new com.lias.model.Equipe();
                eq.setId(rs.getInt("id"));
                eq.setNom(rs.getString("nom"));
                eq.setDescription(rs.getString("description"));
                liste.add(eq);
            }
        } catch (SQLException e) { System.err.println("Erreur getAllEquipes : " + e.getMessage()); }
        return liste;
    }

    // 2. Créer une nouvelle équipe
    public boolean creerEquipe(String nom, String description) {
        String sql = "INSERT INTO equipe (nom, description) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // 3. Affecter un chercheur à une équipe
    public boolean affecterMembreEquipe(int utilisateurId, int equipeId) {
        // Optionnel : On peut d'abord supprimer son ancienne équipe s'il ne peut en avoir qu'une
        // String sqlDelete = "DELETE FROM membre_equipe WHERE utilisateur_id = ?";
        
        String sql = "INSERT INTO membre_equipe (utilisateur_id, equipe_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ps.setInt(2, equipeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
 // =========================================================================
    // GESTION DES MANDATS ET GOUVERNANCE
    // =========================================================================

    // 1. Récupérer l'historique de tous les mandats
    public java.util.List<com.lias.model.Mandat> getAllMandats() {
        java.util.List<com.lias.model.Mandat> liste = new java.util.ArrayList<>();
        String sql = "SELECT m.*, u.nom, u.prenom FROM mandat m JOIN utilisateur u ON m.id_membre = u.id ORDER BY m.statut ASC, m.date_debut DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                com.lias.model.Mandat m = new com.lias.model.Mandat();
                m.setIdMandat(rs.getInt("id_mandat"));
                m.setIdMembre(rs.getInt("id_membre"));
                m.setNomMembre(rs.getString("nom") + " " + rs.getString("prenom"));
                m.setTypeMandat(rs.getString("type_mandat"));
                m.setDateDebut(rs.getDate("date_debut"));
                m.setDateFin(rs.getDate("date_fin"));
                m.setStatut(rs.getString("statut"));
                liste.add(m);
            }
        } catch (SQLException e) { System.err.println("Erreur getAllMandats : " + e.getMessage()); }
        return liste;
    }

    // 2. Assigner un nouveau mandat (Historisation)
    public boolean assignerMandat(int idMembre, String typeMandat, java.sql.Date dateDebut) {
        // Optionnel mais recommandé : Clôturer l'ancien mandat actif pour ce type de poste
        String sqlUpdate = "UPDATE mandat SET statut = 'TERMINE', date_fin = CURRENT_DATE WHERE type_mandat = ? AND statut = 'ACTIF'";
        String sqlInsert = "INSERT INTO mandat (id_membre, type_mandat, date_debut, statut) VALUES (?, ?, ?, 'ACTIF')";
        
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement psUp = conn.prepareStatement(sqlUpdate)) {
                psUp.setString(1, typeMandat);
                psUp.executeUpdate(); // On termine l'ancien directeur/chef
            }
            try (PreparedStatement psIn = conn.prepareStatement(sqlInsert)) {
                psIn.setInt(1, idMembre);
                psIn.setString(2, typeMandat);
                psIn.setDate(3, dateDebut);
                return psIn.executeUpdate() > 0;
            }
        } catch (SQLException e) { return false; }
    }
 // =========================================================================
    // ARCHIVAGE DOCUMENTAIRE (UPLOAD & PV)
    // =========================================================================

    // 1. Récupérer tous les documents avec le nom de celui qui l'a posté
    public java.util.List<com.lias.model.ArchiveDocument> getAllDocuments() {
        java.util.List<com.lias.model.ArchiveDocument> liste = new java.util.ArrayList<>();
        String sql = "SELECT d.*, u.nom, u.prenom FROM document d LEFT JOIN utilisateur u ON d.id_deposeur = u.id ORDER BY d.id_doc DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                com.lias.model.ArchiveDocument doc = new com.lias.model.ArchiveDocument();
                doc.setIdDoc(rs.getInt("id_doc"));
                doc.setTitre(rs.getString("titre"));
                doc.setTypeDoc(rs.getString("type_doc"));
                doc.setNomFichier(rs.getString("nom_fichier"));
                doc.setIdDeposeur(rs.getInt("id_deposeur"));
                
                String nom = rs.getString("nom");
                if(nom != null) {
                    doc.setNomDeposeur(nom + " " + rs.getString("prenom"));
                } else {
                    doc.setNomDeposeur("Système");
                }
                
                doc.setDateDepot(rs.getDate("date_depot"));
                liste.add(doc);
            }
        } catch (SQLException e) { System.err.println("Erreur getAllDocuments : " + e.getMessage()); }
        return liste;
    }

    // 2. Enregistrer un nouveau document dans la BDD
    public boolean ajouterDocument(String titre, String typeDoc, String nomFichier, int idDeposeur, java.sql.Date dateDepot) {
        String sql = "INSERT INTO document (titre, type_doc, nom_fichier, id_deposeur, date_depot) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setString(2, typeDoc);
            ps.setString(3, nomFichier);
            ps.setInt(4, idDeposeur);
            ps.setDate(5, dateDepot);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            System.err.println("Erreur ajouterDocument : " + e.getMessage());
            return false; 
        }
    }
 // =========================================================================
    // CONVENTIONS ET COLLABORATIONS
    // =========================================================================

    // 1. Récupérer toutes les conventions
    public java.util.List<com.lias.model.Partenaire> getAllPartenaires() {
        java.util.List<com.lias.model.Partenaire> liste = new java.util.ArrayList<>();
        String sql = "SELECT * FROM partenaire ORDER BY date_signature DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                com.lias.model.Partenaire p = new com.lias.model.Partenaire();
                p.setIdPartenaire(rs.getInt("id_partenaire"));
                p.setNomInstitution(rs.getString("nom_institution"));
                p.setTypeConvention(rs.getString("type_convention"));
                p.setDateSignature(rs.getDate("date_signature"));
                p.setDescription(rs.getString("description"));
                liste.add(p);
            }
        } catch (SQLException e) { System.err.println("Erreur getAllPartenaires : " + e.getMessage()); }
        return liste;
    }

    // 2. Ajouter un nouveau partenaire/convention
    public boolean ajouterPartenaire(String nomInstitution, String typeConvention, java.sql.Date dateSignature, String description) {
        String sql = "INSERT INTO partenaire (nom_institution, type_convention, date_signature, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomInstitution);
            ps.setString(2, typeConvention);
            ps.setDate(3, dateSignature);
            ps.setString(4, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
 // =========================================================================
    // SYSTEME DE NOTIFICATIONS
    // =========================================================================

    // 1. Récupérer les notifications non lues pour un membre spécifique (ou globales)
    public java.util.List<com.lias.model.Notification> getUnreadNotifications(int idMembre, String role) {
        java.util.List<com.lias.model.Notification> liste = new java.util.ArrayList<>();
        // Si c'est le directeur, il voit aussi les notifs globales d'administration (ex: id_membre IS NULL)
        String sql = "SELECT * FROM notification WHERE est_lu = FALSE AND (id_membre = ? " + 
                     ("DIRECTEUR".equals(role) ? "OR id_membre IS NULL" : "") + 
                     ") ORDER BY date_creation DESC LIMIT 5";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMembre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.lias.model.Notification n = new com.lias.model.Notification();
                    n.setIdNotif(rs.getInt("id_notif"));
                    n.setMessage(rs.getString("message"));
                    n.setTypeNotif(rs.getString("type_notif"));
                    n.setDateCreation(rs.getTimestamp("date_creation"));
                    liste.add(n);
                }
            }
        } catch (SQLException e) { System.err.println("Erreur getUnreadNotifications : " + e.getMessage()); }
        return liste;
    }

    // 2. Créer une nouvelle notification
    public void ajouterNotification(Integer idMembre, String message, String typeNotif) {
        String sql = "INSERT INTO notification (id_membre, message, type_notif) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idMembre != null) {
                ps.setInt(1, idMembre);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER); // NULL = Notification pour les admins/directeurs
            }
            ps.setString(2, message);
            ps.setString(3, typeNotif);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Erreur ajouterNotification : " + e.getMessage()); }
    }
 // =========================================================================
    // COMMUNICATION INTERNE (MESSAGERIE)
    // =========================================================================

    // 1. Récupérer les derniers messages du laboratoire (Chat global)
    public java.util.List<com.lias.model.Message> getMessagesGlobaux() {
        java.util.List<com.lias.model.Message> liste = new java.util.ArrayList<>();
        String sql = "SELECT m.*, u.nom, u.prenom FROM message m JOIN utilisateur u ON m.id_expediteur = u.id ORDER BY m.date_envoi ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                com.lias.model.Message msg = new com.lias.model.Message();
                msg.setIdMessage(rs.getInt("id_message"));
                msg.setIdExpediteur(rs.getInt("id_expediteur"));
                msg.setNomExpediteur(rs.getString("nom") + " " + rs.getString("prenom"));
                msg.setContenu(rs.getString("contenu"));
                msg.setDateEnvoi(rs.getTimestamp("date_envoi"));
                liste.add(msg);
            }
        } catch (SQLException e) { System.err.println("Erreur getMessagesGlobaux : " + e.getMessage()); }
        return liste;
    }

    // 2. Envoyer un message sur le mur
    public boolean envoyerMessageGlobal(int idExpediteur, String contenu) {
        String sql = "INSERT INTO message (id_expediteur, contenu) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idExpediteur);
            ps.setString(2, contenu);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
    
}

