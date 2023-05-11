/*
 * Copyright (C) 2016 Nicola De Nisco
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

import com.workingdogs.village.*;
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
import org.apache.torque.util.Transaction;
import org.commonlib5.utils.SimpleTimer;
import org.commonlib5.utils.StringOper;
import org.rigel5.db.torque.TableMapHelper;

/**
 * Funzioni di base per inserire valori di rifermento in un db Torque.
 *
 * @author Nicola De Nisco
 */
public class SanityTorqueUtils3 extends SanityDatabaseUtils
{
  private static final Log log = LogFactory.getLog(SanityTorqueUtils3.class);
  protected final Stack<String> stkTableNames = new Stack<>();
  protected final HashSet<String> htTableNames = new HashSet<>();
  protected String databaseName;

  public SanityTorqueUtils3()
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
    for(String tname : skipTableIdTable)
      htTables.put(tname.toUpperCase(), "I");

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
    Connection con = null;
    try
    {
      con = Transaction.begin();
      sanityZero(con);
      Transaction.commit(con);
    }
    catch(Exception e)
    {
      Transaction.safeRollback(con);
    }
  }

  private void sanityZero(Connection con)
     throws Exception
  {
    SimpleTimer st = new SimpleTimer();
    DatabaseMap dbMap = Torque.getDatabaseMap();
    TableMap[] arMaps = dbMap.getTables();

    htTableNames.clear();
    stkTableNames.clear();

    // marca quelle che sono da ignorare comunque
    htTableNames.addAll(skipTableInsZero);

    for(TableMap tm : arMaps)
      processTable(new TableMapHelper(tm), dbMap, con);

    htTableNames.clear();
    stkTableNames.clear();

    log.info("sanityZero eseguita in " + st.getElapsed() + " millisecondi.");
  }

  private void processTable(TableMapHelper tm, DatabaseMap dbMap, Connection con)
  {
    String nomeTabella = tm.getNomeTabella().toUpperCase();

    // verifica per tabella gia' considerata
    if(htTableNames.contains(nomeTabella))
      return;

    // controlla alluppamento causa refernza circolare
    if(stkTableNames.contains(nomeTabella))
    {
      log.warn("Tabella " + nomeTabella + "\n"
         + "Relazione circolare fra tabelle. Potrebbe dar luogo ad errori imprevisti:\n"
         + stkTableNames);
      htTableNames.add(nomeTabella);
      return;
    }

    stkTableNames.push(nomeTabella);
    try
    {
      processTable0(nomeTabella, tm, dbMap, con);
      log.info("Tabella " + nomeTabella + " caricata con valore 0.");
    }
    catch(Exception e)
    {
      log.debug("Failure " + nomeTabella + ": " + prepareString(e));
    }
    stkTableNames.pop();
    htTableNames.add(nomeTabella);
  }

  private void processTable0(String nomeTabella, TableMapHelper tm, DatabaseMap dbMap, Connection con)
     throws Exception
  {
    ColumnValues crInsert = new ColumnValues();
    ColumnValues crUpdate = new ColumnValues();
    Criteria crSelect = new Criteria();
    ColumnMap[] cs = tm.getColumns();
    ForeignKeyMap fk;

    for(int i = 0; i < cs.length; i++)
    {
      ColumnMap c = cs[i];
      String colName = c.getColumnName();

      if(c.isPrimaryKey())
      {
        if(tm.isString(c))
        {
          crInsert.put(c, new JdbcTypedValue("0", Types.CHAR));
          crSelect.and(c, "0");
        }
        if(tm.isNumeric(c))
        {
          crInsert.put(c, new JdbcTypedValue(0, Types.INTEGER));
          crSelect.and(c, 0);
        }
        else
        {
          log.info("Tabella " + nomeTabella + "; tipo di chiave primaria non supportato.");
          return;
        }
      }
      else if(c.isNotNull())
      {
        if(tm.isString(c))
        {
          crInsert.put(c, new JdbcTypedValue("0", Types.CHAR));
          crUpdate.put(c, new JdbcTypedValue("0", Types.CHAR));
        }
        else if(c.getType() instanceof Timestamp)
        {
          crInsert.put(c, new JdbcTypedValue(todayts, Types.TIMESTAMP));
          crUpdate.put(c, new JdbcTypedValue(todayts, Types.TIMESTAMP));
        }
        else if(c.getType() instanceof Date)
        {
          crInsert.put(c, new JdbcTypedValue(today, Types.DATE));
          crUpdate.put(c, new JdbcTypedValue(today, Types.DATE));
        }
        else
        {
          crInsert.put(c, new JdbcTypedValue(0, Types.INTEGER));
          crUpdate.put(c, new JdbcTypedValue(0, Types.INTEGER));
        }
      }
      else if(tm.isNumeric(c))
      {
        crInsert.put(c, new JdbcTypedValue(0, Types.INTEGER));
        crUpdate.put(c, new JdbcTypedValue(0, Types.INTEGER));
      }

      if(StringOper.isEquNocase("descrizione", c.getColumnName()))
      {
        if(c.getSize() >= NESSUNO_INDEFINITO.length())
        {
          crInsert.put(c, new JdbcTypedValue(NESSUNO_INDEFINITO, Types.CHAR));
          crUpdate.put(c, new JdbcTypedValue(NESSUNO_INDEFINITO, Types.CHAR));
        }
      }
      else if(StringOper.isEquNocase("stato_rec", c.getColumnName()))
      {
        crInsert.put(c, new JdbcTypedValue(10, Types.INTEGER));
        crUpdate.put(c, new JdbcTypedValue(10, Types.INTEGER));
      }

      if((fk = tm.findForeignKeyByColumnName(colName)) != null)
      {
        processTable(new TableMapHelper(fk.getForeignTable()), dbMap, con);
      }
    }

    // inserisce o aggiorna record 0
    if(doInsert(tm.getFullyQualifiedTableName(), crInsert, con) == 0)
      doUpdate(crSelect, crUpdate, con);
  }

  private static int doInsert(String fullTableName, ColumnValues criteria, Connection con)
  {
    try
    {
      return DbUtils.doInsert(fullTableName, criteria, con);
    }
    catch(ConstraintViolationException e)
    {
      return 0;
    }
    catch(Exception e)
    {
      log.debug(e.getMessage());
      return 0;
    }
  }

  private static int doUpdate(Criteria selectCriteria, ColumnValues updateValues, Connection con)
  {
    try
    {
      return DbUtils.doUpdate(selectCriteria, updateValues, con);
    }
    catch(Exception e)
    {
      log.debug(e.getMessage());
      return 0;
    }
  }

  protected String prepareString(Throwable ex)
  {
    return ex.getMessage().replaceAll("ERROR|error", "warning");
  }

  public void sanityData()
     throws Exception
  {
  }
}
