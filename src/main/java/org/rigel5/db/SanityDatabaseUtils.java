/*
 * Copyright (C) 2020 Nicola De Nisco
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

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Funzioni di base per inserire valori di rifermento in un db generico.
 *
 * @author Nicola De Nisco
 */
public class SanityDatabaseUtils
{
  private final Log log = LogFactory.getLog(SanityDatabaseUtils.class);

  protected final HashSet<String> skipTableIdTable = new HashSet<>();
  protected final HashSet<String> skipTableInsZero = new HashSet<>();
  protected Date today = new Date();
  protected Timestamp todayts = new Timestamp(today.getTime());

  public static final String NESSUNO_INDEFINITO = "Nessuno/indefinito";

  // alcuni database (ORACLE) hanno bisogno di convertire i nomi tabella in maiuscolo
  protected boolean needUppercase = false;
  protected boolean verbose = false;
  protected boolean disableForeign = true;

  public SanityDatabaseUtils()
  {
    // tabelle da non inserire in ID_TABLE (solo MAIUSCOLO per confronto case insensitive)
    skipTableIdTable.add("ID_TABLE");

    // tabelle in cui non va inserito il record 0  (solo MAIUSCOLO per confronto case insensitive)
    skipTableInsZero.add("TURBINE_SCHEDULED_JOB");
    skipTableInsZero.add("TURBINE_USER");
    skipTableInsZero.add("TURBINE_GROUP");
    skipTableInsZero.add("TURBINE_ROLE");
    skipTableInsZero.add("TURBINE_PERMISSION");
    skipTableInsZero.add("TURBINE_USER_GROUP_ROLE");
    skipTableInsZero.add("TURBINE_ROLE_PERMISSION");
    skipTableInsZero.add("ID_TABLE");
  }

  public SanityDatabaseUtils(boolean needUppercase, boolean verbose, boolean disableForeign)
  {
    this();
    this.needUppercase = needUppercase;
    this.verbose = verbose;
    this.disableForeign = disableForeign;
  }

  public void addSkipTableInsZero(String tableName)
  {
    skipTableInsZero.add(tableName);
  }

  public void addSkipTableIdTable(String tableName)
  {
    skipTableIdTable.add(tableName);
  }

  public Date getToday()
  {
    return today;
  }

  public void setToday(Date today)
  {
    this.today = today;
  }

  public boolean isNeedUppercase()
  {
    return needUppercase;
  }

  public void setNeedUppercase(boolean needUppercase)
  {
    this.needUppercase = needUppercase;
  }

  public boolean isVerbose()
  {
    return verbose;
  }

  public void setVerbose(boolean verbose)
  {
    this.verbose = verbose;
  }

  public boolean isDisableForeign()
  {
    return disableForeign;
  }

  public void setDisableForeign(boolean disableForeign)
  {
    this.disableForeign = disableForeign;
  }

  public void inserisciZeroSQL(Connection con, Writer out)
     throws Exception
  {
    List<String> tables = DbUtils.getAllTables(con);

    // elenco di tabelle da ignorare
    HashSet<String> skipSet = new HashSet<>(skipTableInsZero);
    HashMap<String, SQLException> errorsMap = new HashMap<>();

    int num;
    for(num = 1; num <= 10; num++)
    {
      boolean haveError = false;
      out.write("Passaggio creazione record zero: " + num + "\n");
      out.flush();
      errorsMap.clear();

      for(String table : tables)
      {
        if(skipSet.contains(table.toUpperCase()))
          continue;

        if(disableForeign)
          DbUtils.disableForeignKeys(table);

        // costruisce la insert per il record 0
        String sSQL = costruisciSQLzero(con, table);

        if(sSQL != null)
        {
          if(verbose)
            out.write(sSQL + "\n");

          String fatto = "OK!!";
          try(Statement st = con.createStatement())
          {
            st.executeUpdate(sSQL);
            skipSet.add(table);
          }
          catch(SQLException ex)
          {
            if(!testSqlIgnore(ex))
            {
              haveError = true;
              errorsMap.put(table, ex);
              fatto = "ko";
            }
          }

          out.write(table + ": " + fatto + "\n");
          out.flush();
        }

        if(disableForeign)
          DbUtils.enableForeignKeys(table);
      }

      if(!haveError)
        break;
    }

    out.write("Completato in " + num + " passaggi.\n");
    for(String table : tables)
    {
      Throwable t = errorsMap.get(table);
      if(t != null)
        out.write("Errore su tabella " + table + ": " + t.getMessage() + "\n");
    }
    out.flush();
  }

