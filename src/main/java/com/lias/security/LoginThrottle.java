package com.lias.security;
import java.util.*;
/** Bounded process-local throttling; independent from attacker-controlled sessions. */
public final class LoginThrottle {
 private static final Map<String,long[]> attempts=new LinkedHashMap<>();
 private LoginThrottle(){}
 public static synchronized boolean blocked(String address){long[] a=attempts.get(address);if(a==null)return false;if(System.currentTimeMillis()-a[0]>300000){attempts.remove(address);return false;}return a[1]>=20;}
 public static synchronized void fail(String address){if(attempts.size()>4096)attempts.remove(attempts.keySet().iterator().next());long[] a=attempts.computeIfAbsent(address,k->new long[]{System.currentTimeMillis(),0});a[1]++;}
 public static synchronized void clear(String address){attempts.remove(address);}
}
