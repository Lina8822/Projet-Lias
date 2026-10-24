import com.lias.config.DBConnection;
import com.lias.dao.Sql;
import com.lias.security.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;
import com.sun.tools.attach.VirtualMachine;

public class VerifyRealAuthentication {
 static final String BASE="http://localhost:8080/Lias-Jee";
 static void ok(boolean test,String label){if(!test)throw new AssertionError(label);System.out.println("PASS "+label);}
 record Response(int statusCode,String body,HttpHeaders headers) {}
 static class Browser {
  CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
  Response request(String path,String body)throws Exception {
   CookieHandler.setDefault(cookies);
   HttpURLConnection connection=(HttpURLConnection)URI.create(BASE+path).toURL().openConnection();
   connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
   if(body!=null){connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");try(var out=connection.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}
   int code=connection.getResponseCode();
   var raw=new HashMap<String,List<String>>(connection.getHeaderFields());raw.remove(null);
   String text;try(var in=code>=400?connection.getErrorStream():connection.getInputStream()){text=in==null?"":new String(in.readAllBytes(),StandardCharsets.UTF_8);}
   connection.disconnect();return new Response(code,text,HttpHeaders.of(raw,(k,val)->true));
  }
  Response get(String p)throws Exception{return request(p,null);}
  Response login(String email,String password)throws Exception {
   return request("/LoginServlet","email="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8));
  }
  HttpCookie cookie(String name){return cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals(name)).findFirst().orElseThrow();}
 }
 public static void main(String[] args)throws Exception {
  String email="directeur.test@lias.example.test",password=System.getenv("LIAS_TEST_PASSWORD");
  Browser b=new Browser();ok(b.get("/login").statusCode()==200,"real GET /login = 200");
  var login=b.login(email,password);ok(login.statusCode()==302,"real POST /LoginServlet = 302");
  ok(login.headers().firstValue("location").orElse("").endsWith("dashboard.jsp"),"redirect dashboard.jsp");
  var jwt=b.cookie("jwt");ok(jwt.isHttpOnly(),"JWT cookie HttpOnly");
  var claims=JwtUtil.validateToken(jwt.getValue());ok(claims!=null && "DIRECTEUR".equals(claims.get("role")) && "PERMANENT".equals(claims.get("statut")),"signed JWT role DIRECTEUR status PERMANENT");
  ok(b.get("/dashboard").statusCode()==200,"real GET /dashboard = 200");
  ok(b.get("/dashboard.jsp").statusCode()==200,"real GET /dashboard.jsp = 200");
  String sid=b.cookie("JSESSIONID").getValue();
  VirtualMachine vm=VirtualMachine.attach(args[0]);String connector;
  try {var props=vm.getSystemProperties();System.out.println("Tomcat catalina.base="+props.getProperty("catalina.base"));System.out.println("Tomcat LIAS_DB_URL override="+props.getProperty("LIAS_DB_URL","none"));connector=vm.startLocalManagementAgent();}finally{vm.detach();}
  try(JMXConnector jmx=JMXConnectorFactory.connect(new JMXServiceURL(connector))) {
   var server=jmx.getMBeanServerConnection();ObjectName manager=new ObjectName("Catalina:type=Manager,host=localhost,context=/Lias-Jee");
   for(String key:List.of("id","role","statut")) {
    Object value=server.invoke(manager,"getSessionAttribute",new Object[]{sid,key},new String[]{"java.lang.String","java.lang.String"});
    String expected=key.equals("id")?claims.get("sub"):key.equals("role")?"DIRECTEUR":"PERMANENT";
    ok(expected.equals(String.valueOf(value)),"actual session "+key+"="+value);
   }
  }
  b.cookies.getCookieStore().remove(URI.create(BASE),jwt);
  ok(b.get("/dashboard").statusCode()==200,"dashboard with session only, without JWT");
  Browser bad=new Browser();var wrong=bad.login(email,"definitely-wrong");
  ok(wrong.statusCode()==200&&wrong.body().contains("Email ou mot de passe incorrect"),"wrong password shows login error without 500");
  var absent=bad.login("director1788985651828@example.test",password);
  ok(absent.statusCode()==200&&absent.body().contains("Email ou mot de passe incorrect"),"old isolated-test email rejected because absent");
  try(Connection c=DBConnection.getConnection()) {
   System.out.println("JDBC catalog="+c.getCatalog()+"; user="+c.getMetaData().getUserName()+"; url="+c.getMetaData().getURL());
   for(var row:Sql.rows(c,"SELECT id,email,mot_de_passe FROM utilisateur WHERE id IN (1,2)")) {
    String original=Sql.str(row.get("mot_de_passe"));
    if(original.startsWith("pbkdf2$"))continue;
    Browser legacy=new Browser();var response=legacy.login(Sql.str(row.get("email")),original);
    ok(response.statusCode()==302,"existing account id="+row.get("id")+" login = 302");
    ok(legacy.get("/dashboard").statusCode()==200,"existing account id="+row.get("id")+" dashboard = 200");
    String stored=Sql.str(Sql.one(c,"SELECT mot_de_passe FROM utilisateur WHERE id=?",row.get("id")).get("mot_de_passe"));
    ok(stored.startsWith("pbkdf2$")&&Passwords.verify(original,stored),"existing password migrated after successful real login; id="+row.get("id"));
   }
  }
 }
}