  protected String costruisciSQLzero(Connection con, String nomeTabella)
     throws Exception
  {
    String nomeSchema = null;
    int pos = nomeTabella.indexOf('.');
    if(pos != -1)
    {
      nomeSchema = nomeTabella.substring(0, pos);
      nomeTabella = nomeTabella.substring(pos + 1);
    }

    if(needUppercase)
    {
      if(nomeSchema != null)
        nomeSchema = nomeSchema.toUpperCase();

      nomeTabella = nomeTabella.toUpperCase();
    }

    int nsize = NESSUNO_INDEFINITO.length();
    StringBuilder sb1 = new StringBuilder(1024);
    StringBuilder sb2 = new StringBuilder(1024);
    try(ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), nomeSchema, nomeTabella, null))
    {
      for(int i = 0; rs.next(); i++)
      {
        String cn = rs.getString("COLUMN_NAME");
        int tipo = rs.getInt("DATA_TYPE");
        int size = rs.getInt("COLUMN_SIZE");
        int tn = rs.getInt("NULLABLE");

        if(i > 0)
        {
          sb1.append(',');
          sb2.append(',');
        }

        sb1.append(cn);

        switch(tipo)
        {
          case Types.BIT:
          case Types.TINYINT:
          case Types.SMALLINT:
          case Types.INTEGER:
          case Types.BIGINT:
          case Types.FLOAT:
          case Types.REAL:
          case Types.DOUBLE:
          case Types.NUMERIC:
          case Types.DECIMAL:
            if("stato_rec".equalsIgnoreCase(cn))
              sb2.append("10");
            else
              sb2.append("0");
            break;

          case Types.TIMESTAMP:
            if(tn == ResultSetMetaData.columnNoNulls)
              sb2.append("current_timestamp");
            else
              sb2.append("NULL");
            break;

          case Types.BOOLEAN:
            sb2.append("false");
            break;

          default:
            if(tn == ResultSetMetaData.columnNoNulls)
            {
              if(size > nsize)
                sb2.append("'").append(NESSUNO_INDEFINITO).append("'");
              else
                sb2.append("'0'");
            }
            else
              sb2.append("NULL");
            break;
        }
      }
    }

    if(sb1.length() == 0)
      return null;

    if(nomeSchema != null)
      return "INSERT INTO " + nomeSchema + "." + nomeTabella + " (" + sb1.toString() + ") VALUES (" + sb2.toString() + ")";

    return "INSERT INTO " + nomeTabella + " (" + sb1.toString() + ") VALUES (" + sb2.toString() + ")";
  }

  private String ignoreMessages[] =
  {
    "ERROR: duplicate key value violates unique constraint"
  };

  protected boolean testSqlIgnore(SQLException ex)
  {
    String em = ex.getMessage();
    for(int i = 0; i < ignoreMessages.length; i++)
    {
      if(em.contains(ignoreMessages[i]))
        return true;
    }
    return false;
  }

  public void executeSqlScript(Connection con, Writer out, File toRun)
     throws Exception
  {
    if(!toRun.canRead())
    {
      out.write(String.format("Il file %s non leggibile. Continuo.\n", toRun.getAbsolutePath()));
      return;
    }

    out.write(String.format("\n\n=== ESEGUO %s ===\n", toRun.getAbsolutePath()));
    try(InputStream is = new FileInputStream(toRun))
    {
      BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

      String linea;
      StringBuilder sb = new StringBuilder(1024);
      while((linea = br.readLine()) != null)
      {
        linea = linea.trim();

        if(linea.endsWith(";"))
        {
          sb.append(linea.substring(0, linea.length() - 1));
          String sSQL = sb.toString();

          if(needUppercase)
            sSQL = sSQL.toUpperCase();

          if(verbose)
            out.write(sSQL + "\n");

          try(Statement st = con.createStatement())
          {
            st.executeUpdate(sSQL);
          }
          catch(SQLException ex)
          {
            if(!testSqlIgnore(ex))
            {
              if(!verbose)
                out.write(sSQL + "\n");
              out.write("ERROR: " + ex.getMessage() + "\n");
            }
          }

          sb = new StringBuilder(1024);
        }
        else
          sb.append(linea).append('\n');
      }
    }

    out.flush();
  }
}
