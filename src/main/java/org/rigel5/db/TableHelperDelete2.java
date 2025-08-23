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

import com.workingdogs.village.Column;
import com.workingdogs.village.Record;
import com.workingdogs.village.Value;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.om.NumberKey;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.SimpleKey;
import org.apache.torque.om.StringKey;
import org.commonlib5.utils.StringJoin;

/**
 * Utility per la cancellazione ricorsiva di record in tabella.
 *
 * @author Nicola De Nisco
 */
public class TableHelperDelete2 extends TableHelper
{
  private static final Log log = LogFactory.getLog(TableHelperDelete2.class);

  protected boolean enableMultiKey = false;
  protected final List<String> comandi = new ArrayList<>();
  protected final List<String> recurse = new ArrayList<>();

  public TableHelperDelete2(Connection con, boolean dryrun)
     throws SQLException
  {
    super(con, dryrun);
  }

  public void prepareDeleteCascade(String fieldPrimary, ObjectKey<?> primaryKey)
     throws Exception
  {
    Collection<ObjectKey<?>> primaryKeys = new ArrayList<>();
    primaryKeys.add(primaryKey);
    prepareDeleteCascade(fieldPrimary, primaryKeys);
  }

  public void prepareDeleteCascade(String fieldPrimary, Collection<ObjectKey<?>> primaryKeys)
     throws Exception
  {
    Stack<String> sttable = new Stack<>();
    deleteCascade(sttable, fieldPrimary, primaryKeys);
  }

  protected void deleteCascade(Stack<String> sttable, String fieldPrimary, Collection<ObjectKey<?>> primaryKeys)
     throws Exception
  {
    String key = schemaName + "." + tableName;
    if(sttable.contains(key))
    {
      log.warn("Rilevata dipendanza circolare fra tabelle: " + sttable);
      recurse.add(sttable.toString());
      return;
    }

    sttable.push(key);
    deleteTable(fieldPrimary, primaryKeys);

    HashMap<String, Collection<ObjectKey<?>>> mapKeys = new HashMap<>();
    mapKeys.put(fieldPrimary, primaryKeys);

    for(Map.Entry<String, List<RelazioniBean>> entry : esportate.entrySet())
    {
      String nome = entry.getKey();
      List<RelazioniBean> lsRel = entry.getValue();

      for(RelazioniBean b : lsRel)
      {
        Collection<ObjectKey<?>> alternateKeys = mapKeys.get(b.pkcolumn_name);
        if(alternateKeys == null)
        {
          alternateKeys = getAlternateKeys(fieldPrimary, primaryKeys,
             b.pkcolumn_name, b.pktable_schem + "." + b.pktable_name);
          mapKeys.put(b.pkcolumn_name, alternateKeys);
        }

        if(!alternateKeys.isEmpty())
        {
          TableHelperDelete2 thd = new TableHelperDelete2(con, dryrun);
          thd.enableMultiKey = enableMultiKey;
          thd.loadData(b.fktable_schem, b.fktable_name);
          thd.deleteCascade(sttable, b.fkcolumn_name, alternateKeys);
          comandi.addAll(thd.comandi);
          recurse.addAll(thd.recurse);
        }
      }
    }

    sttable.pop();
  }

  private void deleteTable(String fieldPrimary, Collection<ObjectKey<?>> primaryKeys)
     throws Exception
  {
    if(primaryKeys.isEmpty())
      return;

    String jk = joinKeys(primaryKeys);
    if(jk.isEmpty())
      return;

    String sDEL
       = "DELETE FROM " + schemaName + "." + tableName
       + " WHERE " + fieldPrimary + " IN(" + jk + ")";

    comandi.add(sDEL);
  }

  private Collection<ObjectKey<?>> getAlternateKeys(String fieldPrimary, Collection<ObjectKey<?>> primaryKeys, String targetField, String tableName)
     throws Exception
  {
    String sSQL
       = "SELECT DISTINCT " + targetField
       + "  FROM " + tableName
       + " WHERE " + fieldPrimary + " IN(" + joinKeys(primaryKeys) + ")";

    List<Record> lsRecs = DbUtils.executeQuery(sSQL, con);
    if(lsRecs.isEmpty())
      return Collections.EMPTY_LIST;

    Collection<ObjectKey<?>> rv = new ArrayList<>();
    Column col = lsRecs.get(0).schema().column(1);

    if(col.isNumericValue())
    {
      for(Record r : lsRecs)
      {
        Value value = r.getValue(1);
        if(!value.isNull())
          rv.add(SimpleKey.keyFor(value.asInt()));
      }
    }
    else if(col.isStringValue())
    {
      for(Record r : lsRecs)
      {
        Value value = r.getValue(1);
        if(!value.isNull())
          rv.add(SimpleKey.keyFor(value.asString()));
      }
    }
    else
    {
      throw new Exception("Chiave di tipo non ammesso.");
    }

    return rv;
  }

  public String joinKeys(Collection<ObjectKey<?>> keys)
  {
    if(keys.isEmpty())
      return "";

    int test = keys.iterator().next().getJdbcType();

    if(test == Types.VARCHAR)
      return StringJoin.build(",", "'").addObjects(keys, (k) -> ((StringKey) k).getValue()).join();

    if(test == Types.NUMERIC)
      return StringJoin.build(",").addObjects(keys, (k) -> ((NumberKey) k).getValue().toString()).join();

    throw new RuntimeException("Chiave di tipo non ammesso.");
  }

  public List<String> getComandi()
  {
    return comandi;
  }

  public List<String> getRecurse()
  {
    return recurse;
  }

  public long executeDelete()
     throws Exception
  {
    long numDeleted = 0;

    for(int i = comandi.size() - 1; i >= 0; i--)
    {
      final String sDEL = (String) comandi.get(i);

      if(dryrun)
        log.info("sDEL=" + sDEL);
      else
        numDeleted += DbUtils.executeStatement(sDEL, con);
    }

    return numDeleted;
  }

  public boolean isEnableMultiKey()
  {
    return enableMultiKey;
  }

  public void setEnableMultiKey(boolean enableMultiKey)
  {
    this.enableMultiKey = enableMultiKey;
  }
}
