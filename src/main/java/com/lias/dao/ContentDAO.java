package com.lias.dao;
import java.sql.*;import java.util.*;import com.lias.model.Membre;import com.lias.model.Module;import com.lias.service.*;import com.lias.security.Access;
public class ContentDAO {
 public int save(Connection c,Membre actor,Module m,Input in)throws SQLException {
  Access.require(Access.write(actor,m.permission()));Integer id=in.optionalId("id");Map<String,Object> before=null;
  if(id!=null){before=Sql.one(c,"SELECT * FROM "+m.table()+" WHERE "+m.pk()+"=? FOR UPDATE",id);if(m.key().equals("publications"))Access.require(Access.direction(actor)||Sql.id(before.get("id_auteur"))==actor.getIdMembre());}
  Map<String,Object> data=new LinkedHashMap<>();
  for(Module.Field f:m.fields()){
   Object v=switch(f.type()){
    case "textarea"->in.text(f.name(),10000,f.required());case "date"->in.date(f.name(),f.required());case "datetime-local"->in.time(f.name(),f.required());
    case "url"->in.url(f.name());case "email"->in.text(f.name(),254,f.required()).isEmpty()?"":in.email(f.name());
    case "select"->in.choice(f.name(),f.options().split(","));case "reference"->f.required()?Integer.valueOf(in.id(f.name())):in.optionalId(f.name());
    case "year"->{int y=in.id(f.name());if(y<1900||y>java.time.Year.now().getValue()+1)throw new IllegalArgumentException("Année invalide.");yield y;}
    default->in.text(f.name(),255,f.required());};data.put(f.name(),v);
   if("reference".equals(f.type())&&v!=null){String table,pk;if(f.options().equals("documents")){table="document";pk="id_doc";}else {Module ref=Module.get(f.options());table=ref.table();pk=ref.pk();}Sql.one(c,"SELECT "+pk+" FROM "+table+" WHERE "+pk+"=?",v);
    if(f.name().equals("document_id")&&!Access.internal(actor)){throw new SecurityException("Les archives sont internes. Utilisez un lien public.");}
   }
  }
  if(m.key().equals("evenements"))in.dateOrder((String)data.get("date_event"),(String)data.get("date_fin"));
  if(m.key().equals("conventions"))in.dateOrder((String)data.get("date_debut"),(String)data.get("date_fin"));
  if(id==null){if(m.key().equals("publications"))data.put("id_auteur",actor.getIdMembre());if(m.key().equals("reunions")){data.put("id_organisateur",actor.getIdMembre());data.put("date_creation",java.sql.Date.valueOf(java.time.LocalDate.now()));}id=insert(c,m.table(),data);}
  else update(c,m.table(),m.pk(),id,data);
  Trace.audit(c,actor,before==null?"CREATION":"MODIFICATION",m.table(),id,before,data);
  if(m.key().equals("evenements")&&before==null)Trace.broadcast(c,"Nouvel événement : "+data.get("titre"),"evenements.jsp",false);
  return id;
 }
 public void archive(Connection c,Membre actor,Module m,int id)throws SQLException {
  Access.require(Access.direction(actor)&&m.state()!=null);Map<String,Object> before=Sql.one(c,"SELECT * FROM "+m.table()+" WHERE "+m.pk()+"=? FOR UPDATE",id);
  Sql.update(c,"UPDATE "+m.table()+" SET "+m.state()+"='ARCHIVE' WHERE "+m.pk()+"=?",id);Trace.audit(c,actor,"ARCHIVAGE",m.table(),id,before,"ARCHIVE");
 }
 public static int insert(Connection c,String table,Map<String,Object> data)throws SQLException {
  return Sql.insert(c,"INSERT INTO "+table+" ("+String.join(",",data.keySet())+") VALUES ("+String.join(",",Collections.nCopies(data.size(),"?"))+")",data.values().toArray());
 }
 public static void update(Connection c,String table,String pk,int id,Map<String,Object> data)throws SQLException {
  List<Object> args=new ArrayList<>(data.values());args.add(id);Sql.update(c,"UPDATE "+table+" SET "+String.join(",",data.keySet().stream().map(k->k+"=?").toList())+" WHERE "+pk+"=?",args.toArray());
 }
}

