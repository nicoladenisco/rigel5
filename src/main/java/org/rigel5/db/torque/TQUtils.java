/*
 *  TQUtils.java
 *  Creato il 23-giu-2015, 10.37.47
 *
 *  Copyright (C) 2015 Informatica Medica s.r.l.
 *
 *  Questo software è proprietà di Informatica Medica s.r.l.
 *  Tutti gli usi non esplicitimante autorizzati sono da
 *  considerarsi tutelati ai sensi di legge.
 *
 *  Informatica Medica s.r.l.
 *  Viale dei Tigli, 19
 *  Casalnuovo di Napoli (NA)
 */
package org.rigel5.db.torque;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.adapter.AbstractAdapter;
import org.apache.torque.adapter.Adapter;
import org.apache.torque.adapter.IDMethod;
import org.apache.torque.adapter.MssqlAdapter;
import org.apache.torque.adapter.MysqlAdapter;
import org.apache.torque.adapter.OracleAdapter;
import org.apache.torque.adapter.PostgresAdapter;

/**
 * Classe di utilita per Torque.
 *
 * @author Nicola De Nisco
 */
public class TQUtils
{
  public static String getPrimaryKeyMethodInfo(AbstractAdapter dbAdapter, String tableName, String primaryKey)
  {
    if(dbAdapter.getIDMethodType().equals(IDMethod.SEQUENCE))
    {
      if(dbAdapter instanceof PostgresAdapter)
      {
        return tableName + "_" + primaryKey + "_seq";
      }

      return tableName + "_SEQ";
    }
    else if(dbAdapter.getIDMethodType().equals(IDMethod.AUTO_INCREMENT))
    {
      return tableName;
    }

    return "";
  }

  public static String getAdapterName(Adapter dbAdapter)
  {
    if(dbAdapter instanceof PostgresAdapter)
      return "postgresql";
    if(dbAdapter instanceof MysqlAdapter)
      return "mysql";
    if(dbAdapter instanceof MssqlAdapter)
      return "mssql";
    if(dbAdapter instanceof OracleAdapter)
      return "oracle";

    return "unknow";
  }

  public static String getAdapterName()
     throws TorqueException
  {
    String dbname = Torque.getDefaultDB();
    return getAdapterName(dbname);
  }

  public static String getAdapterName(String dbname)
     throws TorqueException
  {
    if(dbname == null)
      dbname = Torque.getDefaultDB();

    Configuration cfg = Torque.getConfiguration();
    String adpterName = cfg.getString("database." + dbname + ".adapter");

    if("auto".equalsIgnoreCase(adpterName))
    {
      Adapter dbAdapter = Torque.getAdapter(dbname);
      adpterName = getAdapterName(dbAdapter);
    }

    return adpterName;
  }

  static
  {
    HashMap<String, String> tmp = new HashMap<>();
    tmp.put("oracle", "OracleQueryBuilder");
    tmp.put("mssql", "MSSQLQueryBuilder");
    tmp.put("mysql", "MysqlQueryBuilder");
    tmp.put("postgresql", "Postgre73QueryBuilder");
    torqueRigelAdapterMap = Collections.unmodifiableMap(tmp);
  }

  public static final Map<String, String> torqueRigelAdapterMap;
}
