package com.lias.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.lias.model.Membre;

/** Signed HS256 identity tokens. Permissions are refreshed from the database. */
public final class JwtUtil {

    // Clé secrète HMAC. >= 32 caractères recommandé pour HS256.
    private static final String SECRET = signingSecret();
    private static String signingSecret() {
        String configured = System.getProperty("LIAS_JWT_SECRET", System.getenv("LIAS_JWT_SECRET"));
        if (configured != null && !configured.isBlank()) {
            if (configured.length() < 32) throw new IllegalStateException("LIAS_JWT_SECRET requires at least 32 characters");
            return configured;
        }
        // Local development: restart invalidates existing tokens; never use a public shared key.
        byte[] bytes = new byte[32]; new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static final String ALGO_HMAC = "HmacSHA256";
    private static final String ALGO_JWT = "HS256";

    // Durée de validité du token (en secondes). 2h par défaut.
    private static final long VALIDITE_SECONDES = 2 * 60 * 60;

    // Header JWT constant : {"alg":"HS256","typ":"JWT"}
    private static final String HEADER_JSON = "{\"alg\":\"" + ALGO_JWT + "\",\"typ\":\"JWT\"}";
    private static final String HEADER_B64 = base64UrlEncode(HEADER_JSON.getBytes(StandardCharsets.UTF_8));

    private JwtUtil() {
        // classe utilitaire, non instanciable
    }

    // =========================================================================
    // GENERATION DU TOKEN
    // =========================================================================

    /** Génère un JWT signé à partir d'un Membre authentifié. */
    public static String generateToken(Membre membre) {
        long maintenant = System.currentTimeMillis() / 1000L;
        long expiration = maintenant + VALIDITE_SECONDES;

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(membre.getIdMembre()));
        claims.put("email", membre.getEmail());
        claims.put("nom", membre.getNom());
        claims.put("prenom", membre.getPrenom());
        claims.put("role", membre.getRoleActuel());
        claims.put("statut", membre.getStatutActuel());
        claims.put("iat", maintenant);
        claims.put("exp", expiration);

        return generateToken(claims);
    }

    /** Génère un JWT signé à partir d'une map de claims arbitraire. */
    public static String generateToken(Map<String, Object> claims) {
        String payloadJson = toJson(claims);
        String payloadB64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

        String donneesSignees = HEADER_B64 + "." + payloadB64;
        String signature = signerHmac(donneesSignees);

        return donneesSignees + "." + signature;
    }

    // =========================================================================
    // VALIDATION DU TOKEN
    // =========================================================================

    /**
     * Valide un JWT : signature correcte + non expiré.
     * @return la map des claims si le token est valide, null sinon.
     */
    public static Map<String, String> validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        String[] parties = token.split("\\.", -1);
        if (parties.length != 3) {
            return null; // format invalide
        }

        String header = parties[0];
        if (!HEADER_B64.equals(header)) return null;
        String payload = parties[1];
        String signatureRecue = parties[2];

        // 1. Vérification de la signature (protège contre la falsification du token)
        String donneesSignees = header + "." + payload;
        String signatureAttendue = signerHmac(donneesSignees);

        boolean signatureValide = MessageDigest.isEqual(
                signatureRecue.getBytes(StandardCharsets.UTF_8),
                signatureAttendue.getBytes(StandardCharsets.UTF_8));

        if (!signatureValide) {
            return null; // token altéré ou signé avec une autre clé
        }

        // 2. Décodage du payload
        Map<String, String> claims;
        try {
            String payloadJson = new String(base64UrlDecode(payload), StandardCharsets.UTF_8);
            claims = parseJson(payloadJson);
        } catch (Exception e) {
            return null; // payload corrompu
        }

