package com.lias.service;
import java.util.*;import java.time.*;
public final class Input {
 private final Map<String,String> values;
 public Input(Map<String,String> values){this.values=values;}
 public String text(String key,int max,boolean required){String v=values.getOrDefault(key,"").trim();if((required&&v.isEmpty())||v.length()>max)throw new IllegalArgumentException("Champ invalide : "+key);return v;}
 public int id(String key){try{int n=Integer.parseInt(text(key,10,true));if(n<1)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IllegalArgumentException("Identifiant ou quantité invalide : "+key);}}
 public Integer optionalId(String key){return text(key,10,false).isEmpty()?null:id(key);}
 public String choice(String key,String... options){String v=text(key,60,true);if(!Arrays.asList(options).contains(v))throw new IllegalArgumentException("Valeur invalide : "+key);return v;}
 public String email(String key){String v=text(key,254,true);if(!v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("Adresse email invalide.");return v;}
 public String date(String key,boolean required){String v=text(key,30,required);if(v.isEmpty())return null;try{LocalDate.parse(v);return v;}catch(Exception e){throw new IllegalArgumentException("Date invalide : "+key);}}
 public String time(String key,boolean required){String v=text(key,30,required);if(v.isEmpty())return null;try{return LocalDateTime.parse(v).toString().replace('T',' ');}catch(Exception e){throw new IllegalArgumentException("Date/heure invalide : "+key);}}
 public String url(String key){String v=text(key,1000,false);if(v.isEmpty())return v;try{java.net.URI uri=java.net.URI.create(v);if(!Set.of("https","http").contains(uri.getScheme())||uri.getHost()==null)throw new Exception();return v;}catch(Exception e){throw new IllegalArgumentException("Lien HTTP(S) invalide.");}}
 public String password(){String v=values.getOrDefault("password","");if(v.length()<12||v.length()>128)throw new IllegalArgumentException("Le mot de passe doit contenir 12 à 128 caractères.");return v;}
 public void dateOrder(String a,String b){if(a!=null&&b!=null&&a.compareTo(b)>0)throw new IllegalArgumentException("La date de fin précède la date de début.");}
}
