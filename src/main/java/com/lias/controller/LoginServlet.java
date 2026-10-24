package com.lias.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Cookie;

import com.lias.dao.AccountDAO;
import com.lias.security.Access;
import com.lias.model.Membre;
import com.lias.security.JwtUtil;
import com.lias.security.JwtFilter;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final AccountDAO membreDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.sendRedirect("login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if(com.lias.security.LoginThrottle.blocked(request.getRemoteAddr())){response.sendError(429);return;}
        Membre membre;
        try { membre = membreDAO.authenticate(email, password); } catch (java.sql.SQLException e) { getServletContext().log("Login unavailable", e); response.sendError(503); return; }

        if (Access.active(membre)) {
            com.lias.security.LoginThrottle.clear(request.getRemoteAddr());

            // Vérification des statuts imposés par le cahier des charges

            if ("RETRAITE".equalsIgnoreCase(membre.getStatutActuel())) {

                request.setAttribute(
                        "erreur",
                        "Votre compte est gelé car vous êtes retraité.");

                request.getRequestDispatcher("login.jsp")
                       .forward(request, response);
                return;
            }

            if ("ANCIEN".equalsIgnoreCase(membre.getStatutActuel())
                    || "ANCIEN_MEMBRE".equalsIgnoreCase(membre.getStatutActuel())) {

                request.setAttribute(
                        "erreur",
                        "Votre compte est désactivé.");

                request.getRequestDispatcher("login.jsp")
                       .forward(request, response);
                return;
            }

            // Génération du JWT : c'est lui qui devient le véritable justificatif
            // d'authentification, transporté dans un cookie HttpOnly (illisible en JS,
            // donc protégé contre le vol de token par XSS).
            String token = JwtUtil.generateToken(membre);

            Cookie cookieJwt = new Cookie(JwtFilter.COOKIE_NAME, token);
            cookieJwt.setHttpOnly(true);
            cookieJwt.setPath("/");
            cookieJwt.setMaxAge(2 * 60 * 60); // doit rester cohérent avec JwtUtil (2h)
            cookieJwt.setSecure(request.isSecure());
            cookieJwt.setAttribute("SameSite", "Lax");
            response.addCookie(cookieJwt);

            // On alimente aussi la session tout de suite : le JwtFilter la resynchronisera
            // de toute façon depuis le token dès la requête suivante, mais cela évite
            // un aller-retour "à vide" avant la redirection vers dashboard.jsp.
            HttpSession session = request.getSession();
            request.changeSessionId();
            session.setAttribute("user", membre);
            session.setAttribute("role", membre.getRoleActuel());
            session.setAttribute("statut", membre.getStatutActuel());
            session.setAttribute("id", String.valueOf(membre.getIdMembre()));

            response.sendRedirect("dashboard.jsp");
            System.out.println("ROLE = " + membre.getRoleActuel());
            System.out.println("STATUT = " + membre.getStatutActuel());
        } else {
            com.lias.security.LoginThrottle.fail(request.getRemoteAddr());

            request.setAttribute(
                    "erreur",
                    "Email ou mot de passe incorrect, ou compte restreint.");

            request.getRequestDispatcher("login.jsp")
                   .forward(request, response);
        }
    }
}
