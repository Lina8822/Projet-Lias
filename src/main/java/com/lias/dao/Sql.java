package com.lias.dao;

import com.lias.config.DBConnection;
import java.sql.*;
import java.util.*;

/** JDBC helpers. SQL identifiers are always supplied by code, never by a request. */
public final class Sql {
    private Sql() {}
    public static List<Map<String,Object>> rows(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p=c.prepareStatement(sql)) {
            bind(p,args);
            try(ResultSet r=p.executeQuery()) {
                List<Map<String,Object>> result=new ArrayList<>();
                while(r.next()) { Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=r.getMetaData().getColumnCount();i++) row.put(r.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT),r.getObject(i));
                    result.add(row);
                } return result;
            }
        }
    }
    public static List<Map<String,Object>> rows(String sql,Object... args) throws SQLException {
        try(Connection c=DBConnection.getConnection()){return rows(c,sql,args);}
    }
    public static int update(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)){bind(p,args);return p.executeUpdate();}
    }
    public static int insert(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            bind(p,args);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){if(r.next())return r.getInt(1);throw new SQLException("Missing generated key");}
        }
    }
    private static void bind(PreparedStatement p,Object[] args)throws SQLException{for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);}
    public static Map<String,Object> one(Connection c,String sql,Object... args)throws SQLException {
        List<Map<String,Object>> rows=rows(c,sql,args);if(rows.isEmpty())throw new IllegalArgumentException("Élément introuvable.");return rows.get(0);
    }
    public static int id(Object o){return ((Number)o).intValue();}
    public static String str(Object o){return o==null?"":o.toString();}
}
