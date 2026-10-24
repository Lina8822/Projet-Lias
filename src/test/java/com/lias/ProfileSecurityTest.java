package com.lias;
import com.lias.model.Membre;
import com.lias.service.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfileSecurityTest {
 @Test void foreignProfileIdIsRejectedBeforeDatabaseAccess(){
  Membre actor=LaunchRegressionTest.member("PERMANENT","MEMBRE_EQUIPE");
  Input input=new Input(Map.of("id","2"));
  assertThrows(SecurityException.class,()->new MembershipService().profile(null,actor,input,null));
 }
 @Test void foreignPasswordTargetIsRejectedBeforeDatabaseAccess(){
  Membre actor=LaunchRegressionTest.member("PERMANENT","MEMBRE_EQUIPE");
  assertThrows(SecurityException.class,()->new MembershipService().password(null,actor,new Input(Map.of("id","2"))));
 }
}
