/*
 * Copyright (C) 2022 Nicola De Nisco
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.rigel5.db;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.commonlib5.utils.Classificatore;

/**
 * Utilita per tabelle.
 *
 * @author Nicola De Nisco
 */
public class TableHelper
{
  protected final Connection con;
  protected final DatabaseMetaData meta;
  protected final Classificatore<String, RelazioniBean> esportate = new Classificatore<>();
  protected final Classificatore<String, RelazioniBean> importate = new Classificatore<>();
  protected String schemaName, tableName;
  protected boolean dryrun;

  public TableHelper(Connection con, boolean dryrun)
     throws SQLException
  {
    this.con = con;
    this.meta = con.getMetaData();
    this.dryrun = dryrun;
  }

  public void loadData(String schemaName, String tableName)
     throws Exception
  {
    this.schemaName = schemaName;
    this.tableName = tableName;

    esportate.clear();
    importate.clear();

    try ( ResultSet rs = meta.getExportedKeys(null, schemaName, tableName))
    {
      while(rs.next())
      {
        RelazioniBean b = new RelazioniBean();
        b.read(rs);

        esportate.aggiungi(makeKey(b), b);
      }
    }

    try ( ResultSet rs = meta.getImportedKeys(null, schemaName, tableName))
    {
      while(rs.next())
      {
        RelazioniBean b = new RelazioniBean();
        b.read(rs);

        importate.aggiungi(makeKey(b), b);
      }
    }
  }

  public String makeKey(RelazioniBean b)
  {
    return b.pktable_schem + "." + b.pktable_name + " -> " + b.fktable_schem + "." + b.fktable_name;
  }

  public String makeKey(String schemaName, String tableName)
  {
    return this.schemaName + "." + this.tableName + " -> " + schemaName + "." + tableName;
  }

  public boolean findEsportate(String schemaName, String tableName)
  {
    return esportate.containsKey(makeKey(schemaName, tableName));
  }

  public boolean findEsportate(String tableName)
  {
    return esportate.containsKey(makeKey(schemaName, tableName));
  }

  public boolean findImportate(String schemaName, String tableName)
  {
    return importate.containsKey(makeKey(schemaName, tableName));
  }

  public boolean findImportate(String tableName)
  {
    return importate.containsKey(makeKey(schemaName, tableName));
  }

  public void dumpImportate(PrintStream out)
     throws IOException
  {
    for(Map.Entry<String, List<RelazioniBean>> entry : importate.entrySet())
    {
      String key = entry.getKey();
      List<RelazioniBean> val = entry.getValue();

      out.print(key + "\n");
      for(RelazioniBean bb : val)
      {
        out.print("\t" + bb.pkcolumn_name + " => " + bb.fkcolumn_name + "\t\t" + bb.key_seq + "\n");
      }
    }
  }

  public void dumpEsportate(PrintStream out)
     throws IOException
  {
    for(Map.Entry<String, List<RelazioniBean>> entry : esportate.entrySet())
    {
      String key = entry.getKey();
      List<RelazioniBean> val = entry.getValue();

      out.print(key + "\n");
      for(RelazioniBean bb : val)
      {
        out.print("\t" + bb.pkcolumn_name + " => " + bb.fkcolumn_name + "\t\t" + bb.key_seq + "\n");
      }
    }
  }

  public static class RelazioniBean
  {
    public String pktable_cat,
       pktable_schem,
       pktable_name,
       pkcolumn_name,
       fktable_cat,
       fktable_schem,
       fktable_name,
       fkcolumn_name,
       fk_name,
       pk_name;

    public int key_seq,
       update_rule,
       delete_rule,
       deferrability;

    public void read(ResultSet rs)
       throws SQLException
    {
      pktable_cat = rs.getString("pktable_cat");
      pktable_schem = rs.getString("pktable_schem");
      pktable_name = rs.getString("pktable_name");
      pkcolumn_name = rs.getString("pkcolumn_name");
      fktable_cat = rs.getString("fktable_cat");
      fktable_schem = rs.getString("fktable_schem");
      fktable_name = rs.getString("fktable_name");
      fkcolumn_name = rs.getString("fkcolumn_name");
      key_seq = rs.getInt("key_seq");
      update_rule = rs.getInt("update_rule");
      delete_rule = rs.getInt("delete_rule");
      fk_name = rs.getString("fk_name");
      pk_name = rs.getString("pk_name");
      deferrability = rs.getInt("deferrability");
    }
  }
}
