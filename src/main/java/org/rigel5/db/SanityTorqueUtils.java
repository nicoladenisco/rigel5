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

import com.workingdogs.village.Record;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;
import org.apache.commons.logging.*;
import org.apache.torque.*;
import org.apache.torque.adapter.Adapter;
import org.apache.torque.adapter.IDMethod;
import org.apache.torque.adapter.PostgresAdapter;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.map.*;
import org.apache.torque.util.ColumnValues;
import org.apache.torque.util.JdbcTypedValue;
import org.commonlib5.utils.StringOper;
import org.rigel5.db.torque.TableMapHelper;

/**
 * Funzioni di base per inserire valori di rifermento in un db Torque.
 *
 * @author Nicola De Nisco
 */
public class SanityTorqueUtils extends SanityDatabaseUtils
{
  private static final Log log = LogFactory.getLog(SanityTorqueUtils.class);
  //
  protected Stack<String> stkTableNames = new Stack<String>();
  protected HashSet<String> htTableNames = new HashSet<String>();
  protected String databaseName;

  public SanityTorqueUtils()
  {
  }

  public void sanityIDtable()
     throws Exception
  {
    DatabaseMap dbMap = Torque.getDatabaseMap();
    String idtName = "ID_TABLE";

    // riporta i next a valori umani
    String sSQL = "UPDATE " + idtName + " SET next_id=10 WHERE next_id IS NULL";
    DbUtils.executeStatement(sSQL);
    sSQL = "UPDATE " + idtName + " SET quantity=10";
    DbUtils.executeStatement(sSQL);

    // estrae l'indice massimo di id_table
    sSQL = "SELECT MAX(id_table_id) FROM " + idtName;
    List lRecs = DbUtils.executeQuery(sSQL);
    Record rec = (Record) lRecs.get(0);
    int nextID = rec.getValue(1).asInt() + 1;

    // estrae elenco tabelle gia' esistenti in ID_TABLE
    HashMap<String, String> htTables = new HashMap<>();
    sSQL = "SELECT table_name FROM " + idtName;
    List lTables = DbUtils.executeQuery(sSQL);
    for(int i = 0; i < lTables.size(); i++)
    {
      Record rl = (Record) lTables.get(i);
      htTables.put(rl.getValue(1).asString().toUpperCase(), "");
    }

    // marca quelle che sono da ignorare comunque
    for(int i = 0; i < skipTableIdTable.length; i++)
      htTables.put(skipTableIdTable[i].toUpperCase(), "I");

    // aggiunge le tabelle mancanti
    TableMap[] arMaps = dbMap.getTables();
    for(int i = 0; i < arMaps.length; i++)
    {
      TableMap tm = arMaps[i];

      // solo le tabelle ID_BROKER sono presenti in ID_TABLE
      if(!StringOper.isEqu(IDMethod.ID_BROKER, tm.getPrimaryKeyMethod()))
        continue;

      // verifica se la tabella e' gia' presente
      String sFlag = null;
      if((sFlag = (String) htTables.get(tm.getName().toUpperCase())) == null)
      {
        try
        {
          // aggiunge la tabella nella id_table
          sSQL = "INSERT INTO " + idtName + " (id_table_id, table_name, next_id, quantity)"
             + " VALUES (" + nextID + ", '" + tm.getName() + "', 10, 10)";

          DbUtils.executeStatement(sSQL);
          log.info("Aggiunta " + tm.getName() + " alla tabella " + idtName);
          nextID++;
        }
        catch(Exception e)
        {
          log.debug("Failure " + prepareString(e) + "\nSQL=" + sSQL);
          continue;
        }
      }
      else if(!sFlag.equals("I"))
      {
        long newPri = -1;

        try
        {
          // estrae il valore massimo della chiave primaria
          newPri = DbUtils.getMaxPrimaryKey(tm, null);
          if(newPri == -1)
            continue;
        }
        catch(Exception e)
        {
          log.debug("Failure " + prepareString(e));
          continue;
        }

        try
        {
          // aggiorna tabella id_table
          newPri += 10;
          sSQL = "UPDATE " + idtName + " SET next_id=" + newPri
             + " WHERE table_name='" + tm.getName() + "'";

          DbUtils.executeStatement(sSQL);
          log.info("Modificata " + tm.getName() + " nella tabella " + idtName + " : pk=" + newPri);
        }
        catch(Exception e)
        {
          log.debug("Failure " + prepareString(e) + "\nSQL=" + sSQL);
          continue;
        }
      }
    }
  }

