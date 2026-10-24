package com.lias;

import com.lias.config.DBConnection;
import com.lias.dao.*;
import com.lias.model.Membre;
import com.lias.service.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in JDBC tests; all fixture changes are rolled back, on lias_test* only. */
class InstitutionalHistoryTest {
 Connection c; Membre admin,member; int team;
 Input input(String... values){Map<String,String> m=new HashMap<>();for(int i=0;i<values.length;i+=2)m.put(values[i],values[i+1]);return new Input(m);}
 @BeforeEach void prepare()throws Exception {
  Assumptions.assumeTrue(DBConnection.setting("LIAS_DB_URL","").contains("/lias_test"),"Set LIAS_DB_URL to an isolated lias_test* database");
  c=DBConnection.getConnection();assertTrue(c.getCatalog().startsWith("lias_test"));c.setAutoCommit(false);
  int a=Sql.insert(c,"INSERT INTO utilisateur(nom,email,statut_actuel,role_actuel,compte_statut) VALUES ('JUnit admin',?,'PERMANENT','ADMIN','ACTIF')",UUID.randomUUID()+"@example.test");
  int m=Sql.insert(c,"INSERT INTO utilisateur(nom,email,statut_actuel,role_actuel,compte_statut) VALUES ('JUnit member',?,'PERMANENT','MEMBRE_EQUIPE','ACTIF')",UUID.randomUUID()+"@example.test");
  admin=new AccountDAO().load(c,a);member=new AccountDAO().load(c,m);
  team=Sql.insert(c,"INSERT INTO equipe(nom,date_creation) VALUES ('JUnit team',CURRENT_DATE)");
  Sql.update(c,"INSERT INTO historique_affiliation(id_membre,ancien_statut,affiliation,date_debut) VALUES (?,'PERMANENT','LIAS',CURRENT_DATE)",m);
 }
 @AfterEach void rollback()throws Exception {if(c!=null){c.rollback();c.close();}}
 @Test void futureMandateClosedByDepartureKeepsValidDates()throws Exception {
  MembershipService service=new MembershipService();String future=LocalDate.now().plusDays(30).toString();
  service.mandate(c,admin,input("membre_id",""+member.getIdMembre(),"type_mandat","VICE_DIRECTEUR","date_debut",future));
  service.lifecycle(c,admin,input("id",""+member.getIdMembre(),"statut_actuel","ANCIEN_MEMBRE"));
  var mandate=Sql.one(c,"SELECT statut,date_fin>=date_debut valid FROM mandat WHERE id_membre=?",member.getIdMembre());
  assertEquals("TERMINE",mandate.get("statut"));assertEquals(1,Sql.id(mandate.get("valid")));
 }
 @Test void departureAndReturnPreserveAllAffiliationPeriods()throws Exception {
  MembershipService service=new MembershipService();int uid=member.getIdMembre();
  var original=Sql.one(c,"SELECT id_historique,date_debut FROM historique_affiliation WHERE id_membre=?",uid);
  service.lifecycle(c,admin,input("id",""+uid,"statut_actuel","ANCIEN_MEMBRE"));
  assertEquals("DESACTIVE",new AccountDAO().load(c,uid).getCompteStatut());
  service.lifecycle(c,admin,input("id",""+uid,"statut_actuel","PERMANENT"));
  var periods=Sql.rows(c,"SELECT * FROM historique_affiliation WHERE id_membre=? ORDER BY id_historique",uid);
  assertEquals(2,periods.size());assertEquals(original.get("id_historique"),periods.get(0).get("id_historique"));
  assertEquals(original.get("date_debut"),periods.get(0).get("date_debut"));assertNotNull(periods.get(0).get("date_fin"));assertNull(periods.get(1).get("date_fin"));
 }
 @Test void successiveRolesAndDirectorSuccessionKeepMandates()throws Exception {
  MembershipService service=new MembershipService();String today=LocalDate.now().toString();int uid=member.getIdMembre();
  List<Integer> created=new ArrayList<>();
  for(String role:List.of("MEMBRE_EQUIPE","CHEF_EQUIPE","VICE_DIRECTEUR","DIRECTEUR")) {
   service.mandate(c,admin,input("membre_id",""+uid,"type_mandat",role,"equipe_id",""+team,"date_debut",today));
   assertEquals(role,new AccountDAO().load(c,uid).getRoleActuel());
   int id=Sql.id(Sql.one(c,"SELECT MAX(id_mandat) id FROM mandat WHERE id_membre=?",uid).get("id"));created.add(id);
   if(!role.equals("DIRECTEUR"))service.closeMandate(c,admin,input("id",""+id));
  }
  int successor=Sql.insert(c,"INSERT INTO utilisateur(nom,statut_actuel,role_actuel,compte_statut) VALUES ('Successor','PERMANENT','MEMBRE_EQUIPE','ACTIF')");
  service.mandate(c,admin,input("membre_id",""+successor,"type_mandat","DIRECTEUR","date_debut",today));
  assertEquals("MEMBRE_EQUIPE",new AccountDAO().load(c,uid).getRoleActuel());assertEquals("DIRECTEUR",new AccountDAO().load(c,successor).getRoleActuel());
  var history=Sql.rows(c,"SELECT id_mandat,statut FROM mandat WHERE id_membre=? ORDER BY id_mandat",uid);
  assertEquals(4,history.size());for(int i=0;i<4;i++){assertEquals(created.get(i).intValue(),Sql.id(history.get(i).get("id_mandat")));assertEquals("TERMINE",history.get(i).get("statut"));}
 }
 @Test void sameTeamCanHaveMultiplePeriodsWithoutLoss()throws Exception {
  MembershipService service=new MembershipService();int uid=member.getIdMembre();
  int director=Sql.insert(c,"INSERT INTO mandat(id_membre,type_mandat,date_debut,statut) VALUES (?,'DIRECTEUR',CURRENT_DATE,'ACTIF')",uid);
  Membre actor=new AccountDAO().load(c,uid);
  service.assignTeam(c,actor,input("membre_id",""+admin.getIdMembre(),"equipe_id",""+team));
  service.assignTeam(c,actor,input("membre_id",""+admin.getIdMembre(),"equipe_id",""+team));
  var periods=Sql.rows(c,"SELECT date_fin FROM membre_equipe WHERE utilisateur_id=? ORDER BY id",admin.getIdMembre());
  assertEquals(2,periods.size());assertNotNull(periods.get(0).get("date_fin"));assertNull(periods.get(1).get("date_fin"));
  assertTrue(director>0);
 }
 @Test void foreignKeyProtectsMemberHistoryFromDeletion()throws Exception {
  Savepoint s=c.setSavepoint();
  assertThrows(SQLException.class,()->Sql.update(c,"DELETE FROM utilisateur WHERE id=?",member.getIdMembre()));c.rollback(s);
  assertNotNull(new AccountDAO().load(c,member.getIdMembre()));
 }
 @Test void adminMovesLegacyMemberKeepingTeamHistoryAndNullStatus()throws Exception {
  int uid=member.getIdMembre();MembershipService service=new MembershipService();
  Sql.update(c,"UPDATE utilisateur SET statut_actuel=NULL,role_actuel=NULL WHERE id=?",uid);
  service.assignTeam(c,admin,input("membre_id",""+uid,"equipe_id",""+team));
  var original=Sql.one(c,"SELECT * FROM membre_equipe WHERE utilisateur_id=?",uid);
  int second=Sql.insert(c,"INSERT INTO equipe(nom,date_creation) VALUES ('JUnit team B',CURRENT_DATE)");
  service.assignTeam(c,admin,input("membre_id",""+uid,"equipe_id",""+second));
  var periods=Sql.rows(c,"SELECT * FROM membre_equipe WHERE utilisateur_id=? ORDER BY id",uid);
  assertEquals(2,periods.size());assertEquals(original.get("id"),periods.get(0).get("id"));
  assertEquals(original.get("date_debut"),periods.get(0).get("date_debut"));
  assertEquals(team,Sql.id(periods.get(0).get("equipe_id")));assertNotNull(periods.get(0).get("date_fin"));
  assertEquals(second,Sql.id(periods.get(1).get("equipe_id")));assertNull(periods.get(1).get("date_fin"));
  var current=Sql.one(c,"SELECT equipe_id,statut_actuel,role_actuel FROM utilisateur WHERE id=?",uid);
  assertEquals(second,Sql.id(current.get("equipe_id")));assertNull(current.get("statut_actuel"));assertNull(current.get("role_actuel"));
  assertFalse(com.lias.security.Access.active(new AccountDAO().load(c,uid)));
  assertEquals(2,Sql.rows(c,"SELECT id FROM audit_log WHERE action='AFFECTATION_EQUIPE' AND entite_id=?",uid).size());
 }
 @Test void unauthorizedAssignmentAndInactiveTargetDoNotChangeHistory()throws Exception {
  MembershipService service=new MembershipService();int uid=member.getIdMembre();
  assertThrows(SecurityException.class,()->service.assignTeam(c,member,input("membre_id",""+uid,"equipe_id",""+team)));
  Sql.update(c,"UPDATE utilisateur SET statut_actuel='RETRAITE',compte_statut='GELE' WHERE id=?",uid);
  assertThrows(IllegalArgumentException.class,()->service.assignTeam(c,admin,input("membre_id",""+uid,"equipe_id",""+team)));
  assertTrue(Sql.rows(c,"SELECT id FROM membre_equipe WHERE utilisateur_id=?",uid).isEmpty());
 }
 @Test void passwordMigrationPreservesDigestsAndAuditsWithoutSecrets()throws Exception {
  String digest="0123456789abcdef0123456789abcdef",plain="Legacy-private-2026!";
  Sql.update(c,"UPDATE utilisateur SET mot_de_passe=? WHERE id=?",digest,admin.getIdMembre());
  Sql.update(c,"UPDATE utilisateur SET mot_de_passe=? WHERE id=?",plain,member.getIdMembre());
  com.lias.config.DatabaseTool.migratePasswords(c);
  assertEquals(digest,Sql.one(c,"SELECT mot_de_passe FROM utilisateur WHERE id=?",admin.getIdMembre()).get("mot_de_passe"));
  assertTrue(com.lias.security.Passwords.verify(plain,Sql.str(Sql.one(c,"SELECT mot_de_passe FROM utilisateur WHERE id=?",member.getIdMembre()).get("mot_de_passe"))));
  var audit=Sql.one(c,"SELECT avant,apres FROM audit_log WHERE action='MIGRATION_PASSWORD' AND entite_id=?",member.getIdMembre());
  assertNull(audit.get("avant"));assertEquals("Stockage PBKDF2",audit.get("apres"));
  assertEquals(0,com.lias.config.DatabaseTool.migratePasswords(c));
 }
}
