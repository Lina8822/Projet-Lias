package com.lias.security;

import java.util.Set;

/** Resolves only known page routes; never turns a request into a JSP filename. */
public final class PageRoutes {
    public static boolean materialRequest(String path,String action){return "/DemandeServlet".equals(path)&&("demander".equals(action)||"traiter".equals(action));}
    private PageRoutes() {}
    public static final Set<String> PAGES = Set.of("index", "login", "candidature", "dashboard", "profil", "membres", "equipes", "gouvernance", "demandes", "evenements", "publications", "documents", "materiel", "conventions", "reunions", "messagerie", "calendrier", "rapport", "notifications", "recherche", "historique");
    public static String key(String path) {
        if (path == null) return null;
        if (path.isEmpty() || path.equals("/")) return "index";
        if (!path.startsWith("/")) return null;
        String key = path.substring(1);
        if (key.endsWith(".jsp")) key = key.substring(0, key.length()-4);
        return PAGES.contains(key) ? key : null;
    }
}