  /**
   * Aggiorna le sequenze utilizzate per le chiavi ad autoincremento
   * con il corrispettivo valore valido nella tabella afferente.
   * @throws Exception
   */
  public void sanitySequence()
     throws Exception
  {
    databaseName = Torque.getDefaultDB();
    Adapter dbAdapter = Torque.getAdapter(databaseName);

    if(dbAdapter instanceof PostgresAdapter)
      sanitySequencePostgres();
  }

  public void sanitySequencePostgres()
     throws Exception
  {
    Adapter dbAdapter = Torque.getAdapter(databaseName);
    DatabaseMap dbMap = Torque.getDatabaseMap();
    TableMap[] arMaps = dbMap.getTables();
    for(int i = 0; i < arMaps.length; i++)
    {
      TableMap tm = arMaps[i];

      // solo le tabelle NATIVE hanno le sequenze
      if(!StringOper.isEqu(IDMethod.NATIVE, tm.getPrimaryKeyMethod()))
        continue;

      IDMethod m = dbAdapter.getIDMethodType();
      String seqName = StringOper.okStr(tm.getPrimaryKeyMethodInfo(m), null);
      if(seqName != null)
      {
        long newPri = -1;
        String sSQL = null;

        try
        {
          // estrae il valore massimo della chiave primaria
          newPri = DbUtils.getMaxPrimaryKey(tm, null);
          if(newPri < 0)
            newPri = 0;
        }
        catch(Exception e)
        {
          log.debug("Failure " + prepareString(e));
          continue;
        }

        try
        {
          newPri++;
          sSQL = "SELECT pg_catalog.setval('" + seqName + "', " + newPri + ", false);";
          DbUtils.executeQuery(sSQL);
          log.info("Modificata " + seqName + " per la tabella " + tm.getName() + " : pk=" + newPri);
        }
        catch(Exception e)
        {
          log.debug("Failure " + prepareString(e) + "\nSQL=" + sSQL);
          continue;
        }
      }
    }
  }

  /**
   * Inserisce valori nulli per tutte le tabelle.
   * @throws Exception
   */
  public void sanityZero()
     throws Exception
  {
    DatabaseMap dbMap = Torque.getDatabaseMap();
    TableMap[] arMaps = dbMap.getTables();

    htTableNames.clear();
    stkTableNames.clear();

    // marca quelle che sono da ignorare comunque
    htTableNames.addAll(Arrays.asList(skipTableInsZero));

    Connection con = Torque.getConnection();
    try
    {
      for(int i = 0; i < arMaps.length; i++)
      {
        TableMap tm = arMaps[i];
        String nomeTabella = tm.getName().toUpperCase();

        // scarta tutte le tabelle turbine; non vanno toccate
        if(nomeTabella.startsWith("TURBINE_"))
        {
          htTableNames.add(nomeTabella);
          continue;
        }

        // salta tabelle già analizzate
        if(htTableNames.contains(nomeTabella))
          continue;

        try
        {
          if(testZero(con, dbMap, tm))
          {
            htTableNames.add(nomeTabella);
          }
        }
        catch(Throwable t)
        {
          // eccezione ignorata; scarica connessione potrebbe essere non riutilizzabile
          Torque.closeConnection(con);
          con = Torque.getConnection();
        }
      }
    }
    finally
    {
      Torque.closeConnection(con);
    }

    for(int i = 0; i < arMaps.length; i++)
    {
      TableMapHelper tm = new TableMapHelper(arMaps[i]);

      try
      {
        insertZeroCriteria(dbMap, tm);
      }
      catch(Exception e)
      {
        log.error("Non riesco a caricare il record 0 per " + tm.getNomeTabella());
      }
    }
  }

  /**
   * Verifica se la tabella indicata ha già il record 0.
   * @param con
   * @param dbMap
   * @param tm
   * @return
   * @throws Exception
   */
  protected boolean testZero(Connection con, DatabaseMap dbMap, TableMap tm)
     throws Exception
  {
    Criteria cr = new Criteria();
    ColumnMap[] cs = tm.getColumns();
    Column statoRec = null;
    for(int i = 0; i < cs.length; i++)
    {
      ColumnMap c = cs[i];
      if(c.isPrimaryKey())
      {
        if(c.getType() instanceof String)
          cr.and(c, "0");
        else if(c.getType() instanceof Timestamp)
          cr.and(c, new Timestamp(today.getTime()));
        else if(c.getType() instanceof Date)
          cr.and(c, today);
        else
          cr.and(c, 0);

        cr.addSelectColumn(c);
      }

      if(StringOper.isEquNocase("stato_rec", c.getColumnName()))
        statoRec = c;
    }

    List values = DbUtils.doSelect(cr, con);
    if(values.isEmpty())
      return false;

    if(statoRec != null)
    {
      ColumnValues upd = new ColumnValues();
      upd.put(statoRec, new JdbcTypedValue(10, Types.INTEGER));
      DbUtils.doUpdate(cr, upd, con);
    }

    return true;
  }