        // 3. Vérification de l'expiration
        String expStr = claims.get("exp");
        if (expStr == null) {
            return null;
        }
        try {
            long exp = Long.parseLong(expStr);
            long maintenant = System.currentTimeMillis() / 1000L;
            if (maintenant >= exp) {
                return null; // token expiré
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return claims;
    }

    // =========================================================================
    // SIGNATURE HMAC-SHA256
    // =========================================================================

    private static String signerHmac(String donnees) {
        try {
            Mac mac = Mac.getInstance(ALGO_HMAC);
            SecretKeySpec cle = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), ALGO_HMAC);
            mac.init(cle);
            byte[] signatureBytes = mac.doFinal(donnees.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // Ne devrait jamais arriver : HmacSHA256 est standard dans tout JDK.
            throw new IllegalStateException("Erreur lors de la signature du JWT", e);
        }
    }

    // =========================================================================
    // BASE64 URL-SAFE (RFC 7515 / RFC 4648 §5), sans padding
    // =========================================================================

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    // =========================================================================
    // MINI JSON (fait main, suffisant pour des claims plats clé -> valeur simple)
    // =========================================================================

    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object valeur = entry.getValue();
            if (valeur == null) {
                sb.append("null");
            } else if (valeur instanceof Number || valeur instanceof Boolean) {
                sb.append(valeur.toString());
            } else {
                sb.append("\"").append(escapeJson(valeur.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /** Strict parser for the flat scalar claims emitted above. */
    private static Map<String, String> parseJson(String json) {
        JsonCursor c = new JsonCursor(json);
        Map<String, String> result = new LinkedHashMap<>();
        c.expect('{');
        if (!c.take('}')) {
            do {
                String key = c.string(); c.expect(':'); c.space();
                String value;
                if (c.peek() == '"') value = c.string();
                else {
                    int start = c.i;
                    while (c.i < json.length() && json.charAt(c.i) != ',' && json.charAt(c.i) != '}') c.i++;
                    value = json.substring(start, c.i).trim();
                    if (!value.matches("null|true|false|-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) throw new IllegalArgumentException("Invalid scalar");
                }
                if (result.containsKey(key)) throw new IllegalArgumentException("Duplicate claim");
                result.put(key, value);
            } while (c.take(','));
            c.expect('}');
        }
        c.space(); if (c.i != json.length()) throw new IllegalArgumentException("Trailing JSON");
        return result;
    }
    private static final class JsonCursor {
        final String text; int i;
        JsonCursor(String text) { this.text = text; }
        void space() { while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++; }
        char peek() { if (i >= text.length()) throw new IllegalArgumentException("Truncated JSON"); return text.charAt(i); }
        boolean take(char ch) { space(); if (i < text.length() && text.charAt(i) == ch) { i++; return true; } return false; }
        void expect(char ch) { if (!take(ch)) throw new IllegalArgumentException("Invalid JSON"); }
        String string() {
            expect('"'); StringBuilder out = new StringBuilder();
            while (i < text.length()) {
                char ch = text.charAt(i++);
                if (ch == '"') return out.toString();
                if (ch < 32) throw new IllegalArgumentException("Control character");
                if (ch == '\\') {
                    ch = peek(); i++;
                    switch (ch) {
                        case '"', '\\', '/' -> out.append(ch);
                        case 'b' -> out.append('\b'); case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n'); case 'r' -> out.append('\r'); case 't' -> out.append('\t');
                        case 'u' -> { if (i + 4 > text.length()) throw new IllegalArgumentException("Unicode escape"); out.append((char)Integer.parseInt(text.substring(i,i+4),16)); i+=4; }
                        default -> throw new IllegalArgumentException("Escape");
                    }
                } else out.append(ch);
            }
            throw new IllegalArgumentException("Unterminated string");
        }
    }
    private static String escapeJson(String s) {
        StringBuilder out = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '"' || ch == '\\') out.append('\\').append(ch);
            else if (ch < 32) out.append(String.format("\\u%04x", (int)ch));
            else out.append(ch);
        }
        return out.toString();
    }
}
