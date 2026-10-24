package com.lias.security;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import com.lias.dao.AccountDAO;
import com.lias.model.Membre;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

/** JWT proves identity; current account and mandates determine permissions. */
@WebFilter("/*")
public class JwtFilter implements Filter {
    public static final String COOKIE_NAME = "jwt";
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of("/LoginServlet", "/LogoutServlet", "/CandidatureServlet", "/SuiviCandidatureServlet", "/favicon.ico");
    private static final Map<String,String> ACTIONS = Map.ofEntries(
        Map.entry("/DemandeServlet", "demandes"), Map.entry("/MandatServlet", "gouvernance"),
        Map.entry("/CompteServlet", "comptes"), Map.entry("/EquipeServlet", "equipes"),
        Map.entry("/ConventionServlet", "conventions"), Map.entry("/ReunionServlet", "reunions"),
        Map.entry("/AjouterEvenementServlet", "evenements"), Map.entry("/AjouterPublicationServlet", "publications"),
        Map.entry("/AjouterMaterielServlet", "materiel"), Map.entry("/DocumentServlet", "documents"),
        Map.entry("/MessageServlet", "messagerie"), Map.entry("/ModifierProfilServlet", "profil"),
        Map.entry("/NotificationServlet", "notifications"));
    protected Membre loadUser(int id) throws SQLException { return new AccountDAO().load(id); }
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        request.setCharacterEncoding("UTF-8");
        String path = request.getServletPath();
        if (path.startsWith("/assets/")) { chain.doFilter(req, res); return; }
        HttpSession csrfSession=request.getSession();
        if(csrfSession.getAttribute("csrf")==null)csrfSession.setAttribute("csrf",java.util.UUID.randomUUID().toString());
        if("POST".equals(request.getMethod())||"PUT".equals(request.getMethod())||"DELETE".equals(request.getMethod())||"PATCH".equals(request.getMethod())) {
            String supplied;
            try { supplied=request.getParameter("csrf"); }
            catch(IllegalStateException e){response.sendError(413,"Fichier trop volumineux");return;}
            String expected=(String)csrfSession.getAttribute("csrf");
            if(supplied==null||!java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8))){response.sendError(403);return;}
        }
        String key = PageRoutes.key(path);
        // Entry pages must remain reachable even with an expired cookie or an unavailable database.
        if ((key != null && Set.of("index", "login", "candidature").contains(key))
                || Set.of("/LoginServlet", "/CandidatureServlet", "/SuiviCandidatureServlet").contains(path)) {
            chain.doFilter(req, res); return;
        }
        boolean publicRoute = PUBLIC_ENDPOINTS.contains(path) || (key != null && Access.read(null, key));
        HttpSession session = request.getSession(false);
        Object cached = session == null ? null : session.getAttribute("user");
        Membre user = cached instanceof Membre ? (Membre) cached : null;
        Cookie jwt = null;
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) if (COOKIE_NAME.equals(cookie.getName())) jwt = cookie;
        try {
            if (jwt != null) {
                Map<String,String> claims = JwtUtil.validateToken(jwt.getValue());
                int id = claims == null ? 0 : Integer.parseInt(claims.get("sub"));
                user = id > 0 ? loadUser(id) : null;
                if (!Access.active(user)) { expire(request, response); return; }
            } else if (user != null) {
                user = loadUser(user.getIdMembre());
                if (!Access.active(user)) { expire(request, response); return; }
            }
        } catch (NumberFormatException e) { expire(request, response); return; }
        catch (SQLException e) { request.getServletContext().log("Account refresh unavailable", e); response.sendError(503, "Connexion à la base indisponible"); return; }
        if (user != null) {
            session = request.getSession();
            session.setAttribute("user", user);
            session.setAttribute("role", user.getRoleActuel());
            session.setAttribute("statut", user.getStatutActuel());
            session.setAttribute("id", String.valueOf(user.getIdMembre()));
        }
        // Team assignments are administrative writes, including when called anonymously.
        if ("/EquipeServlet".equals(path) && "affecter".equals(request.getParameter("action")) && !Access.assignTeam(user)) { response.sendError(403); return; }
        if (!publicRoute && user == null) { response.sendRedirect(request.getContextPath()+"/login.jsp"); return; }
        if (key != null && !Access.read(user, key)) { response.sendError(403); return; }
        String actionModule = PageRoutes.materialRequest(path,request.getParameter("action"))?"materiel":ACTIONS.get(path);
        if (actionModule != null && !Access.write(user, actionModule,request.getParameter("action"))) { response.sendError(403); return; }
        chain.doFilter(req, res);
    }
    private void expire(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Cookie cookie = new Cookie(COOKIE_NAME, ""); cookie.setPath("/"); cookie.setHttpOnly(true); cookie.setMaxAge(0); response.addCookie(cookie);
        HttpSession session = request.getSession(false); if (session != null) session.invalidate();
        response.sendRedirect(request.getContextPath()+"/login.jsp?erreur=session_expiree");
    }
}


