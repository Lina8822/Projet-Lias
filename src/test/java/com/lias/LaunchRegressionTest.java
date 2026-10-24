package com.lias;

import com.lias.model.Membre;
import com.lias.security.*;
import com.lias.controller.PageServlet;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LaunchRegressionTest {
    static Membre member(String status, String role) {
        Membre u = new Membre(); u.setIdMembre(1); u.setStatutActuel(status); u.setRoleActuel(role); u.setCompteStatut("ACTIF"); return u;
    }
    @Test void routesAndMappings() {
        var mappings = List.of(PageServlet.class.getAnnotation(jakarta.servlet.annotation.WebServlet.class).urlPatterns());
        for (String key : PageRoutes.PAGES) {
            assertEquals(key, PageRoutes.key("/"+key)); assertEquals(key, PageRoutes.key("/"+key+".jsp"));
            assertTrue(mappings.contains("/"+key)); assertTrue(mappings.contains("/"+key+".jsp"));
        }
        assertEquals("index", PageRoutes.key("")); assertNull(PageRoutes.key("/../dashboard.jsp")); assertNull(PageRoutes.key("/missing"));
    }
    @Test void legacyPasswordMigrationVerification() {
        assertTrue(Passwords.verifyLegacyPlaintext("old-password", "old-password"));
        assertFalse(Passwords.verifyLegacyPlaintext("wrong", "old-password"));
        assertFalse(Passwords.verifyLegacyPlaintext(null, "old-password"));
        String digest="0123456789abcdef0123456789abcdef";
        assertFalse(Passwords.verifyLegacyPlaintext(digest,digest));
        String hash=Passwords.hash("old-password");
        assertFalse(Passwords.verifyLegacyPlaintext(hash,hash));
        assertTrue(Passwords.verify("old-password",hash));
    }
    @Test void jwtValidation() {
        String token = JwtUtil.generateToken(member("PERMANENT", "DIRECTEUR"));
        assertEquals("1", JwtUtil.validateToken(token).get("sub"));
        assertNull(JwtUtil.validateToken(token+"x")); assertNull(JwtUtil.validateToken(null));
        assertNull(JwtUtil.validateToken(JwtUtil.generateToken(Map.of("sub","1","exp",1))));
    }
    @Test void accountPermissions() {
        assertFalse(Access.active(new Membre()));
        Membre doc = member("DOCTORANT", "MEMBRE_EQUIPE");
        assertTrue(Access.read(doc,"dashboard")); assertFalse(Access.read(doc,"gouvernance"));
        assertTrue(Access.read(member("PERMANENT","ADMIN"),"gouvernance"));
        Membre assoc = member("ASSOCIE","MEMBRE_EQUIPE"); assertFalse(Access.write(assoc,"publications"));
        assoc.setPublicationAutorisee(true); assertTrue(Access.write(assoc,"publications"));
        assoc.setCompteStatut("GELE"); assertFalse(Access.active(assoc));
    }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, InvocationHandler h) {return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h);}
    static class Exchange {
        Map<String,Object> attributes = new HashMap<>(); int status; String method="GET", csrf, action; String redirect; boolean passed; Cookie cookie;
        HttpSession session = proxy(HttpSession.class,(p,m,a)->switch(m.getName()) {
            case "getAttribute" -> attributes.get(a[0]); case "setAttribute" -> {attributes.put((String)a[0],a[1]);yield null;}
            case "invalidate" -> {attributes.clear();yield null;} default -> null;
        });
        HttpServletResponse response = proxy(HttpServletResponse.class,(p,m,a)-> {if(m.getName().equals("sendError"))status=(int)a[0];if(m.getName().equals("sendRedirect"))redirect=(String)a[0];return null;});
        void run(String path, Membre current) throws Exception {
            HttpServletRequest request = proxy(HttpServletRequest.class,(p,m,a)->switch(m.getName()) {
                case "getMethod" -> method; case "getParameter" -> "csrf".equals(a[0])?csrf:action; case "getServletPath" -> path; case "getContextPath" -> "/Lias-Jee"; case "getSession" -> session;
                case "getCookies" -> cookie==null?null:new Cookie[]{cookie}; default -> null;
            });
            new JwtFilter(){protected Membre loadUser(int id){return current;}}.doFilter(request,response,(r,s)->passed=true);
        }
    }
    @Test void anonymousAndAuthenticatedRoutes() throws Exception {
        for(String route:List.of("", "/login", "/membres", "/publications", "/evenements")) {Exchange e=new Exchange();e.run(route,null);assertTrue(e.passed,route);}
        for(String route:List.of("/dashboard", "/dashboard.jsp", "/demandes", "/gouvernance")) {Exchange e=new Exchange();e.run(route,null);assertEquals("/Lias-Jee/login.jsp",e.redirect);}
        for(String route:List.of("/dashboard", "/dashboard.jsp", "/demandes", "/gouvernance")) {Exchange e=new Exchange();Membre u=member("PERMANENT","DIRECTEUR");e.cookie=new Cookie("jwt",JwtUtil.generateToken(u));e.run(route,u);assertTrue(e.passed,route);assertSame(u,e.attributes.get("user"));}
    }
    @Test void publicEntryPagesIgnoreInvalidJwtAndCachedUser() throws Exception {
        for (String route : List.of("", "/", "/login", "/login.jsp", "/candidature", "/candidature.jsp")) {
            Exchange e = new Exchange();
            e.cookie = new Cookie("jwt", "invalid.expired.cookie");
            e.attributes.put("user", member("PERMANENT", "DIRECTEUR"));
            e.run(route, null);
            assertTrue(e.passed, route); assertNull(e.redirect, route); assertEquals(0, e.status, route);
        }
    }
    @Test void invalidRevokedAndForbidden() throws Exception {
        Exchange e=new Exchange();e.cookie=new Cookie("jwt","broken");e.run("/dashboard",null);assertNotNull(e.redirect);assertFalse(e.passed);
        e=new Exchange();e.cookie=new Cookie("jwt",JwtUtil.generateToken(Map.of("sub","bad","exp",Long.MAX_VALUE)));e.run("/dashboard",null);assertNotNull(e.redirect);
        Membre u=member("DOCTORANT","MEMBRE_EQUIPE");e=new Exchange();e.cookie=new Cookie("jwt",JwtUtil.generateToken(u));e.run("/gouvernance",u);assertEquals(403,e.status);
        u.setCompteStatut("DESACTIVE");e=new Exchange();e.cookie=new Cookie("jwt",JwtUtil.generateToken(u));e.run("/dashboard",u);assertNotNull(e.redirect);assertFalse(e.passed);
    }

    @Test void jwtEscapedProfileClaims() {
        String name = "Nom \"cité\" \\ et\nligne\té";
        var values = new LinkedHashMap<String,Object>();
        values.put("sub", "4"); values.put("nom",name); values.put("exp",Long.MAX_VALUE);
        assertEquals(name, JwtUtil.validateToken(JwtUtil.generateToken(values)).get("nom"));
    }
    @Test void teamAssignmentPermissionsAndDirectPost() throws Exception {
        for (String role : List.of("DIRECTEUR","VICE_DIRECTEUR","ADMIN","CHEF_EQUIPE","MEMBRE_EQUIPE")) {
            Membre u=member("PERMANENT",role); Exchange e=new Exchange();
            e.method="POST";e.action="affecter";e.csrf="token";e.attributes.put("csrf","token");
            e.cookie=new Cookie("jwt",JwtUtil.generateToken(u));e.run("/EquipeServlet",u);
            boolean allowed=List.of("DIRECTEUR","VICE_DIRECTEUR","ADMIN").contains(role);
            assertEquals(allowed,e.passed,role);assertEquals(allowed?0:403,e.status,role);
        }
        for(String status:List.of("ASSOCIE","DOCTORANT","RETRAITE","ANCIEN_MEMBRE"))
            assertFalse(Access.assignTeam(member(status,"DIRECTEUR")),status);
        assertFalse(Access.assignTeam(null));
        Exchange visitor=new Exchange();visitor.method="POST";visitor.action="affecter";visitor.csrf="token";
        visitor.attributes.put("csrf","token");visitor.run("/EquipeServlet",null);
        assertEquals(403,visitor.status);assertFalse(visitor.passed);
        Membre admin=member("PERMANENT","ADMIN");
        for(String action:List.of("save","archive","unknown"))assertFalse(Access.write(admin,"equipes",action));
        Exchange missing=new Exchange();missing.method="POST";missing.action="affecter";
        missing.cookie=new Cookie("jwt",JwtUtil.generateToken(admin));missing.run("/EquipeServlet",admin);
        assertEquals(403,missing.status);assertFalse(missing.passed);
    }
    @Test void legacyAssignmentDoesNotGrantLoginRights() {
        for(String status:Arrays.asList(null,""," ")) {
            Membre legacy=member(status,null);assertTrue(Access.teamTarget(legacy));assertFalse(Access.active(legacy));
            legacy.setCompteStatut("GELE");assertFalse(Access.teamTarget(legacy));
            legacy.setCompteStatut("DESACTIVE");assertFalse(Access.teamTarget(legacy));
        }
        for(String status:List.of("RETRAITE","ANCIEN_MEMBRE","UNKNOWN"))assertFalse(Access.teamTarget(member(status,null)));
    }
    @Test void csrfProtectsPublicAndProtectedPosts() throws Exception {
        for (String route : List.of("/LoginServlet", "/CandidatureServlet", "/AjouterPublicationServlet")) {
            Exchange e = new Exchange(); e.method="POST"; e.run(route, null);
            assertEquals(403,e.status); assertFalse(e.passed);
        }
        Exchange e = new Exchange(); e.method="POST"; e.csrf="valid-token";
        e.attributes.put("csrf","valid-token"); e.cookie=new Cookie("jwt","expired.invalid.cookie"); e.run("/LoginServlet",null);
        assertTrue(e.passed);
    }
}