  /**
   * Inserisce un record 0 per la tabella indicata.
   * @param dbMap
   * @param tm
   */
  protected void insertZeroCriteria(DatabaseMap dbMap, TableMapHelper tm)
  {
    boolean recurse = true;
    String nomeTabella = tm.getNomeTabella().toUpperCase();

    // verifica per tabella gia' considerata
    if(htTableNames.contains(nomeTabella))
      return;

    // controlla alluppamento causa refernza circolare
    if(stkTableNames.contains(nomeTabella))
    {
      log.warn(
         "Relazione circolare fra tabelle. Potrebbe dar luogo ad errori imprevisti:\n"
         + stkTableNames);

      recurse = false;
      if(disableForeign)
        disableForeignKeys(stkTableNames);
    }

    stkTableNames.push(nomeTabella);
    try
    {
      ColumnValues c = buildInsertZeroCriteria(dbMap, tm, recurse);

      if(c != null)
      {
        DbUtils.doInsert(tm.getTmap().getFullyQualifiedTableName(), c);
        log.info("Tabella " + nomeTabella + " caricata con valore 0.");
      }
    }
    catch(Exception e)
    {
      if(!e.getMessage().contains("duplicate key value"))
        log.debug("Failure " + nomeTabella + ": " + prepareString(e));
    }
    stkTableNames.pop();
    htTableNames.add(nomeTabella);

    if(!recurse && disableForeign)
      enableForeignKeys(stkTableNames);
  }

  /**
   * Costruisce il criteria per l'aggiornamento della tabella indicata.
   * @param dbMap
   * @param tm
   * @param recurse
   * @return il criteria oppure null se non applicabile
   * @throws Exception
   */
  protected ColumnValues buildInsertZeroCriteria(DatabaseMap dbMap, TableMapHelper tm, boolean recurse)
     throws Exception
  {
    ColumnValues cr = new ColumnValues(tm.getTmap(), databaseName);
    ColumnMap[] cs = tm.getColumns();
    ForeignKeyMap fk;
    int numPrimary = 0;

    for(int i = 0; i < cs.length; i++)
    {
      ColumnMap c = cs[i];
      String colName = c.getColumnName();

      if(c.isPrimaryKey())
        numPrimary++;

      if(c.isPrimaryKey() || c.isNotNull())
      {
        if(c.getType() instanceof String)
          cr.put(c, new JdbcTypedValue("0", Types.CHAR));
        else if(c.getType() instanceof Timestamp)
          cr.put(c, new JdbcTypedValue(todayts, Types.TIMESTAMP));
        else if(c.getType() instanceof Date)
          cr.put(c, new JdbcTypedValue(today, Types.DATE));
        else
          cr.put(c, new JdbcTypedValue(0, Types.INTEGER));
      }

      if(StringOper.isEquNocase("descrizione", colName))
        cr.put(c, new JdbcTypedValue("Nessuno/Indefinito", Types.CHAR));
      if(StringOper.isEquNocase("stato_rec", colName))
        cr.put(c, new JdbcTypedValue(10, Types.INTEGER));

      if(recurse && (fk = tm.findForeignKeyByColumnName(colName)) != null)
      {
        TableMap tMap = fk.getForeignTable();
        insertZeroCriteria(dbMap, new TableMapHelper(tMap));
      }
    }

    return numPrimary == 1 ? cr : null;
  }

  protected String prepareString(Throwable ex)
  {
    String message = ex.getMessage().replaceAll("ERROR|error", "warning");
    return message;
  }

  public void sanityData()
     throws Exception
  {
  }

  protected void disableForeignKeys(Collection<String> tables)
  {
    for(String nome : tables)
      disableForeignKeys(nome);
  }

  protected void enableForeignKeys(Collection<String> tables)
  {
    for(String nome : tables)
      enableForeignKeys(nome);
  }

  protected void disableForeignKeys(String nomeTabella)
  {
    String sSQL = "ALTER TABLE " + nomeTabella + " DISABLE TRIGGER ALL";
    try
    {
      DbUtils.executeStatement(sSQL);
    }
    catch(TorqueException ex)
    {
      log.error(sSQL, ex);
    }
  }

  protected void enableForeignKeys(String nomeTabella)
  {
    String sSQL = "ALTER TABLE " + nomeTabella + " ENABLE TRIGGER ALL";
    try
    {
      DbUtils.executeStatement(sSQL);
    }
    catch(TorqueException ex)
    {
      log.error(sSQL, ex);
    }
  }
}
