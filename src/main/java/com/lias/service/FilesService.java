package com.lias.service;
import jakarta.servlet.http.Part;import java.nio.file.*;import java.io.*;import java.util.*;import com.lias.config.DBConnection;
public final class FilesService {
 private FilesService(){}
 public static Path root()throws IOException {Path p=Path.of(DBConnection.setting("LIAS_UPLOAD_DIR",System.getProperty("catalina.base",".")+"/lias-files")).toAbsolutePath().normalize();Files.createDirectories(p);return p;}
 public static String save(Part part,boolean photo)throws IOException {
  if(part==null||part.getSize()==0)return null;if(part.getSize()>10*1024*1024)throw new IllegalArgumentException("Fichier limité à 10 Mo.");
  byte[] bytes;try(InputStream in=part.getInputStream()){bytes=in.readNBytes(10*1024*1024+1);}String ext;
  if(!photo&&bytes.length>=5&&new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))ext=".pdf";
  else if(photo&&bytes.length>=8&&bytes[0]==(byte)137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71)ext=".png";
  else if(photo&&bytes.length>=3&&bytes[0]==(byte)255&&bytes[1]==(byte)216&&bytes[2]==(byte)255)ext=".jpg";
  else throw new IllegalArgumentException(photo?"Photo PNG ou JPEG requise.":"Document PDF requis.");
  if(photo){var img=javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));if(img==null||img.getWidth()>6000||img.getHeight()>6000)throw new IllegalArgumentException("Photo invalide ou trop grande.");}
  String name=UUID.randomUUID()+ext;Files.write(root().resolve(name),bytes,StandardOpenOption.CREATE_NEW);return name;
 }
 public static Path resolve(String name)throws IOException{if(name==null||!name.matches("[0-9a-f-]{36}\\.(pdf|png|jpg)"))throw new IllegalArgumentException("Fichier historique à importer dans le stockage sécurisé.");Path p=root().resolve(name).normalize();if(!p.startsWith(root()))throw new SecurityException();return p;}
}
