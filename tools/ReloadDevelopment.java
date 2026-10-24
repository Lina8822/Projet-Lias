import com.sun.tools.attach.VirtualMachine;
import javax.management.*;
import javax.management.remote.*;
public class ReloadDevelopment {
 public static void main(String[] args)throws Exception {
  var vms=VirtualMachine.list().stream().filter(v->v.displayName().contains("org.apache.catalina.startup.Bootstrap")).toList();
  if(vms.size()!=1)throw new IllegalStateException("Expected one local Tomcat");
  VirtualMachine vm=VirtualMachine.attach(vms.get(0).id());String address;
  try{address=vm.startLocalManagementAgent();}finally{vm.detach();}
  try(var connection=JMXConnectorFactory.connect(new JMXServiceURL(address))){
   var server=connection.getMBeanServerConnection();var names=server.queryNames(new ObjectName("Catalina:j2eeType=WebModule,*"),null).stream().filter(n->n.getKeyProperty("name").equals("//localhost/Lias-Jee")).toList();
   if(names.size()!=1)throw new IllegalStateException("Context not found");
   server.invoke(names.get(0),"reload",new Object[0],new String[0]);System.out.println("Reloaded Eclipse Tomcat /Lias-Jee");
  }
 }
}
