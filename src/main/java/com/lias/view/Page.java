package com.lias.view;
import java.util.*;import com.lias.model.Module.Field;
public class Page {
 public String key,title,subtitle;public List<Block> blocks=new ArrayList<>();public List<Form> forms=new ArrayList<>();public Map<String,Long> stats=new LinkedHashMap<>();
 public Map<String,List<Map<String,Object>>> choices=new HashMap<>();public List<Map<String,Object>> notifications=new ArrayList<>(),calendar=new ArrayList<>(),chat=new ArrayList<>();public Map<String,Object> profile=new HashMap<>();
 public Page(String key,String title){this.key=key;this.title=title;this.subtitle="Laboratoire LIAS · Faculté des Sciences Ben M’Sik";}
 public record Action(String label,String endpoint,Map<String,String> params,boolean post,String confirm){}
 public record Row(Map<String,Object> values,List<Action> actions){}
 public record Block(String title,Map<String,String> columns,List<Row> rows){}
 public record Form(String title,String endpoint,List<Field> fields,Map<String,Object> values,Map<String,String> hidden,boolean multipart){}
 public static String e(Object v){if(v==null)return "";return v.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
 public static String q(Object v){return java.net.URLEncoder.encode(v==null?"":v.toString(),java.nio.charset.StandardCharsets.UTF_8);}
 public static String url(Action a){StringBuilder s=new StringBuilder(a.endpoint());if(!a.params().isEmpty()){s.append('?');a.params().forEach((k,v)->s.append(q(k)).append('=').append(q(v)).append('&'));}return s.toString();}
}
