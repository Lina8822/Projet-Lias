package com.lias.security;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public final class Passwords {
    private Passwords() {}
    public static String hash(String password) {
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return "pbkdf2$210000$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(password,salt,210000));
    }
    public static boolean verify(String password,String stored) {
        if(password==null||stored==null)return false;
        try {String[] p=stored.split("\\$");if(p.length!=4||!p[0].equals("pbkdf2"))return false;
            int rounds=Integer.parseInt(p[1]);if(rounds<10000||rounds>1000000)return false;
            return MessageDigest.isEqual(derive(password,Base64.getDecoder().decode(p[2]),rounds),Base64.getDecoder().decode(p[3]));
        }catch(RuntimeException e){return false;}
    }
    /** Temporary compatibility for legacy plaintext only; never treats a known digest as a password. */
    public static boolean verifyLegacyPlaintext(String password, String stored) {
        if (password == null || stored == null || stored.isEmpty() || stored.startsWith("$") || stored.startsWith("pbkdf2$")
                || stored.matches("(?i)[a-f0-9]{32}|[a-f0-9]{40}|[a-f0-9]{64}|[a-f0-9]{128}")) return false;
        return MessageDigest.isEqual(password.getBytes(java.nio.charset.StandardCharsets.UTF_8), stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static byte[] derive(String p,byte[] salt,int rounds){
        PBEKeySpec spec=new PBEKeySpec(p.toCharArray(),salt,rounds,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(GeneralSecurityException e){throw new IllegalStateException(e);}finally{spec.clearPassword();}
    }
    public static void main(String[] args){java.io.Console c=System.console();if(c==null)throw new IllegalStateException("Exécuter dans un terminal interactif.");char[] p=c.readPassword("Mot de passe : ");System.out.println(hash(new String(p)));Arrays.fill(p,'\0');}
}

